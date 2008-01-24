/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;


/**
 * Not implemented.
 * @author mrk
 *
 */
public class LinearConvertFactory {
    /**
     * Factory creation method.
     * @param dbStructure The field to support.
     * @return The Support interface.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getSupportName();
        PVStructure pvStructure = dbStructure.getPVStructure();
        if(supportName==null) {
            pvStructure.message("supportName is null", MessageType.error);
            return null;
        }
        if(supportName.equals(linearConvertInput)) {
            return new LinearConvertInput(dbStructure);
        }
        if(supportName.equals(linearConvertOutput)) {
            return new LinearConvertOutput(dbStructure);
        }
        if(!supportName.equals(supportName)) {
            dbStructure.getPVStructure().message(
                "does not have support " + supportName,MessageType.error);
            return null;
        }
        return new LinearConvertInput(dbStructure);
    }
    
    private static final String linearConvertInput = "linearConvertInput";
    private static final String linearConvertOutput = "linearConvertOutput";
    
    private static abstract class LinearConvertBase extends AbstractSupport
    {
        
        protected DBStructure dbStructure = null;
        protected PVStructure pvStructure = null;
        protected DBField dbValue = null;
        protected PVDouble pvValue = null;
        protected DBField dbRawValue = null;
        protected PVInt pvRawValue;
        protected PVInt pvDeviceHigh;
        protected PVInt pvDeviceLow;
        
        protected PVDouble pvEngUnitsLow;
        protected PVDouble pvEngUnitsHigh;
        protected PVDouble pvSlope;
        protected DBField dbSlope;
        protected PVDouble pvIntercept;
        protected DBField dbIntercept;
        
        protected double slope;
        protected double intercept;
        
        
        protected LinearConvertBase(String supportName,DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,linearConvertInput)) return;
            DBRecord dbRecord = dbStructure.getDBRecord();
            DBField parentDBField = dbStructure.getParent();
            PVField parentPVField = parentDBField.getPVField();
            PVField pvField = parentPVField.findProperty("value");
            if(pvField==null) {
                super.message("parent does not have a value field", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=Type.pvInt) {
                super.message("parent value field does not have type int", MessageType.error);
                return;
            }
            pvRawValue = (PVInt)pvField;
            dbRawValue = dbRecord.findDBField(pvField);
            parentDBField = parentDBField.getParent();
            parentPVField = parentDBField.getPVField();
            pvField = parentPVField.findProperty("value");
            if(pvField==null) {
                super.message("parent of parent does not have a value field", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=Type.pvDouble) {
                super.message("parent of parent value field does not have type double", MessageType.error);
                return;
            }
            pvValue = (PVDouble)pvField;
            dbValue = dbRecord.findDBField(pvField);
            pvEngUnitsLow = (PVDouble)pvStructure.findProperty("engUnitsLow");
            pvEngUnitsHigh = (PVDouble)pvStructure.findProperty("engUnitsHigh");
            pvDeviceLow = (PVInt)pvStructure.findProperty("deviceLow");
            pvDeviceHigh = (PVInt)pvStructure.findProperty("deviceHigh");
            pvSlope = (PVDouble)pvStructure.findProperty("slope");
            dbSlope = dbRecord.findDBField(pvSlope);
            pvIntercept = (PVDouble)pvStructure.findProperty("intercept");
            dbIntercept = dbRecord.findDBField(pvSlope);
            super.setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
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
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            super.setSupportState(SupportState.readyForStart);
        }
        
    }
    
    private static  class LinearConvertInput extends LinearConvertBase {
        
        private LinearConvertInput(DBStructure dbStructure) {
            super(linearConvertInput,dbStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            double rawValue = (double)pvRawValue.get();
            double value = rawValue*slope + intercept;
            pvValue.put(value);
            dbValue.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
    }
    
    private static  class LinearConvertOutput extends LinearConvertBase {
        
        private LinearConvertOutput(DBStructure dbStructure) {
            super(linearConvertOutput,dbStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            double value = pvValue.get();
            double rawValue = (value -intercept)/slope;
            pvRawValue.put((int)rawValue);
            dbRawValue.postPut();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
    }
}
