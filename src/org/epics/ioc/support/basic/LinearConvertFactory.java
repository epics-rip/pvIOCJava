/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;
import org.epics.ioc.util.*;

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
        PVScalar pvScalar = pvAuxInfo.getInfo("support");
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
    
    private static final String linearConvertInput = "linearConvertInput";
    private static final String linearConvertOutput = "linearConvertOutput";
    
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
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,linearConvertInput)) return;
            PVStructure parentPVField = pvStructure.getParent();
            pvRawValue = getInt(pvStructure,"value");
            if(pvRawValue==null) return;
            parentPVField = parentPVField.getParent();
            pvValue = getDouble(parentPVField,"value");
            if(pvValue==null) return;
            pvEngUnitsLow = getDouble(pvStructure,"pvEngUnitsLow");
            if(pvEngUnitsLow==null) return;
            pvEngUnitsHigh = getDouble(pvStructure,"pvEngUnitsHigh");
            if(pvEngUnitsHigh==null) return;
            pvDeviceLow = getInt(pvStructure,"pvDeviceLow");
            if(pvDeviceLow==null) return;
            pvDeviceHigh = getInt(pvStructure,"pvDeviceHigh");
            if(pvDeviceHigh==null) return;
            pvSlope = getDouble(pvStructure,"pvSlope");
            if(pvSlope==null) return;
            pvIntercept = getDouble(pvStructure,"pvIntercept");
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
