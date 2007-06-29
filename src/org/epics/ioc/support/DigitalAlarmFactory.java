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
public class DigitalAlarmFactory {
    /**
     * Create support for a digitalAlarm link.
     * @param dbLink The link.
     * @return An interface to the support or null if the supportName was not "digitalAlarm".
     */
    public static Support create(DBLink dbLink) {
        PVLink pvLink = dbLink.getPVLink();
        String supportName = pvLink.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvLink.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new DigitalAlarmImpl(dbLink);
    }
    
    private static String supportName = "digitalAlarm";
    
    private static class DigitalAlarmImpl extends AbstractLinkSupport
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        private MenuArrayData menuArrayData = new MenuArrayData();
        private PVMenuArray pvMenuArray = null;
        private PVMenu pvChangeOfStateMenu = null;
        
        private PVEnum pvValue;
        
        private int prevIndex = 0;
       
        private DigitalAlarmImpl(DBLink dbLink) {
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
                super.message("setField was not called with an enum field", MessageType.error);
                noop = true;
                return;
            }
            PVStructure configStructure = super.getConfigStructure("digitalAlarm", false);
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
            if(pvActive==null) return;
            pvMenuArray = (PVMenuArray)configStructure.getArrayField("stateSeverity", Type.pvMenu);
            if(pvMenuArray==null) return;
            pvChangeOfStateMenu = configStructure.getMenuField("changeStateAlarm","alarmSeverity");
            if(pvChangeOfStateMenu==null) return;
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
            prevIndex = pvValue.getIndex();
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
            pvMenuArray = null;
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
            int  value = pvValue.getIndex();
            if(value<pvMenuArray.getLength()) {
                pvMenuArray.get(value, 1, menuArrayData);
                PVMenu pvMenu = menuArrayData.data[menuArrayData.offset];
                if(pvMenu!=null) {
                    if(pvMenu.getMenu().getMenuName().equals("alarmSeverity")) {
                        index = pvMenu.getIndex();
                        if(index>0) alarmSupport.setAlarm(
                                message + " state alarm",
                                AlarmSeverity.getSeverity(index));
                    }
                }
            } else {
                alarmSupport.setAlarm(
                        message + ":alarmSupport value out of bounds",
                        AlarmSeverity.major);
            }
            if(prevIndex!=value) {
                prevIndex = value;
                index = pvChangeOfStateMenu.getIndex();
                if(index>0) alarmSupport.setAlarm(
                        message + " change of state alarm",
                        AlarmSeverity.getSeverity(index));
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            PVField pvField = dbField.getPVField();
            if(pvField.getField().getType()!=Type.pvEnum) {
                super.message("setField: field type is not enum", MessageType.error);
                return;
            }
            pvValue = (PVEnum)pvField;
        }
    }
}
