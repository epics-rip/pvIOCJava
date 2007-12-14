/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.create.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for booleanAlarm link.
 * @author mrk
 *
 */
public class BooleanAlarmFactory {
    /**
     * Create support for a booleanAlarm structure.
     * @param dbStructure The structure.
     * @return An interface to the support or null if the supportName was not "booleanAlarm".
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        String supportName = pvStructure.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvStructure.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new BooleanAlarmImpl(dbStructure);
    }
    
    private static String supportName = "booleanAlarm";
    
    private static class BooleanAlarmImpl extends AbstractSupport
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private PVInt pvFalseAlarm;
        private PVInt pvTrueAlarm;
        private PVInt pvChangeStateAlarm;
        
        private PVBoolean pvValue;
        boolean prevValue;
       
        private BooleanAlarmImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            SupportState supportState = SupportState.readyForStart;
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBField dbParent = dbStructure.getParent();
            PVField pvParent = dbParent.getPVField();
            PVField pvField = pvParent.findProperty("value");
            if(pvField==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            DBField valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            pvField = valueDBField.getPVField();
            if(pvField.getField().getType()!=Type.pvBoolean) {
                super.message("field type is not boolean", MessageType.error);
                return;
            }
            pvValue = (PVBoolean)pvField;
            noop = false;
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            
            DBField[] dbFields = dbStructure.getDBFields();
            Structure structure = dbStructure.getPVStructure().getStructure();
            int index = structure.getFieldIndex("falseAlarm");
            if(index<0) {
                super.message("falseAlarm does not exist", MessageType.error);
                return;
            }
            Enumerated enumerated = AlarmSeverity.getAlarmSeverity(dbFields[index]);
            if(enumerated==null) return;
            pvFalseAlarm = enumerated.getIndexField();
            
            index = structure.getFieldIndex("trueAlarm");
            if(index<0) {
                super.message("trueAlarm does not exist", MessageType.error);
                return;
            }
            enumerated = AlarmSeverity.getAlarmSeverity(dbFields[index]);
            if(enumerated==null) return;
            pvTrueAlarm = enumerated.getIndexField();

            index = structure.getFieldIndex("changeStateAlarm");
            if(index<0) {
                super.message("changeStateAlarm does not exist", MessageType.error);
                return;
            }
            enumerated = AlarmSeverity.getAlarmSeverity(dbFields[index]);
            if(enumerated==null) return;
            pvChangeStateAlarm = enumerated.getIndexField();
          
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
            if(noop) {
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            boolean active = pvActive.get();
            if(!active) return;
            int index;
            String message = pvStructure.getFullFieldName();
            boolean  value = pvValue.get();
            if(value!=prevValue) {
                prevValue = value;
                index = pvChangeStateAlarm.get();
                if(index>0) alarmSupport.setAlarm(
                    message + " changeState",
                    AlarmSeverity.getSeverity(index));
            }
            if(value) {
                index = pvTrueAlarm.get();
                if(index>0) alarmSupport.setAlarm(
                    message + " true ",
                    AlarmSeverity.getSeverity(index));
            } else {
                index = pvFalseAlarm.get();
                if(index>0) alarmSupport.setAlarm(
                    message + " false ",
                    AlarmSeverity.getSeverity(index));
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
    }
}
