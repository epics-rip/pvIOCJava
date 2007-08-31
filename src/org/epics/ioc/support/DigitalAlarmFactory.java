/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
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
        return new DigitalAlarmImpl(dbStructure);
    }
    
    private static String supportName = "digitalAlarm";
    
    private static class DigitalAlarmImpl extends AbstractSupport
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVBoolean pvActive;
        
        private PVInt[] pvInts = null;
        private PVInt pvChangeStateAlarm;
        
        private PVInt pvValue;
        
        private int prevValue = 0;
       
        private DigitalAlarmImpl(DBStructure dbStructure) {
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
            noop = false;
            if(pvValue==null) {
                super.message("setField was not called with an enumerated structure field",
                    MessageType.error);
                noop = true;
                return;
            }
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                return;
            }
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;
            
            DBField[] dbFields = dbStructure.getFieldDBFields();
            Structure structure = dbStructure.getPVStructure().getStructure();
            int index;
            Enumerated enumerated;
            
            index = structure.getFieldIndex("stateSeverity");
            if(index<0) {
                super.message("stateSeverity does not exist", MessageType.error);
                return;
            }
            DBField dbField = dbFields[index];
            PVField pvField = dbField.getPVField();
            Field field = pvField.getField();
            if(field.getType()!=Type.pvArray) {
                super.message("stateSeverity is not an array", MessageType.error);
                return;
            }
            Array array = (Array)field;
            if(array.getElementType()!=Type.pvStructure) {
                super.message("stateSeverity is not a structure array", MessageType.error);
                return;
            }
            pvField.setMutable(false);
            DBNonScalarArray dbStateSeverity = (DBNonScalarArray)dbFields[index];
            DBField[] dbStateSeverityFields = dbStateSeverity.getElementDBFields();
            int length = dbStateSeverityFields.length;
            for(int i=length-1; i>=0; i-- ) {
                if(dbStateSeverityFields[i]==null) {
                    length--;
                } else {
                    break;
                }
            }
            if(length==0) {
                noop = true;
                return;
            }
            pvInts = new PVInt[length];
            for(int i=0; i< length; i++) {
                dbField = dbStateSeverityFields[i];
                if(dbField==null ||
                        (enumerated = AlarmSeverity.getAlarmSeverity(dbFields[index]))==null) {
                    super.message("stateSeverity has an element that is not an enumerated structure",
                        MessageType.error);
                    return;
                }
                pvInts[i] = enumerated.getIndexField();
                dbField.getPVField().setMutable(false);
            }
            
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
            pvInts = null;
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
            if(value<pvInts.length) {
                PVInt pvInt = pvInts[value];
                int alarmValue = pvInt.get();
                if(alarmValue>0) {
                    alarmSupport.setAlarm(
                            message + " state alarm",
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
                if(index>0) alarmSupport.setAlarm(
                        message + " changeOfState alarm",
                        AlarmSeverity.getSeverity(index));
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            PVField pvField = dbField.getPVField();
            if(pvField.getField().getType()!=Type.pvStructure) {
                pvField.message("field is not an alarmSeverity structure", MessageType.error);
                return;
            }
            DBStructure dbStructure = (DBStructure)dbField;
            Create create = dbStructure.getCreate();
            if(create==null || !(create instanceof Enumerated)) {
                pvField.message("interface Enumerated not found", MessageType.error);
                return;
            }
            Enumerated enumerated = (Enumerated)create;
            pvValue = enumerated.getIndexField();
        }
    }
}
