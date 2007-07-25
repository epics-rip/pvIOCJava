/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
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
     * Create support for a booleanAlarm link.
     * @param dbLink The link.
     * @return An interface to the support or null if the supportName was not "booleanAlarm".
     */
    public static Support create(DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        String supportName = pvLink.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvLink.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new BooleanAlarmImpl(dbLink);
    }
    
    private static String supportName = "booleanAlarm";
    
    private static class BooleanAlarmImpl extends AbstractLinkSupport
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private PVMenu pvFalseAlarm;
        private PVMenu pvTrueAlarm;
        private PVMenu pvChangeStateAlarm;
        
        private PVBoolean pvValue;
        boolean prevValue;
       
        private BooleanAlarmImpl(DBLink dbLink) {
            super(supportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            SupportState supportState = SupportState.readyForStart;
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            noop = false;
            if(pvValue==null) {
                super.message("setField was not called with a boolean field", MessageType.error);
                noop = true;
                return;
            }
            PVStructure configStructure = super.getConfigStructure("booleanAlarm", false);
            if(configStructure==null) {
                noop = true;
                setSupportState(supportState);
                return;
            }
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = configStructure.getBooleanField("active");
            pvFalseAlarm = configStructure.getMenuField("falseAlarm", "alarmSeverity");
            pvTrueAlarm = configStructure.getMenuField("trueAlarm", "alarmSeverity");
            pvChangeStateAlarm = configStructure.getMenuField("changeStateAlarm", "alarmSeverity");
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
            String message = pvLink.getFullFieldName();
            boolean  value = pvValue.get();
            if(value!=prevValue) {
                prevValue = value;
                index = pvChangeStateAlarm.getIndex();
                if(index>0) alarmSupport.setAlarm(
                    message + " changeState",
                    AlarmSeverity.getSeverity(index));
            }
            if(value) {
                index = pvTrueAlarm.getIndex();
                if(index>0) alarmSupport.setAlarm(
                    message + " true ",
                    AlarmSeverity.getSeverity(index));
            } else {
                index = pvFalseAlarm.getIndex();
                if(index>0) alarmSupport.setAlarm(
                    message + " false ",
                    AlarmSeverity.getSeverity(index));
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            PVField pvField = dbField.getPVField();
            if(pvField.getField().getType()!=Type.pvBoolean) {
                super.message("setField: field type is not boolean", MessageType.error);
                return;
            }
            pvValue = (PVBoolean)pvField;
        }
    }
}
