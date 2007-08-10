/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;
import org.epics.ioc.util.*;
import org.epics.ioc.process.*;

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
        if(startDBField==null) return null;
        DBField parentDBField;
        if(startDBField instanceof DBStructure) {
            parentDBField = startDBField;
        } else {
            parentDBField = startDBField.getParent();
        }
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
        private DBField dbSeverity = null;
        private PVString pvMessage = null;
        
        private boolean gotAlarm;
        private boolean active = false;
        private int beginIndex = 0;
        
        private AlarmImpl parentAlarmSupport = null;
        
        private AlarmImpl(DBStructure dbAlarm) {
            super(alarmSupportName,dbAlarm);
            this.dbAlarm = dbAlarm;
            DBField[] dbFields = dbAlarm.getFieldDBFields();
            pvAlarm = dbAlarm.getPVStructure();
            PVField[] pvFields = pvAlarm.getFieldPVFields();
            Structure structure = (Structure)pvAlarm.getField();
            int index = structure.getFieldIndex("severity");
            pvSeverity = (PVMenu)pvFields[index];
            dbSeverity = dbFields[index];
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
            }
            super.setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            parentAlarmSupport = null;
            super.setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#beginProcess()
         */
        public void beginProcess() {
            gotAlarm = false;
            active = true;
            beginIndex = pvSeverity.getIndex();
            if(beginIndex!=0)  {
                pvSeverity.setIndex(0);
                pvMessage.put(null);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#endProcess()
         */
        public void endProcess() {
            active = false;         
            int currentIndex = pvSeverity.getIndex();
            if(currentIndex!=beginIndex) {
                dbSeverity.postPut();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setAlarm(String message, AlarmSeverity severity) {
            checkForIllegalRequest();
            int newIndex = severity.ordinal();
            int currentIndex = pvSeverity.getIndex();
            if(!gotAlarm || newIndex>currentIndex) {
                pvSeverity.setIndex(newIndex);
                pvMessage.put(message);
                gotAlarm = true;
                if(parentAlarmSupport!=null) {
                    parentAlarmSupport.setAlarm(message, severity);
                }
                return true;
            }
            return false;
        }
        
        private void checkForIllegalRequest() {
            if(active) return;
            pvAlarm.message("illegal request because record is not active",MessageType.info);
            throw new IllegalStateException("record is not active");
        }
    }
}
