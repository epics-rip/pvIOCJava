/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;
import org.epics.ioc.create.*;
import org.epics.ioc.db.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.util.*;
import org.epics.ioc.process.*;

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
        DBNonScalarArray dbArray = (DBNonScalarArray)dbField;
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
        protected DBNonScalarArray dbStates = null;
        protected PVField pvStates;
        
        protected DBField dbValue = null;
        protected PVStringArray pvValueChoices = null;
        protected DBField dbValueIndex = null;
        protected PVInt pvValueIndex = null;
        
        protected DBField dbRegisterValue = null;
        protected PVInt pvRegisterValue = null;
        
        protected int[] values = null;
        
        protected DBNonScalarArray dbStateSeverity = null;
        
        protected DigitalBase(String supportName,DBNonScalarArray dbArray) {
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
            Create create = parentDBField.getCreate();
            if(create==null || !(create instanceof Enumerated)) {
                super.message("the value is not an enumerated structure", MessageType.error);
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
            DBStructure dbStructure = (DBStructure)dbValue;
            PVStructure pvStructure = dbStructure.getPVStructure();
            Structure structure = pvStructure.getStructure();
            DBField[] dbFields = dbStructure.getFieldDBFields();
            PVField[] pvFields = dbStructure.getPVStructure().getFieldPVFields();
            int index;
            index = structure.getFieldIndex("index");
            if(index<0) {
                super.message("value does not have an index field. Why???", MessageType.error);
                return false;
            }
            dbValueIndex = dbFields[index];
            PVField pvField = pvFields[index];
            if(pvField.getField().getType()!=Type.pvInt) {
                super.message("value.index is not an integer. Why???", MessageType.error);
                return false;
            }
            pvValueIndex = (PVInt)pvFields[index];
            index = structure.getFieldIndex("choices");
            if(index<0) {
                super.message("value does not have a choices field. Why???", MessageType.error);
                return false;
            }
            pvField = pvFields[index];
            if(pvField.getField().getType()!=Type.pvArray) {
                super.message("value.choices is not an array. Why???", MessageType.error);
                return false;
            }
            PVArray pvArray = (PVArray)pvField;
            if(pvArray.getArray().getElementType()!=Type.pvString) {
                super.message("value.choices is not an array of string. Why???", MessageType.error);
                return false;
            }
            pvValueChoices = (PVStringArray)pvArray;
            return true;
        }
        
        private boolean initValueAlarm(DBField dbValueAlarm) {
            DBStructure dbStructure = (DBStructure)dbValueAlarm;
            PVStructure pvStructure = dbStructure.getPVStructure();
            Structure structure = pvStructure.getStructure();
            PVField[] pvFields = dbStructure.getPVStructure().getFieldPVFields();
            int index;
            index = structure.getFieldIndex("stateSeverity");
            if(index<0) {
                super.message("valueAlarm does not have a stateSeverity field. Why???", MessageType.error);
                return false;
            }
            PVField pvField = pvFields[index];
            if(pvField.getField().getType()!=Type.pvArray) {
                super.message("valueAlarm.stateSeverity is not an array. Why???", MessageType.error);
                return false;
            }
            PVArray pvArray = (PVArray)pvField;
            if(pvArray.getArray().getElementType()!=Type.pvStructure) {
                super.message("valueAlarm.stateSeverity is not an array of structures. Why???", MessageType.error);
                return false;
            }
            dbStateSeverity = (DBNonScalarArray)dbStructure.getDBRecord().findDBField(pvArray);
            return true;
        }
        
        private boolean initFields() {
            DBField[] dbStatesFields = dbStates.getElementDBFields();
            int nstates = dbStatesFields.length;
            if(nstates<1) return false;
            String[] names = new String[nstates];
            PVStructure[] pvSeverities = new PVStructure[nstates];
            values = new int[nstates];
            PVStructureArray pvStateSeverityArray = (PVStructureArray)dbStateSeverity.getPVField();
            pvStateSeverityArray.setCapacity(nstates);
            for(int indState=0; indState<nstates; indState++) {
                DBField dbField = dbStatesFields[indState];
                if(dbField==null) {
                    super.message(
                        "states has a null element. index " + indState,
                        MessageType.error);
                    return false;
                }
                Field field = dbField.getPVField().getField();
                if(field.getType()!=Type.pvStructure) {
                    super.message(
                        "states index " + indState + " is not a structure",
                        MessageType.error);
                    return false;
                }
                DBStructure dbStateSeverity = (DBStructure)dbField;                
                DBField[] dbStateFields = dbStateSeverity.getFieldDBFields();
                PVStructure pvStructure = (PVStructure)dbField.getPVField();
                Structure structure = pvStructure.getStructure();
                PVField[] pvFields = pvStructure.getFieldPVFields();
                int indField = structure.getFieldIndex("name");
                if(indField<0) {
                    super.message(
                            "states index " + indState + " does not have field name",
                            MessageType.error);
                    return false;
                }
                PVField pvField = pvFields[indField];
                if(pvField.getField().getType()!=Type.pvString) {
                    super.message(
                            "states index " + indState + " field name is not a string",
                            MessageType.error);
                    return false;
                }
                PVString pvName= (PVString)pvField;
                names[indState] = pvName.get();
                indField = structure.getFieldIndex("value");
                if(indField<0) {
                    super.message(
                            "states index " + indState + " does not have field value",
                            MessageType.error);
                    return false;
                }
                pvField = pvFields[indField];
                if(pvField.getField().getType()!=Type.pvInt) {
                    super.message(
                            "states index " + indState + " field name is not an int",
                            MessageType.error);
                    return false;
                }
                PVInt pvValue= (PVInt)pvField;
                values[indState] = pvValue.get();
                indField = structure.getFieldIndex("severity");
                if(indField<0) {
                    super.message(
                            "states index " + indState + " does not have field severity",
                            MessageType.error);
                    return false;
                }
                pvField = pvFields[indField];
                Enumerated enumerated;
                enumerated = AlarmSeverity.getAlarmSeverity(dbStateFields[indField]);
                if(enumerated==null) {
                    super.message(
                            "states index " + indState + " field name is not an alarmSeverity",
                            MessageType.error);
                    return false;
                }
                String actualFieldName = "[" + indState + "]";
                DBDStructure dbdStructure = DBDFactory.getMasterDBD().getStructure("alarmSeverity");
                Field newField = fieldCreate.createStructure(
                        actualFieldName,
                        dbdStructure.getStructureName(),
                        dbdStructure.getFields(),
                        dbdStructure.getFieldAttribute());
                newField.setCreateName("enumerated");
                PVStructure pvStateSeverity = (PVStructure)pvDataCreate.createPVField(
                        pvStateSeverityArray,newField);
                pvSeverities[indState] = pvStateSeverity;
                convert.copyStructure((PVStructure)pvField, pvStateSeverity);
            }          
            pvValueChoices.put(0, nstates, names, 0);
            pvStateSeverityArray.put(0,nstates, pvSeverities, 0);
            dbStateSeverity.replacePVArray();
            DBField[] dbFields = dbStateSeverity.getElementDBFields();
            for(int indState=0; indState<nstates; indState++) {
                EnumeratedFactory.create(dbFields[indState]);
            }
            return true;
        } 
    }
    
    static private class DigitalInput extends DigitalBase {
        private int prevRegisterValue = 0;
        
        private DigitalInput(String supportName,DBNonScalarArray dbArray) {
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
        private DigitalOutput(String supportName,DBNonScalarArray dbArray) {
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
