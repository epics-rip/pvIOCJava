/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Record that implements incremental outputs.
 * It requires the following:
 * value double
 *     desired
 *         value double
 *         input
 *         incremental boolean supportName = incremental
 *         rateOfChange
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
     * @param dbField The field for which to create support. It must have type boolean.
     * @return The support instance.
     */
    public static Support create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvBoolean) {
            pvField.message("type is not boolean", MessageType.error);
        }
        return new IncrementalImpl(dbField);
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    
    static private class IncrementalImpl extends AbstractSupport
    {
        private static String supportName = "incremental";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBField dbValue = null;
        private PVField pvValue = null;
        private PVField pvDesiredValue = null;
        private PVBoolean pvIncremental = null;
        private PVField pvRateOfChange = null;
        
        private double desiredValue = 0.0;
        private double value = 0.0;
        private boolean incremental = true;
        private double rateOfChange = 0.0;
        
        private IncrementalImpl(DBField dbField) {
            super(supportName,dbField);
            pvIncremental = (PVBoolean)dbField.getPVField();
            dbStructure = (DBStructure)dbField.getParent();
            pvStructure = dbStructure.getPVStructure();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBRecord dbRecord = dbStructure.getDBRecord();
            DBField parentDBField = dbStructure;
            PVField parentPVField = parentDBField.getPVField();
            PVField pvField = parentPVField.findProperty("value");
            if(pvField==null) {
                super.message("parent does not have a value field", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=Type.pvDouble) {
                super.message("the parent value field does not have type double", MessageType.error);
                return;
            }
            pvDesiredValue = (PVDouble)pvField;
            parentDBField = parentDBField.getParent();
            parentPVField = parentDBField.getPVField();
            pvField = parentPVField.findProperty("value");
            if(pvField==null) {
                super.message("parent of parent does not have a value field", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=Type.pvDouble) {
                super.message("the parent of parent value field does not have type double", MessageType.error);
                return;
            }
            pvValue = (PVDouble)pvField;
            dbValue = dbRecord.findDBField(pvField);
            pvRateOfChange = pvStructure.findProperty("rateOfChange");
            if(pvRateOfChange==null) {
                super.message("rateOfChange not found", MessageType.error);
                return;
            }
            if(!pvRateOfChange.getField().getType().isNumeric()) {
                super.message("rateOfChange must be a numeric field", MessageType.error);
                return;
            }
            super.setSupportState(SupportState.readyForStart); 
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            dbValue = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            value = convert.toDouble(pvValue);
            desiredValue = convert.toDouble(pvDesiredValue);
            if(pvIncremental!=null) incremental = pvIncremental.get();
            if(pvRateOfChange!=null) rateOfChange = convert.toDouble(pvRateOfChange);
            if(desiredValue==value) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            double newValue = desiredValue;
            if(incremental) {
                double diff = desiredValue - value;
                if(diff<0.0) {
                    newValue = value - rateOfChange;
                    if(newValue<desiredValue) newValue = desiredValue;
                } else {
                    newValue = value + rateOfChange;
                    if(newValue>desiredValue) newValue = desiredValue;
                }
            }
            convert.fromDouble(pvValue, newValue);
            dbValue.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
