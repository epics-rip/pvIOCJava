/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.create.EnumeratedFactory;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.DBStructureArray;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.StructureArrayData;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Record that has the following fields:
 * <ul>
 *    <li>value which must be an enum.</li>
 *    <li>registerValue which must be an int.</li>
 *    <li>numberOfBits which must be an int.</li>
 *    <li>states which must be an array of digitalState structures.</li>
 * </ul>
 * If defined it calls support for fields named: input, valueAlarm, output, and linkArray.
 * @author mrk
 *
 */
public class DigitalFactory {
    /**
     * Create the support.
     * @param dbField The field for which to create support.
     * @return The support instance.
     */
    public static Support create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvArray) {
            pvField.message("support only works for an array of structures", MessageType.fatalError);
            return null;
        }
        PVArray pvArray = (PVArray)pvField;
        if(pvArray.getArray().getElementType()!=Type.pvStructure) {
            pvField.message("support only works for an array of structures", MessageType.fatalError);
            return null;
        }
        String supportName = dbField.getSupportName();
        DBStructureArray dbArray = (DBStructureArray)dbField;
        if(supportName.equals(digitalInputName)) {
            return new DigitalInput(supportName,dbArray);
        } else if(supportName.equals(digitalOutputName)) {
            return new DigitalOutput(supportName,dbArray);
        }
        pvField.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    
    private static final String digitalInputName = "digitalInput";
    private static final String digitalOutputName = "digitalOutput";
    

    
    static private abstract class DigitalBase extends AbstractSupport
    {
        protected static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        protected static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        protected static Convert convert = ConvertFactory.getConvert();
        protected String supportName;
        protected DBStructureArray dbStates = null;
        protected PVField pvStates;
        
        protected DBField dbValue = null;
        protected PVStringArray pvValueChoices = null;
        protected DBField dbValueIndex = null;
        protected PVInt pvValueIndex = null;
        
        protected DBField dbRegisterValue = null;
        protected PVInt pvRegisterValue = null;
        
        protected int[] values = null;
        
        protected DBStructureArray dbStateAlarm = null;
        
        protected DigitalBase(String supportName,DBStructureArray dbArray) {
            super(supportName,dbArray);
            this.dbStates = dbArray;
            pvStates = dbArray.getPVField();
            this.supportName = supportName;
        }


        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            DBRecord dbRecord = dbStates.getDBRecord();
            DBField parentDBField = dbStates.getParent().getParent();
            PVField parentPVField = parentDBField.getPVField();
            PVField pvField = parentPVField.findProperty("value");
            if(pvField==null) {
                super.message("parent does not have a value field", MessageType.error);
                return;
            }
            dbValue = dbRecord.findDBField(pvField);
            if(!initValue()) return;
            pvField = parentPVField.findProperty("valueAlarm");
            if(pvField==null) {
                super.message("valueAlarm does not exist", MessageType.error);
                return;
            }
            DBField dbField = dbRecord.findDBField(pvField);
            if(!initValueAlarm(dbField)) return;
            parentDBField = dbStates.getParent();
            parentPVField = parentDBField.getPVField();
            pvField = parentPVField.findProperty("value");
            if(pvField.getField().getType()!=Type.pvInt) {
                super.message("registerValue is not an int", MessageType.error);
                return;
            }
            pvRegisterValue = (PVInt)pvField;
            if(!initFields()) return; 
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            dbValue = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */

        private boolean initValue() {
            if(dbValue==null) {
                super.message("no value field", MessageType.error);
                return false;
            }
            Create create = dbValue.getCreate();
            if(create==null || !(create instanceof Enumerated)) {
                super.message("value is not an enumerated structure", MessageType.error);
                return false;
            }
            Enumerated enumerated = (Enumerated)create;
            pvValueIndex = enumerated.getIndexField();
            DBRecord dbRecord = super.getDBField().getDBRecord();
            dbValueIndex = dbRecord.findDBField(pvValueIndex);
            pvValueChoices = enumerated.getChoicesField();
            return true;
        }
        
        private boolean initValueAlarm(DBField dbValueAlarm) {
            DBStructure dbStructure = (DBStructure)dbValueAlarm;
            PVStructure pvStructure = dbStructure.getPVStructure();
            PVField pvField = pvStructure.findProperty("stateAlarm");
            if(pvField==null) {
                super.message("valueAlarm does not have a stateAlarm field. Why???", MessageType.error);
                return false;
            }
            if(pvField.getField().getType()!=Type.pvArray) {
                super.message("valueAlarm.stateAlarm is not an array. Why???", MessageType.error);
                return false;
            }
            PVArray pvArray = (PVArray)pvField;
            if(pvArray.getArray().getElementType()!=Type.pvStructure) {
                super.message("valueAlarm.stateAlarm is not an array of structures. Why???", MessageType.error);
                return false;
            }
            dbStateAlarm = (DBStructureArray)dbStructure.getDBRecord().findDBField(pvArray);
            return true;
        }
        
        private boolean initFields() {
            // from the state fields this code 
            // 1) sets value.choices
            // 2) creates the valueAlarm.stateAlarm array
            DBStructure[] dbStatesFields = dbStates.getElementDBStructures();
            
            int nstates = dbStatesFields.length;
            if(nstates<1) return false;
            StructureArrayData structureArrayData = new StructureArrayData();
            PVStructureArray pvStructureArray = dbStates.getPVStructureArray();
            int num = pvStructureArray.get(0, nstates, structureArrayData);
            if(num!=nstates) {
                return false;
            }
            PVStructure[] pvStatesFields = structureArrayData.data;
            String[] names = new String[nstates];
            PVStructure[] pvAlarmSeveritys = new PVStructure[nstates];
            values = new int[nstates];
            PVStructureArray pvStateAlarmArray = dbStateAlarm.getPVStructureArray();
            pvStateAlarmArray.setCapacity(nstates);
            for(int indState=0; indState<nstates; indState++) {
                DBStructure dbState = dbStatesFields[indState];
                PVStructure pvState = pvStatesFields[indState];
                if(dbState==null || pvState==null) {
                    super.message(
                        "states has a null element. index " + indState,
                        MessageType.error);
                    return false;
                }
                PVString pvName = pvState.getStringField("name");
                if(pvName==null) return false;
                names[indState] = pvName.get();
                PVInt pvValue = pvState.getIntField("value");
                if(pvValue==null) return false;
                values[indState] = pvValue.get();
                PVString pvMessage = pvState.getStringField("message");
                if(pvMessage==null) return false;
                PVStructure pvStructure = pvState.getStructureField("severity", "alarmSeverity");
                if(pvStructure==null) return false;
                DBStructure dbSeverity = (DBStructure)dbStates.getDBRecord().findDBField(pvStructure);
                Enumerated enumerated;
                enumerated = AlarmSeverity.getAlarmSeverity(dbSeverity);
                if(enumerated==null) {
                    super.message(
                            "states index " + indState + " field name is not an alarmSeverity",
                            MessageType.error);
                    return false;
                }
                DBDStructure dbdStructure = DBDFactory.getMasterDBD().getStructure("enumeratedAlarmState");
                String actualFieldName = "[" + indState + "]";
                Field enumAlarmStateField = fieldCreate.createStructure(
                        actualFieldName,
                        dbdStructure.getStructureName(),
                        dbdStructure.getFields(),
                        dbdStructure.getFieldAttribute());
                PVStructure pvNewAlarmState = (PVStructure)pvDataCreate.createPVField(
                        pvStateAlarmArray,enumAlarmStateField);
                pvAlarmSeveritys[indState] = pvNewAlarmState;
                dbdStructure = DBDFactory.getMasterDBD().getStructure("alarmSeverity");
                Field severityField = fieldCreate.createStructure(
                        "severity",
                        dbdStructure.getStructureName(),
                        dbdStructure.getFields(),
                        dbdStructure.getFieldAttribute());
                severityField.setCreateName("enumerated");
                PVStructure pvNewStateSeverity = (PVStructure)pvDataCreate.createPVField(
                        pvNewAlarmState,severityField);
                convert.copyStructure(dbSeverity.getPVStructure(), pvNewStateSeverity);
                Field messageField = fieldCreate.createField("message", Type.pvString);
                PVString newMessageField = (PVString)pvDataCreate.createPVField(pvNewAlarmState, messageField);
                newMessageField.put(pvMessage.get());
                PVField[] pvFields = pvNewAlarmState.getPVFields();
                pvFields[0].replacePVField(pvNewStateSeverity);
                pvFields[1].replacePVField(newMessageField);
            }          
            pvValueChoices.put(0, nstates, names, 0);
            pvStateAlarmArray.put(0,nstates, pvAlarmSeveritys, 0);
            dbStateAlarm.replacePVField(pvStateAlarmArray);
            DBStructure[] dbFields = dbStateAlarm.getElementDBStructures();
            for(int indState=0; indState<nstates; indState++) {
                DBStructure dbStructure = (DBStructure)dbFields[indState];
                DBField[] subFields = dbStructure.getDBFields();
                EnumeratedFactory.create(subFields[0]);
            }
            return true;
        } 
    }
    
    static private class DigitalInput extends DigitalBase {
        private int prevRegisterValue = 0;
        
        private DigitalInput(String supportName,DBStructureArray dbArray) {
            super(supportName,dbArray);
        }
        
        public void process(SupportProcessRequester supportProcessRequester)
        {
            int newValue = pvRegisterValue.get();
            if(newValue!=prevRegisterValue) {
                prevRegisterValue = newValue;
                for(int i=0; i< values.length; i++) {
                    if(values[i]==newValue) {
                        pvValueIndex.put(i);
                        dbValueIndex.postPut();
                        break;
                    }
                }
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
    
    static private class DigitalOutput extends DigitalBase {
        private int prevValueIndex = 0;
        private DigitalOutput(String supportName,DBStructureArray dbArray) {
            super(supportName,dbArray);
        }
        
        public void process(SupportProcessRequester supportProcessRequester)
        {
            int value = pvValueIndex.get();
            if(prevValueIndex!=value) {
                prevValueIndex = value;
                if(value<0 || value>=values.length) {
                    pvStates.message("Illegal value", MessageType.warning);
                } else {
                    pvRegisterValue.put(value);
                }
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
