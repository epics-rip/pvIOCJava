/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;

import org.epics.ioc.util.*;

/**
 * Support for booleanAlarm link.
 * @author mrk
 *
 */
public class BooleanAlarmFactory {
    /**
     * Create support for a booleanAlarm structure.
     * @param pvStructure The structure.
     * @return An interface to the support or null if the supportName was not "booleanAlarm".
     */
    public static Support create(PVStructure pvStructure) {
        return new BooleanAlarmImpl(pvStructure);
    }
    
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    
    private static class BooleanAlarmImpl extends AbstractSupport
    {
        private PVStructure pvStructure;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private PVInt pvFalseAlarm;
        private PVString pvFalseMessage;
        private PVInt pvTrueAlarm;
        private PVString pvTrueMessage;
        private PVInt pvChangeStateAlarm;
        private PVString pvChangeStateMessage;
        
        private PVBoolean pvValue;
        boolean prevValue;
       
        private BooleanAlarmImpl(PVStructure pvStructure) {
            super("booleanAlarm",pvStructure);
            this.pvStructure = pvStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            SupportState supportState = SupportState.readyForStart;
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVField pvParent = pvStructure.getParent();
            PVField pvField = pvProperty.findProperty(pvParent, "value");
            if(pvField==null) {
                pvStructure.message("value not found", MessageType.error);
                return;
            }
            if(pvField.getField().getType()!=Type.scalar) {
                super.message("value is not a boolean", MessageType.error);
                return;
            }
            PVScalar pvScalar = (PVScalar)pvField;
            if(pvScalar.getScalar().getScalarType()!=ScalarType.pvBoolean) {
                super.message("field is not a boolean", MessageType.error);
                return;
            }
            pvValue = (PVBoolean)pvField;
            noop = false;
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;
            
            PVStructure pvStruct = pvStructure.getStructureField("falseAlarm");
            if(pvStruct==null) return;
            Enumerated enumerated = AlarmSeverity.getAlarmSeverity(pvStruct);
            if(enumerated==null) return;
            pvFalseAlarm = enumerated.getIndex();
            pvFalseMessage = pvStructure.getStringField("falseMessage");
            if(pvFalseMessage==null) return;
            
            pvStruct = pvStructure.getStructureField("trueAlarm");
            if(pvStruct==null) return;
            enumerated = AlarmSeverity.getAlarmSeverity(pvStruct);
            if(enumerated==null) return;
            pvTrueAlarm = enumerated.getIndex();
            pvTrueMessage = pvStructure.getStringField("trueMessage");
            if(pvTrueMessage==null) return;
            
            pvStruct = pvStructure.getStructureField("changeStateAlarm");
            if(pvStruct==null) return;
            enumerated = AlarmSeverity.getAlarmSeverity(pvStruct);
            if(enumerated==null) return;
            pvChangeStateAlarm = enumerated.getIndex();
            pvChangeStateMessage = pvStructure.getStringField("changeStateMessage");
            if(pvChangeStateMessage==null) return;

            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(noop) {
                setSupportState(SupportState.ready);
                return;
            }
            prevValue = pvValue.get();
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(noop) {
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            pvActive = null;
            pvFalseAlarm = null;
            pvTrueAlarm = null;
            pvChangeStateAlarm = null;
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(noop || !pvActive.get()) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            int index;
            boolean  value = pvValue.get();
            if(value!=prevValue) {
                prevValue = value;
                index = pvChangeStateAlarm.get();
                if(index>0) alarmSupport.setAlarm(
                    pvChangeStateMessage.get(),
                    AlarmSeverity.getSeverity(index));
            }
            if(value) {
                index = pvTrueAlarm.get();
                if(index>0) alarmSupport.setAlarm(
                    pvTrueMessage.get(),
                    AlarmSeverity.getSeverity(index));
            } else {
                index = pvFalseAlarm.get();
                if(index>0) alarmSupport.setAlarm(
                    pvFalseMessage.get(),
                    AlarmSeverity.getSeverity(index));
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
    }
}
