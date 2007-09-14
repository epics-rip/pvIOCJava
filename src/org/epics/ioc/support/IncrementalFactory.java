/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * Record that implements incremental outputs.
 * The value field of the structure it supports is the desired value.
 * It puts it's value into the field passed to setField.
 * It requires field rateOfChange which must be numeric.
 * It optionally supports the following field incremental
 * If present it must be a boolean.
 * If the support exists it calls the support for input.
 * @author mrk
 *
 */
public class IncrementalFactory {
    /**
     * Create the support for the record or structure.
     * @param dbStructure The struvture or record for which to create support.
     * @return The support instance.
     */
    public static Support create(DBStructure dbStructure) {
        return new IncrementalImpl(dbStructure);
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    
    static private class IncrementalImpl extends AbstractSupport
    implements Support, SupportProcessRequester
    {
        private static String supportName = "incremental";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBField valueDBField = null;
        private PVField valuePVField = null;
        private DBField desiredValueDBField = null;
        private PVField desiredValuePVField = null;
        private Support inputSupport = null;
        private PVBoolean incrementalOutputPVField = null;
        private PVField rateOfChangePVField = null;
        
        private double desiredValue = 0.0;
        private double value = 0.0;
        private boolean incrementalOutput = true;
        private double rateOfChange = 0.0;
        
        private SupportProcessRequester supportProcessRequester = null;
        
        private IncrementalImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            Structure structure = (Structure)pvStructure.getField();
            DBField[] dbFields = dbStructure.getFieldDBFields();
            int index;
            PVField pvField = null;
            if(valueDBField==null) {
                super.message("setField was not called", MessageType.error);
                return;           
            }
            valuePVField = valueDBField.getPVField();
            if(!valuePVField.getField().getType().isNumeric()){
                super.message("value must be a numeric field", MessageType.error);
            }
            index = structure.getFieldIndex("value");
            if(index<0) {
                super.message("no desiredValue field", MessageType.error);
                return;
            }
            desiredValueDBField = dbFields[index];
            desiredValuePVField = desiredValueDBField.getPVField();
            if(!desiredValuePVField.getField().getType().isNumeric()){
                super.message("desiredValue must be a numeric field", MessageType.error);
                return;
            }
            index = structure.getFieldIndex("rateOfChange");
            if(index<0) {
                super.message("rateOfChange does not exist", MessageType.error);
                return;
            }
            pvField = dbFields[index].getPVField();
            if(!pvField.getField().getType().isNumeric()) {
                super.message("rateOfChange must be a numeric field", MessageType.error);
                return;
            }
            rateOfChangePVField = pvField;
            index = structure.getFieldIndex("input");
            if(index>=0) {
                inputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("incrementalOutput");
            if(index>=0) {
                pvField = dbFields[index].getPVField();
                if(pvField.getField().getType()!=Type.pvBoolean) {
                    super.message("incrementalOutput must be a boolean field", MessageType.error);
                    return;
                }
                incrementalOutputPVField = (PVBoolean)pvField;
            }           
            if(inputSupport!=null) {
                inputSupport.setField(desiredValueDBField);
                inputSupport.initialize();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            setSupportState(supportState);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            if(inputSupport!=null) {
                inputSupport.start();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.ready) return;
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(inputSupport!=null) inputSupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(inputSupport!=null) inputSupport.uninitialize();
            inputSupport = null;
            valueDBField = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequester==null) {
                throw new IllegalStateException("supportProcessRequester is null");
            }
            this.supportProcessRequester = supportProcessRequester;
            if(inputSupport!=null) {
                inputSupport.process(this);
                return;
            }
            computeValue();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {           
            computeValue();                
            supportProcessRequester.supportProcessDone(requestResult);
            return;
        }
        
        
        
        private void computeValue() {
            value = convert.toDouble(valuePVField);
            desiredValue = convert.toDouble(desiredValuePVField);
            if(incrementalOutputPVField!=null) incrementalOutput = incrementalOutputPVField.get();
            if(rateOfChangePVField!=null) rateOfChange = convert.toDouble(rateOfChangePVField);
            
            if(desiredValue==value) return;
            double newValue = desiredValue;
            if(incrementalOutput) {
                double diff = desiredValue - value;
                if(diff<0.0) {
                    newValue = value - rateOfChange;
                    if(newValue<desiredValue) newValue = desiredValue;
                } else {
                    newValue = value + rateOfChange;
                    if(newValue>desiredValue) newValue = desiredValue;
                }
            }
            convert.fromDouble(valuePVField, newValue);
            valueDBField.postPut();
        }
    }
}
