/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;

/**
 * Not implemented.
 * @author mrk
 *
 */
public class LinearConvertFactory {
    /**
     * Factory creation method.
     * @param pvRecordStructure The field to support.
     * @return The Support interface.
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
    	PVStructure pvStructure = pvRecordStructure.getPVStructure();
        PVAuxInfo pvAuxInfo = pvStructure.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvStructure.message("no pvAuxInfo with name support. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        if(supportName==null) {
            pvStructure.message("supportName is not defined", MessageType.error);
            return null;
        }
        if(supportName.equals(linearConvertInput)) {
            return new LinearConvertInput(pvRecordStructure);
        }
        if(supportName.equals(linearConvertOutput)) {
            return new LinearConvertOutput(pvRecordStructure);
        }
        pvStructure.message("no support for " + supportName,MessageType.error);
        return null;
    }

    private static final String linearConvertInput = "org.epics.pvioc.linearConvertInputFactory";
    private static final String linearConvertOutput = "org.epics.pvioc.linearConvertOutputFactory";
    
    private static abstract class LinearConvertBase extends AbstractSupport
    {
        
        protected PVStructure pvStructure = null;
        protected PVDouble pvValue = null;
        protected PVInt pvRawValue = null;
        protected PVInt pvDeviceHigh;
        protected PVInt pvDeviceLow;
        
        protected PVDouble pvEngUnitsLow;
        protected PVDouble pvEngUnitsHigh;
        protected PVDouble pvSlope;
        protected PVDouble pvIntercept;
        
        protected double slope;
        protected double intercept;
        
        
        protected LinearConvertBase(String supportName,PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
            pvStructure = pvRecordStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,linearConvertInput)) return;
            PVStructure pvParent = pvStructure.getParent();
            pvRawValue = getInt(pvParent,"value");
            if(pvRawValue==null) return;
            pvParent = pvParent.getParent();
            pvValue = getDouble(pvParent,"value");
            if(pvValue==null) return;
            pvEngUnitsLow = getDouble(pvStructure,"engUnitsLow");
            if(pvEngUnitsLow==null) return;
            pvEngUnitsHigh = getDouble(pvStructure,"engUnitsHigh");
            if(pvEngUnitsHigh==null) return;
            pvDeviceLow = getInt(pvStructure,"deviceLow");
            if(pvDeviceLow==null) return;
            pvDeviceHigh = getInt(pvStructure,"deviceHigh");
            if(pvDeviceHigh==null) return;
            pvSlope = getDouble(pvStructure,"slope");
            if(pvSlope==null) return;
            pvIntercept = getDouble(pvStructure,"intercept");
            if(pvIntercept==null) return;
            super.setSupportState(SupportState.readyForStart);
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#start()
         */
        public void start(AfterStart afterStart) {
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
                pvIntercept.put(intercept);
            }
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#stop()
         */
        public void stop() {
            super.setSupportState(SupportState.readyForStart);
        }
        
        private PVDouble getDouble(PVStructure parent,String fieldName) {
            PVField pvField = parent.getSubField(fieldName);
            if(pvField==null) {
                parent.message("does not have field " + fieldName, MessageType.error);
                return null;
            }
            if(pvField.getField().getType()!=Type.scalar) {
                pvField.message("type is not double", MessageType.error);
                return null;
            }
            PVScalar pvScalar = (PVScalar)pvField;
            if(pvScalar.getScalar().getScalarType()!=ScalarType.pvDouble) {
                super.message("parent of parent value field does not have type int", MessageType.error);
                return null;
            }
            return (PVDouble)pvField;
        }
        
        private PVInt getInt(PVStructure parent,String fieldName) {
            PVField pvField = parent.getSubField(fieldName);
            if(pvField==null) {
                parent.message("does not have field " + fieldName, MessageType.error);
                return null;
            }
            if(pvField.getField().getType()!=Type.scalar) {
                pvField.message("type is not double", MessageType.error);
                return null;
            }
            PVScalar pvScalar = (PVScalar)pvField;
            if(pvScalar.getScalar().getScalarType()!=ScalarType.pvInt) {
                super.message("parent of parent value field does not have type int", MessageType.error);
                return null;
            }
            return (PVInt)pvField;
        }
        
    }
    
    private static  class LinearConvertInput extends LinearConvertBase {
        
        private LinearConvertInput(PVRecordStructure pvRecordStructure) {
            super(linearConvertInput,pvRecordStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            double rawValue = (double)pvRawValue.get();
            double value = rawValue*slope + intercept;
            pvValue.put(value);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
    }
    
    private static  class LinearConvertOutput extends LinearConvertBase {
        
        private LinearConvertOutput(PVRecordStructure pvRecordStructure) {
            super(linearConvertOutput,pvRecordStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            double value = pvValue.get();
            double rawValue = (value -intercept)/slope;
            pvRawValue.put((int)rawValue);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
    }
}
