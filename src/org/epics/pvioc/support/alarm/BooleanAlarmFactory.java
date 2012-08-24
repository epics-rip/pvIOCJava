/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.alarm;

import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVScalar;
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
 * Support for booleanAlarm link.
 * @author mrk
 *
 */
public class BooleanAlarmFactory {
    /**
     * Create support for a booleanAlarm structure.
     * @param pvRecordStructure The structure.
     * @return An interface to the support or null if the supportName was not "booleanAlarm".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new BooleanAlarmImpl(pvRecordStructure);
    }
    
    
    
    private static class BooleanAlarmImpl extends AbstractSupport
    {
        private static final String supportName = "org.epics.pvioc.booleanAlarm";
        private PVRecordStructure pvRecordStructure;
        private PVStructure pvStructure;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private PVInt pvFalseSeverity;
        private PVInt pvTrueSeverity;
        private PVInt pvChangeStateSeverity;
        private PVBoolean pvValue;
        boolean prevValue;
       
        private BooleanAlarmImpl(PVRecordStructure pvRecordStructure) {
            super(supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
            pvStructure = pvRecordStructure.getPVStructure();
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            SupportState supportState = SupportState.readyForStart;
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVStructure pvParent = pvStructure.getParent();
            PVField pvField = pvParent.getSubField("value");
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
            alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecordStructure);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;
            pvFalseSeverity = pvStructure.getIntField("falseSeverity");
            if(pvFalseSeverity==null) return;
            pvTrueSeverity = pvStructure.getIntField("trueSeverity");
            if(pvTrueSeverity==null) return;
            pvChangeStateSeverity = pvStructure.getIntField("changeStateSeverity");
            if(pvChangeStateSeverity==null) return;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#start()
         */
        @Override
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
         * @see org.epics.pvioc.process.Support#stop()
         */
        @Override
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#uninitialize()
         */
        @Override
        public void uninitialize() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(noop) {
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            pvActive = null;
            pvFalseSeverity = null;
            pvTrueSeverity = null;
            pvChangeStateSeverity = null;
            setSupportState(SupportState.readyForInitialize);
        }       
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.Support#process(org.epics.pvioc.process.RecordProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            if(noop || !pvActive.get()) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            int index;
            boolean  value = pvValue.get();
            if(value!=prevValue) {
                prevValue = value;
                index = pvChangeStateSeverity.get();
                if(index>0) alarmSupport.setAlarm(
                    "changeStateAlarm",
                    AlarmSeverity.getSeverity(index),AlarmStatus.RECORD);
            }
            if(value) {
                index = pvTrueSeverity.get();
                if(index>0) alarmSupport.setAlarm(
                    "stateAlarm",
                    AlarmSeverity.getSeverity(index),AlarmStatus.RECORD);
            } else {
                index = pvFalseSeverity.get();
                if(index>0) alarmSupport.setAlarm(
                    "stateAlarm",
                    AlarmSeverity.getSeverity(index),AlarmStatus.RECORD);
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
    }
}
