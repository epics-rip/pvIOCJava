/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.DBStructureArray;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Support for alarms for an enumerated value.
 * @author mrk
 *
 */
public class EnumeratedAlarmFactory {
    /**
     * Create support for a digitalAlarm structure.
     * @param dbStructure The structure.
     * @return An interface to the support or null if the supportName was not "digitalAlarm".
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        String supportName = pvStructure.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvStructure.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new EnumeratedAlarmImpl(dbStructure);
    }
    
    private static String supportName = "enumeratedAlarm";
    
    private static class EnumeratedAlarmImpl extends AbstractSupport
    {
        private DBStructure dbStructure;
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
       
        private EnumeratedAlarmImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBField dbParent = dbStructure.getParent();
            PVField pvParent = dbParent.getPVField();
            PVField pvField = pvParent.findProperty("value");
            if(pvField==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            
            if(pvField.getField().getType()!=Type.pvStructure) {
                pvField.message("field is not an alarmSeverity structure", MessageType.error);
                return;
            }
            DBField dbField = dbStructure.getDBRecord().findDBField(pvField);
            DBStructure dbStructure = (DBStructure)dbField;
            Create create = dbStructure.getCreate();
            if(create==null || !(create instanceof Enumerated)) {
                pvField.message("interface Enumerated not found", MessageType.error);
                return;
            }
            Enumerated enumerated = (Enumerated)create;
            pvValue = enumerated.getIndexField();
            noop = false;
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;            
            dbStructure = this.dbStructure;
            DBField[] dbFields = dbStructure.getDBFields();
            Structure structure = dbStructure.getPVStructure().getStructure();
            int index;
            index = structure.getFieldIndex("stateAlarm");
            if(index<0) {
                super.message("stateAlarm does not exist", MessageType.error);
                return;
            }
            dbField = dbFields[index];
            pvField = dbField.getPVField();
            Field field = pvField.getField();
            if(field.getType()!=Type.pvArray) {
                super.message("stateAlarm is not an array", MessageType.error);
                return;
            }
            Array array = (Array)field;
            if(array.getElementType()!=Type.pvStructure) {
                super.message("stateAlarm is not a structure array", MessageType.error);
                return;
            }
            pvField.setMutable(false);
            DBStructureArray dbStateAlarm = (DBStructureArray)dbFields[index];
            if(!stateAlarmFieldsInit(dbStateAlarm.getElementDBStructures())) return;
            index = structure.getFieldIndex("changeStateAlarm");
            if(index<0) {
                super.message("changeStateAlarm does not exist", MessageType.error);
                return;
            }
            DBField tempDB = dbFields[index];
            if(tempDB.getPVField().getField().getType()!=Type.pvStructure) {
                super.message("changeStateAlarm not a structure", MessageType.error);
                return;
            }
            Structure tempStructure = (Structure)tempDB.getPVField().getField();
            if(!tempStructure.getStructureName().equals("enumeratedAlarmState")) {
                super.message("changeStateAlarm not an enumeratedAlarmState structure", MessageType.error);
                return;
            }
            DBStructure dbEnumeratedAlarmState = (DBStructure)dbFields[index];
            dbFields = dbEnumeratedAlarmState.getDBFields();
            enumerated = AlarmSeverity.getAlarmSeverity(dbFields[0]);
            if(enumerated==null) return;
            pvChangeStateAlarm = enumerated.getIndexField();
            pvChangeStateMessage = (PVString)dbFields[1].getPVField();
            setSupportState(SupportState.readyForStart);
        }
        
        private boolean stateAlarmFieldsInit(DBStructure[] dbStateAlarmFields) {
            int length = dbStateAlarmFields.length;
            for(int i=length-1; i>=0; i-- ) {
                if(dbStateAlarmFields[i]==null) {
                    length--;
                } else {
                    break;
                }
            }
            if(length==0) {
                noop = true;
                setSupportState(SupportState.readyForStart);
                return false;
            }
            pvSeverityIndex = new PVInt[length];
            pvMessage = new PVString[length];
            for(int i=0; i< length; i++) {
                DBField dbField = dbStateAlarmFields[i];
                if(dbField==null) {
                    super.message("stateAlarm has a null element",MessageType.error);
                    return false;
                }
                if(dbField.getPVField().getField().getType()!=Type.pvStructure) {
                    super.message("stateAlarm has an element that is not a structure",MessageType.error);
                    return false;
                }
                dbStructure = (DBStructure)dbField;
                DBField[] dbFields = dbStructure.getDBFields();
                Structure structure = dbStructure.getPVStructure().getStructure();
                int index = structure.getFieldIndex("severity");
                if(index<0) {
                    super.message("stateAlarm has an illegal structure element",MessageType.error);
                    return false;
                }
                dbField = dbFields[index];
                Enumerated enumerated = AlarmSeverity.getAlarmSeverity(dbField);
                if(enumerated==null) {
                    super.message("stateAlarm has an illegal structure element",MessageType.error);
                    return false;
                }
                pvSeverityIndex[i] = enumerated.getIndexField();
                dbField.getPVField().setMutable(false);
                index = structure.getFieldIndex("message");
                if(index<0) {
                    super.message("stateAlarm has an illegal structure element",MessageType.error);
                    return false;
                }
                dbField = dbFields[index];
                if(dbField.getPVField().getField().getType()!=Type.pvString) {
                    super.message("stateAlarm has an illegal structure element",MessageType.error);
                    return false ;
                }
                pvMessage[i] = (PVString)dbField.getPVField();
            }
            return true;
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
            pvSeverityIndex = null;
            pvChangeStateAlarm = null;
            pvValue = null;
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
