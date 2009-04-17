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
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Support for a doubleAlarm link.
 * @author mrk
 *
 */
public class DoubleAlarmFactory {
    /**
     * Create support for an doubleAlarm structure.
     * @param pvStructure The structure.
     * @return An interface to the support.
     */
    public static Support create(PVStructure pvStructure) {
        return new DoubleAlarmImpl(pvStructure);
    }
    
    
    private static class DoubleAlarmImpl extends AbstractSupport
    {
        private PVStructure pvStructure;
        
        private AlarmSupport alarmSupport;
        
        private PVInt pvOutOfRange;
        private PVBoolean pvActive;
        private PVDouble pvHystersis;
        
        private PVStructure pvAlarmIntervalArray = null;
        private PVDouble[] pvAlarmIntervalValue = null;
        private PVInt[] pvAlarmIntervalSeverity = null;
        private PVString[] pvAlarmIntervalMessage = null;
        
        private PVDouble pvValue;
        private double lastAlarmIntervalValue;
        private int lastAlarmSeverityIndex;
       
        private DoubleAlarmImpl(PVStructure pvStructure) {
            super("doubleAlarm",pvStructure);
            this.pvStructure = pvStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(LocateSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            pvValue = pvStructure.getParent().getDoubleField("value");
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
            pvHystersis = pvStructure.getDoubleField("hystersis");
            if(pvHystersis==null) return;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start(AfterStart afterStart) {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            PVField[] pvFields = pvAlarmIntervalArray.getPVFields();
            int size = pvFields.length;
            if(size<=0) {
                super.message("invalid interval", MessageType.error);
                return;
            }
            pvAlarmIntervalValue = new PVDouble[size];
            pvAlarmIntervalSeverity = new PVInt[size];
            pvAlarmIntervalMessage = new PVString[size];
            for(int i=0; i<size; i++) {
                PVField pvField = pvFields[i];
                if(pvField.getField().getType()!=Type.structure) {
                    super.message("invalid interval. not a structure", MessageType.error);
                    return;
                }
                PVStructure pvStructure = (PVStructure)pvField;
                PVDouble pvValue = pvStructure.getDoubleField("value");
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
            double  val = pvValue.get();
            int len = pvAlarmIntervalValue.length;
            double intervalValue = 0;
            for(int i=0; i<len; i++) {
                intervalValue = pvAlarmIntervalValue[i].get();
                if(val<=intervalValue) {
                    int sevIndex = pvAlarmIntervalSeverity[i].get();
                    raiseAlarm(intervalValue,val,sevIndex,pvAlarmIntervalMessage[i].get());
                    return;
                }
            }
            int outOfRange = pvOutOfRange.get();
            // intervalValue is pvAlarmIntervalValue[length-1].get();
            raiseAlarm(intervalValue,val,outOfRange,"out of range");
        }
        
        private void raiseAlarm(double intervalValue,double val,int severityIndex,String message) {
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(severityIndex);
            if(severityIndex<lastAlarmSeverityIndex) {
                double diff = lastAlarmIntervalValue - val;
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
