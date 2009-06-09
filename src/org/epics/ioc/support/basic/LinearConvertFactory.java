/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Not implemented.
 * @author mrk
 *
 */
public class LinearConvertFactory {
    /**
     * Factory creation method.
     * @param pvStructure The field to support.
     * @return The Support interface.
     */
    public static Support create(PVStructure pvStructure) {
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
            return new LinearConvertInput(pvStructure);
        }
        if(supportName.equals(linearConvertOutput)) {
            return new LinearConvertOutput(pvStructure);
        }
        if(!supportName.equals(supportName)) {
            pvStructure.message(
                "does not have support " + supportName,MessageType.error);
            return null;
        }
        return new LinearConvertInput(pvStructure);
    }
    
    private static final String linearConvertInput = "org.epics.ioc.linearConvertInputFactory";
    private static final String linearConvertOutput = "org.epics.ioc.linearConvertOutputFactory";
    
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
        
        
        protected LinearConvertBase(String supportName,PVStructure pvStructure) {
            super(supportName,pvStructure);
            this.pvStructure = pvStructure;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        @Override
        public void initialize(LocateSupport recordSupport) {
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
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            super.setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
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
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            super.setSupportState(SupportState.readyForStart);
        }
        
        private PVDouble getDouble(PVStructure parent,String fieldName) {
            PVField pvField = parent.getSubField(fieldName);
            if(pvField==null) {
                parent.message("does not have field " + fieldName, MessageType.error);
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
        
        private LinearConvertInput(PVStructure pvStructure) {
            super(linearConvertInput,pvStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            double rawValue = (double)pvRawValue.get();
            double value = rawValue*slope + intercept;
            pvValue.put(value);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
    }
    
    private static  class LinearConvertOutput extends LinearConvertBase {
        
        private LinearConvertOutput(PVStructure pvStructure) {
            super(linearConvertOutput,pvStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            double value = pvValue.get();
            double rawValue = (value -intercept)/slope;
            pvRawValue.put((int)rawValue);
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        
    }
}
