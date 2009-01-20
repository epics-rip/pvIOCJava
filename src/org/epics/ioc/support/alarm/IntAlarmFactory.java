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
 * Support for an intAlarm link.
 * @author mrk
 *
 */
public class IntAlarmFactory {
    /**
     * Create support for an intAlarm structure.
     * @param pvStructure The structure.
     * @return An interface to the support.
     */
    public static Support create(PVStructure pvStructure) {
        return new IntAlarmImpl(pvStructure);
    }
    
    private static class IntAlarmImpl extends AbstractSupport
    {
        private PVStructure pvStructure;
        
        private AlarmSupport alarmSupport;
        
        private PVInt pvOutOfRange;
        private PVBoolean pvActive;
        private PVInt pvHystersis;
        
        private PVStructure pvAlarmIntervalArray = null;
        private PVInt[] pvAlarmIntervalValue = null;
        private PVInt[] pvAlarmIntervalSeverity = null;
        private PVString[] pvAlarmIntervalMessage = null;
        
        private PVInt pvValue;
        private int lastAlarmIntervalValue;
        private int lastAlarmSeverityIndex;
       
        private IntAlarmImpl(PVStructure pvStructure) {
            super("intAlarm",pvStructure);
            this.pvStructure = pvStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            pvValue = pvStructure.getParent().getIntField("value");
            if(pvValue==null) return;
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;
            pvAlarmIntervalArray = pvStructure.getStructureField("interval");
            if(pvAlarmIntervalArray==null) return;  
            PVStructure pvStruct = pvStructure.getStructureField("outOfRange");
            if(pvStruct==null) return;
            Enumerated enumerated = AlarmSeverity.getAlarmSeverity(pvStruct);
            if(enumerated==null) return;
            pvOutOfRange = enumerated.getIndex();
            pvHystersis = pvStructure.getIntField("hystersis");
            if(pvHystersis==null) return;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            PVField[] pvFields = pvAlarmIntervalArray.getPVFields();
            int size = pvFields.length;
            if(size<=0) {
                super.message("invalid interval", MessageType.error);
                return;
            }
            pvAlarmIntervalValue = new PVInt[size];
            pvAlarmIntervalSeverity = new PVInt[size];
            pvAlarmIntervalMessage = new PVString[size];
            for(int i=0; i<size; i++) {
                PVField pvField = pvFields[i];
                if(pvField.getField().getType()!=Type.structure) {
                    super.message("invalid interval. not a structure", MessageType.error);
                    return;
                }
                PVStructure pvStructure = (PVStructure)pvField;
                PVInt pvValue = pvStructure.getIntField("value");
                if(pvValue==null) return;
                pvAlarmIntervalValue[i] = pvValue;
                PVStructure pvStruct = pvStructure.getStructureField("severity");
                if(pvStruct==null) return;
                
                Enumerated enumerated = AlarmSeverity.getAlarmSeverity(pvStruct);
                if(enumerated==null) {
                    super.message("invalid interval severity field is not alarmSeverity", MessageType.error);
                    return;
                }
                pvAlarmIntervalSeverity[i] = enumerated.getIndex();
                PVString pvMessage = pvStructure.getStringField("message");
                if(pvMessage==null) return;
                pvAlarmIntervalMessage[i] = pvMessage;
            }
            lastAlarmSeverityIndex = 0;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            pvAlarmIntervalValue = null;
            pvAlarmIntervalSeverity = null;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(pvActive.get()) checkAlarm();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                

        private void checkAlarm() {
            boolean active = pvActive.get();
            if(!active) return;
            int  val = pvValue.get();
            int len = pvAlarmIntervalValue.length;
            int intervalValue = 0;
            for(int i=0; i<len; i++) {
                intervalValue = pvAlarmIntervalValue[i].get();
                if(val<=intervalValue) {
                    int sevIndex = pvAlarmIntervalSeverity[i].get();
                    raiseAlarm(intervalValue,val,sevIndex,pvAlarmIntervalMessage[i].get());
                    return;
                }
            }
            int outOfRange = pvOutOfRange.get();
            // intervalValue is pvAlarmIntervalValue[len-1].get();
            raiseAlarm(intervalValue,val,outOfRange,"out of range");
        }
        
        private void raiseAlarm(int intervalValue,int val,int severityIndex,String message) {
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(severityIndex);
            if(severityIndex<lastAlarmSeverityIndex) {
                int diff = lastAlarmIntervalValue - val;
                if(diff<0) diff = -diff;
                if(diff<pvHystersis.get()) {
                    alarmSeverity = AlarmSeverity.getSeverity(lastAlarmSeverityIndex);
                    intervalValue = lastAlarmIntervalValue;
                }
            }
            if(alarmSeverity==AlarmSeverity.none) {
                lastAlarmSeverityIndex = severityIndex;
                return;
            }
            alarmSupport.setAlarm(message, alarmSeverity);
            lastAlarmIntervalValue = intervalValue;
            lastAlarmSeverityIndex = severityIndex;
        }
    }
}
