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
 * Not implemented.
 * @author mrk
 *
 */
public class LinearConvertOutputFactory {
    /**
     * Factory creation method.
     * @param dbStructure The field to support.
     * @return The Support interface.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getSupportName();
        if(!supportName.equals(supportName)) {
            dbStructure.getPVStructure().message(
                "does not have support " + supportName,MessageType.error);
            return null;
        }
        return new LinearConvertImpl(dbStructure);
    }
    
    private static final String supportName = "linearConvertOutput";
    
    private static class LinearConvertImpl extends AbstractSupport
    implements SupportProcessRequester
    {
        
        private DBStructure dbStructure = null;
        private DBField dbValue = null;
        private PVDouble pvValue = null;
        
        private DBField dbRawValue = null;
        private PVInt pvRawValue;
        private DBField dbDeviceHigh;
        private PVInt pvDeviceHigh;
        private DBField dbDeviceLow;
        private PVInt pvDeviceLow;
        
        private Support outputSupport;
        private PVDouble pvEngUnitsLow;
        private PVDouble pvEngUnitsHigh;
        private PVDouble pvSlope;
        private DBField dbSlope;
        private PVDouble pvIntercept;
        private DBField dbIntercept;
        
        private double slope;
        private double intercept;
        
        private SupportProcessRequester supportProcessRequester;
        
        private LinearConvertImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            if(dbValue==null) {
                super.message("setField was not called", MessageType.error);
                return;
            }
            DBField[] dbFields = dbStructure.getFieldDBFields();
            PVStructure pvStructure = dbStructure.getPVStructure();
            PVField[] pvFields = pvStructure.getFieldPVFields();
            Structure structure = (Structure)pvStructure.getField();
            int index;
            
            index = structure.getFieldIndex("rawValue");
            dbRawValue = dbFields[index];
            pvRawValue = (PVInt)pvFields[index];
                       
            index = structure.getFieldIndex("output");
            outputSupport = dbFields[index].getSupport();
            outputSupport.setField(dbRawValue);
            index = structure.getFieldIndex("linearConvert");
            DBStructure dbLinearConvert = (DBStructure)dbFields[index];
            PVStructure linearConvert = (PVStructure)pvFields[index];
            structure = (Structure)linearConvert.getField();
            dbFields = dbLinearConvert.getFieldDBFields();
            pvFields = linearConvert.getFieldPVFields();
            index = structure.getFieldIndex("engUnitsLow");
            pvEngUnitsLow = (PVDouble)pvFields[index];
            index = structure.getFieldIndex("engUnitsHigh");
            pvEngUnitsHigh = (PVDouble)pvFields[index];
            index = structure.getFieldIndex("deviceLow");
            dbDeviceLow = dbFields[index];
            pvDeviceLow = (PVInt)pvFields[index];
            index = structure.getFieldIndex("deviceHigh");
            dbDeviceHigh = dbFields[index];
            pvDeviceHigh = (PVInt)pvFields[index];
            index = structure.getFieldIndex("slope");
            pvSlope = (PVDouble)pvFields[index];
            dbSlope = dbFields[index];
            index = structure.getFieldIndex("intercept");
            pvIntercept = (PVDouble)pvFields[index];
            dbIntercept = dbFields[index];
            outputSupport.initialize();
            super.setSupportState(outputSupport.getSupportState());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            outputSupport.uninitialize();
            super.setSupportState(outputSupport.getSupportState());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            outputSupport.start();
            if(outputSupport instanceof DeviceLimit) {
                ((DeviceLimit)outputSupport).get(dbDeviceLow, dbDeviceHigh);
            }
            slope = pvSlope.get();
            intercept = pvIntercept.get();
            if(slope==0.0) {
                double engUnitsLow = pvEngUnitsLow.get();
                double engUnitsHigh = pvEngUnitsHigh.get();
                if(engUnitsLow==engUnitsHigh) {
                    super.message("can't compute slope", MessageType.error);
                    return;
                }
                double deviceLow = (double)pvDeviceLow.get();
                double deviceHigh = (double)pvDeviceHigh.get();
                if(deviceLow==deviceHigh) {
                    super.message("can't compute slope", MessageType.error);
                    return;
                }
                slope = (engUnitsHigh - engUnitsLow)/(deviceHigh - deviceLow);
                intercept = (deviceHigh*engUnitsLow - deviceLow*engUnitsHigh)
                    /(deviceHigh - deviceLow);
                pvSlope.put(slope);
                dbSlope.postPut();
                pvIntercept.put(intercept);
                dbIntercept.postPut();
            }
            super.setSupportState(outputSupport.getSupportState());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            outputSupport.stop();
            super.setSupportState(outputSupport.getSupportState());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            this.supportProcessRequester = supportProcessRequester;
            double value = pvValue.get();
            double rawValue = (value -intercept)/slope;
            pvRawValue.put((int)rawValue);
            dbRawValue.postPut();
            outputSupport.process(this);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            if(dbField.getPVField().getField().getType()!=Type.pvDouble) {
                super.message("setField illegal argument. Must have type double", MessageType.error);
                return;
            }
            dbValue = dbField;
            pvValue = (PVDouble)dbField.getPVField();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            supportProcessRequester.supportProcessDone(requestResult);
        }
    }
}
