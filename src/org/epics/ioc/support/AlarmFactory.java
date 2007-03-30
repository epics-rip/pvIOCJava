/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.*;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for alarm field.
 * @author mrk
 *
 */
public class AlarmFactory {
   
    private static final String alarmSupportName = "alarm";
    /**
     * Create support for an alarm field.
     * @param dbStructure The interface to the alarm field.
     * @return The support or null if the alarm field is improperly defined.
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        Structure structure = (Structure)pvStructure.getField();
        String supportName = pvStructure.getSupportName();
        if(!supportName.equals(alarmSupportName)) {
            pvStructure.message("no support for " + supportName, MessageType.fatalError);
            return null;
        }
        if(!structure.getStructureName().equals("alarm")) {
            pvStructure.message("not an alarm structure" + supportName, MessageType.fatalError);
        }
        return new AlarmImpl(dbStructure);
    }
    
    public static AlarmSupport findAlarmSupport(DBField startDBField) {
        DBField parentDBField = startDBField.getParent();
        while(parentDBField!=null) {
            if(parentDBField instanceof DBStructure) {
                DBStructure parentDBStructure = (DBStructure)parentDBField;
                DBField[] dbFields = parentDBStructure.getFieldDBFields();
                for(DBField dbField : dbFields) {
                    Field field = dbField.getPVField().getField();
                    Type type = field.getType();
                    if(type==Type.pvStructure) {
                        Structure structure = (Structure)field;
                        if(structure.getStructureName().equals("alarm")) {
                            Support support = dbField.getSupport();
                            if(support instanceof AlarmImpl) {
                                return (AlarmSupport)support;
                            }
                        }
                    }
                }
            }
            parentDBField = parentDBField.getParent();
        }
        return null;
    }
    
    private static class AlarmImpl extends AbstractSupport implements AlarmSupport
    {
        private DBStructure dbAlarm;
        private PVStructure pvAlarm;
        
        private PVMenu pvSeverity = null;
        private PVString pvMessage = null;
        private boolean isNew;
        private String prevMessage;
        private int prevIndex;
        private String newMessage;
        private int newIndex;
        
        private boolean active = false;
        
        private AlarmImpl parentAlarmSupport = null;
        private List<AlarmImpl> childAlarmList = new ArrayList<AlarmImpl>();
        
        private AlarmImpl(DBStructure dbAlarm) {
            super(alarmSupportName,dbAlarm);
            this.dbAlarm = dbAlarm;
            pvAlarm = dbAlarm.getPVStructure();
            PVField[] pvFields = pvAlarm.getFieldPVFields();
            Structure structure = (Structure)pvAlarm.getField();
            int index = structure.getFieldIndex("severity");
            pvSeverity = (PVMenu)pvFields[index];
            index = structure.getFieldIndex("message");
            pvMessage = (PVString)pvFields[index];
        }  
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize(org.epics.ioc.process.SupportCreation)
         */
        public void initialize() {
            // look for parent starting with parent of parent
            AlarmSupport parent = AlarmFactory.findAlarmSupport(dbAlarm.getParent().getParent());
            if(parent!=null) {
                parentAlarmSupport = (AlarmImpl)parent;
                parentAlarmSupport.addChildAlarm(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            childAlarmList.clear();
            parentAlarmSupport = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#beginProcess()
         */
        public void beginProcess() {
            isNew = false;
            prevMessage = pvMessage.get();
            prevIndex = pvSeverity.getIndex();
            newMessage = null;
            newIndex = 0;
            ListIterator<AlarmImpl> iter = childAlarmList.listIterator();
            while(iter.hasNext()) {
                AlarmImpl alarm = iter.next();
                alarm.beginProcess();
            }
            active = true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#endProcess()
         */
        public void endProcess() {
            ListIterator<AlarmImpl> iter = childAlarmList.listIterator();
            while(iter.hasNext()) {
                AlarmImpl alarm = iter.next();
                alarm.endProcess();
            }
            active = false;
            if(!isNew) {
                int index = pvSeverity.getIndex();
                String message = pvMessage.get();
                if(index==0 && (message==null || message.length()==0)) return;
            }
            if(prevIndex!=newIndex || !prevMessage.equals(newMessage)) {
                pvSeverity.setIndex(newIndex);                
                pvMessage.put(newMessage);                
                dbAlarm.postPut();
            }
            active = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#getAlarmStamp(org.epics.ioc.util.AlarmStamp)
         */
        public void getAlarmStamp(AlarmStamp alarmStamp) {
            checkForIllegalRequest();
            alarmStamp.message = getMessage();
            alarmStamp.severity = getSeverity();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#getSeverity()
         */
        public AlarmSeverity getSeverity() {
            checkForIllegalRequest();
            return AlarmSeverity.getSeverity(newIndex);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#getStatus()
         */
        public String getMessage() {
            checkForIllegalRequest();
            return newMessage;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setAlarm(String message, AlarmSeverity severity) {
            checkForIllegalRequest();
            if(!isNew || severity.ordinal()>newIndex) {  
                isNew = true;
                newMessage = message;
                newIndex = severity.ordinal();
                if(parentAlarmSupport!=null) {
                    parentAlarmSupport.setAlarm(newMessage, severity);
                }
                return true;
            }
            return false;
        }
        
        private void addChildAlarm(AlarmImpl alarm) {
            childAlarmList.add(alarm);
        }
        
        private void checkForIllegalRequest() {
            if(active) return;
            pvAlarm.message("illegal request because record is not active",MessageType.info);
            throw new IllegalStateException("record is not active");
        }
    }
}
