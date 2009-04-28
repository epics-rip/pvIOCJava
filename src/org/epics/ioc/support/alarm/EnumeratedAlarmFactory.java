/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Support for alarms for an enumerated value.
 * @author mrk
 *
 */
public class EnumeratedAlarmFactory {
    /**
     * Create support for a digitalAlarm structure.
     * @param pvStructure The structure.
     * @return An interface to the support.
     */
    public static Support create(PVStructure pvStructure) {
        return new EnumeratedAlarmImpl(pvStructure);
    }
    
    private static class EnumeratedAlarmImpl extends AbstractSupport
    {
        private static final String supportName = "org.epics.ioc.enumeratedAlarm";
        private PVStructure pvStructure;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        
        private PVInt[] pvSeverityIndex = null;
        private PVString[] pvMessage;
        private PVInt pvChangeStateAlarm;
        private PVString pvChangeStateMessage;
        
        private PVInt pvValue;
        
        private int prevValue = 0;
       
        private EnumeratedAlarmImpl(PVStructure pvStructure) {
            super(supportName,pvStructure);
            this.pvStructure = pvStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize(LocateSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVStructure pvStruct = pvStructure.getParent().getStructureField("value");
            if(pvStruct==null) return;
            Enumerated enumerated = EnumeratedFactory.getEnumerated(pvStruct);
            if(enumerated==null) {
                pvStruct.message(" is not an enumerated structure", MessageType.error);
                return;
            }
            pvValue = enumerated.getIndex();
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;
            pvStruct = pvStructure.getStructureField("stateAlarm");
            if(pvStruct==null) return;
            if(!stateAlarmFieldsInit(pvStruct.getPVFields())) return;
            pvStruct = pvStructure.getStructureField("changeStateAlarm");
            if(pvStruct==null) return;
            PVStructure pvStruct1 = pvStruct.getStructureField("severity");
            enumerated = AlarmSeverity.getAlarmSeverity(pvStruct1);
            if(enumerated==null) return;
            pvChangeStateAlarm = enumerated.getIndex();
            pvChangeStateMessage = pvStruct.getStringField("message");
            if(pvChangeStateMessage==null) return;
            setSupportState(SupportState.readyForStart);
        }
        
        private boolean stateAlarmFieldsInit(PVField[] pvStateAlarmFields) {
            int length = pvStateAlarmFields.length;
            if(length==0) {
                noop = true;
                setSupportState(SupportState.readyForStart);
                return false;
            }
            pvSeverityIndex = new PVInt[length];
            pvMessage = new PVString[length];
            for(int i=0; i< length; i++) {
                PVField pvField = pvStateAlarmFields[i];
                if(pvField.getField().getType()!=Type.structure) {
                    super.message("stateAlarm has an element that is not a structure",MessageType.error);
                    return false;
                }
                PVStructure pvStructure = (PVStructure)pvField;
                PVField pvSevr = pvStructure.getSubField("severity");
                if(pvSevr==null) return false;
                Enumerated enumerated = AlarmSeverity.getAlarmSeverity(pvSevr);
                if(enumerated==null) return false;
                pvSeverityIndex[i] = enumerated.getIndex();
                pvMessage[i] = pvStructure.getStringField("message");
                if(pvMessage[i]==null) return false;
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start(AfterStart afterStart) {
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
            pvSeverityIndex = null;
            pvChangeStateAlarm = null;
            pvValue = null;
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
            String message = pvStructure.getFullFieldName();
            int  value = pvValue.get();
            if(value<pvSeverityIndex.length) {
                PVInt pvInt = pvSeverityIndex[value];
                int alarmValue = pvInt.get();
                if(alarmValue>0) {
                    alarmSupport.setAlarm(
                            pvMessage[value].get(),
                            AlarmSeverity.getSeverity(alarmValue));
                }
            } else {
                alarmSupport.setAlarm(
                        message + "alarmSupport: value out of bounds",
                        AlarmSeverity.major);
            }
            if(prevValue!=value) {
                prevValue = value;
                index = pvChangeStateAlarm.get();
                alarmSupport.setAlarm(
                        pvChangeStateMessage.get(),
                        AlarmSeverity.getSeverity(index));
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
    }
}
