/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.basic.GenericBase;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Support for alarm field.
 * @author mrk
 *
 */
public class AlarmSupportFactory {
   
    private static final String alarmSupportName = "org.epics.ioc.alarm";
    /**
     * Create support for an alarm field.
     * @param pvStructure The interface to the alarm field.
     * @return The support or null if the alarm field is improperly defined.
     */
    public static Support create(PVStructure pvStructure) {
        AlarmSupportImpl impl = new AlarmSupportImpl(pvStructure);
        if(impl.isAlarmSupport()) return impl;
        return null;
    }
    /**
     * If pvField has AlarmSupport return it.
     * @param pvField The field.
     * @param locateSupport The locateSupport;
     * @return The AlarmSupport or null if not found.
     */
    public static AlarmSupport getAlarmSupport(PVField pvField,LocateSupport locateSupport) {
        Support support = locateSupport.getSupport(pvField);
        if(support!=null && (support instanceof AlarmSupportImpl)) {
            return (AlarmSupport)support;
        }
        return null;
    }
    /**
     * Find alarm support.
     * Look first in startPVField if it is a structure.
     * If not found look up the parent tree.
     * @param startPVField The starting field.
     * @param locateSupport The locateSupport.
     * @return The AlarmSupport or null if not found.
     */
    public static AlarmSupport findAlarmSupport(PVField startPVField,LocateSupport locateSupport) {
        if(startPVField==null) return null;
        PVField parentPVField;
        if(startPVField instanceof PVStructure) {
            parentPVField = startPVField;
        } else {
            parentPVField = startPVField.getParent();
        }
        while(parentPVField!=null) {
            if(parentPVField instanceof PVStructure) {
                PVStructure parentPVStructure = (PVStructure)parentPVField;
                PVField[] pvFields = parentPVStructure.getPVFields();
                for(PVField pvField : pvFields) {
                    Field field = pvField.getField();
                    Type type = field.getType();
                    if(type==Type.structure) {
                        if(field.getFieldName().equals("alarm")) {
                            Support support = locateSupport.getSupport(pvField);
                            if(support!=null && (support instanceof AlarmSupportImpl)) {
                                return (AlarmSupport)support;
                            }
                        }
                    }
                }
            }
            parentPVField = parentPVField.getParent();
        }
        return null;
    }
    
    private static class AlarmSupportImpl extends GenericBase implements AlarmSupport
    {
        private PVStructure pvAlarm;
        private RecordProcess recordProcess = null;
        
        private PVInt pvSeverity = null;
        private PVString pvMessage = null;
        
        private boolean gotAlarm;
        private boolean active = false;
        private int beginIndex = 0;
        private int currentIndex = 0;
        private String beginMessage = null;
        private String currentMessage = null;
        
        private AlarmSupportImpl parentAlarmSupport = null;
        
        private AlarmSupportImpl(PVStructure pvAlarm) {
            super(alarmSupportName,pvAlarm);
            this.pvAlarm = pvAlarm;
            
        }
        
        private boolean isAlarmSupport (){
            pvMessage = pvAlarm.getStringField("message");
            if(pvMessage==null) {
                pvAlarm.message("field message does not exist", MessageType.error);
                return false;
            }
            PVStructure pvStructure = pvAlarm.getStructureField("severity");
            if(pvStructure==null) {
                pvAlarm.message("field severity does not exist", MessageType.error);
                return false;
            }
            pvSeverity = pvStructure.getIntField("index");
            if(pvSeverity==null) {
                pvStructure.message("field index does not exist", MessageType.error);
                return false;
            }
            return true;
        }
       
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        @Override
        public void initialize(LocateSupport recordSupport) {
            // look for parent starting with parent of parent
            AlarmSupport parent = AlarmSupportFactory.findAlarmSupport(pvAlarm.getParent().getParent(),recordSupport);
            if(parent!=null) {
                parentAlarmSupport = (AlarmSupportImpl)parent;
            }
            recordProcess = recordSupport.getRecordProcess();
            super.initialize(recordSupport);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            parentAlarmSupport = null;
            super.uninitialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#beginProcess()
         */
        public void beginProcess() {
            if(active) return;
            gotAlarm = false;
            active = true;
            beginIndex = pvSeverity.get();
            beginMessage = pvMessage.get();
            currentMessage = beginMessage;
            currentIndex = 0;
            currentMessage = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#endProcess()
         */
        public void endProcess() {
            active = false;
            boolean messageChange = false;
            if(beginMessage==null) {
                if(currentMessage!=null) messageChange = true;
            } else {
                if(currentMessage==null) {
                    messageChange = true;
                } else if(!beginMessage.equals(currentMessage)) {
                    messageChange = true;
                }
            }
            if(currentIndex!=beginIndex || messageChange) {
                pvSeverity.put(currentIndex);
                pvSeverity.postPut();
                pvMessage.put(currentMessage);
                pvMessage.postPut();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setAlarm(String message, AlarmSeverity severity) {
            int newIndex = severity.ordinal();
            if(!active) {
                if(recordProcess.isActive()) {
                    beginProcess();
                } else { // record is not being processed
                    if(newIndex>0) { // raise alarm
                        pvSeverity.getPVRecord().beginGroupPut();
                        pvSeverity.put(newIndex);
                        pvSeverity.postPut();
                        pvMessage.put(message);
                        pvMessage.postPut();
                        pvSeverity.getPVRecord().endGroupPut();
                        return true;
                    } else { // no alarm just return false
                        return false;
                    }
                }
            }
            if(!gotAlarm || newIndex>currentIndex) {
                currentIndex = newIndex;
                currentMessage = message;
                gotAlarm = true;
                if(parentAlarmSupport!=null) {
                    parentAlarmSupport.setAlarm(message, severity);
                }
                return true;
            }
            return false;
        }
    }
}
