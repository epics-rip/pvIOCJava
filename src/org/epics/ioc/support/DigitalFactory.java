/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.pv.*;
import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.util.*;

/**
 * Record that has the following fields:
 * <ul>
 *    <li>value which must be an enum. If value is not present then setField must have an enum field.</li>
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
     * Create the support for the record or structure.
     * @param dbStructure The field for which to create support.
     * @return The support instance.
     */
    public static Support create(DBStructure dbStructure) {
        return new DigitalImpl(dbStructure);
    }
    
    private enum ProcessState {
        inputSupport,
        outputSupport,
        valueAlarmSupport
    }
    
    static private class DigitalImpl extends AbstractSupport
    implements SupportProcessRequester
    {
        private static String supportName = "digital";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        
        private DBField dbValue = null;
        private PVStringArray pvValueChoices = null;
        private DBField dbValueIndex = null;
        private PVInt pvValueIndex = null;
        
        private DBField dbRegisterValue = null;
        private PVInt pvRegisterValue = null;
        private DBNonScalarArray dbStates = null;
        private int[] values = null;
        private int numberOfBits = 0;
        private int mask = 0;
        private Support inputSupport = null;
        private Support outputSupport = null;
        
        private Support valueAlarmSupport = null;
        private PVStructureArray pvStateSeverityArray = null;
        
        private SupportProcessRequester supportProcessRequester = null;
        private ProcessState processState = ProcessState.inputSupport;
        private RequestResult finalResult = RequestResult.success;
        
        private int prevValueIndex;
        private int prevRegisterValue;
        
        
        private DigitalImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }


        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            Structure structure = (Structure)pvStructure.getField();
            DBField[] dbFields = dbStructure.getFieldDBFields();
            int index;
            DBField dbField = null;
            if(dbValue==null) {
                index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("no value field", MessageType.error);
                    return;
                }
                setField(dbFields[index]);         
            }
            if(!initValue()) return;
            if(dbValue==null) {
                super.message("no value field", MessageType.error);
                return;
            }
            index = structure.getFieldIndex("registerValue");
            if(index<0) {
                super.message("no registerValue field", MessageType.error);
                return;
            }
            dbField = dbFields[index];
            if(dbField.getPVField().getField().getType()!=Type.pvInt) {
                super.message("value is not an int", MessageType.error);
                return;
            }
            dbRegisterValue = dbFields[index];
            pvRegisterValue = (PVInt)dbRegisterValue.getPVField();
            index = structure.getFieldIndex("numberOfBits");
            if(index<0) {
                super.message("no numberOfBits field", MessageType.error);
                return;
            }
            dbField = dbFields[index];
            if(dbField.getPVField().getField().getType()!=Type.pvInt) {
                super.message("numberOfBits is not an int", MessageType.error);
                return;
            }
            PVInt pvInt = (PVInt)dbFields[index].getPVField();
            numberOfBits = pvInt.get();
            if(numberOfBits<=0) {
                super.message("numberOfBits is <=0", MessageType.error);
                return;
            }
            mask = 1;
            for(int i=1; i<numberOfBits; i++) {
                mask <<= 1;
                mask |= 1;
            }
            index = structure.getFieldIndex("states");
            if(index<0) {
                super.message("no states field", MessageType.error);
                return;
            }
            dbField = dbFields[index];
            Field field = dbField.getPVField().getField();
            if(field.getType()!=Type.pvArray) {
                super.message("states is not array", MessageType.error);
                return;
            }
            Array array = (Array)field;
            if(array.getElementType()!=Type.pvStructure) {
                super.message("states is not an array of structures", MessageType.error);
                return;
            }
            dbStates = (DBNonScalarArray)dbFields[index];
            index = structure.getFieldIndex("input");
            if(index>=0) {
                inputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("output");
            if(index>=0) {
                outputSupport = dbFields[index].getSupport();
            }            
            index = structure.getFieldIndex("valueAlarm");
            if(index<0) {
                super.message("valueAlarm does not exist", MessageType.error);
                return;
            }
            if(!initValueAlarm(dbFields[index])) return;
            if(!initFields()) return;
            if(inputSupport!=null) {
                inputSupport.setField(dbRegisterValue);
                inputSupport.initialize();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            if(outputSupport!=null) {
                outputSupport.setField(dbRegisterValue);
                outputSupport.initialize();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    return;
                }
            }
            if(valueAlarmSupport!=null) {
                valueAlarmSupport.setField(dbValue);
                valueAlarmSupport.initialize();
                supportState = valueAlarmSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    if(outputSupport!=null) outputSupport.uninitialize();
                    return;
                }
            }            
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            prevRegisterValue = pvRegisterValue.get();
            prevValueIndex = pvValueIndex.get();
            if(inputSupport!=null) {
                inputSupport.start();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.ready) return;
            }
            if(outputSupport!=null) {
                outputSupport.start();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    return;
                }
            }
            if(valueAlarmSupport!=null) {
                valueAlarmSupport.start();
                supportState = valueAlarmSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    if(outputSupport!=null) outputSupport.stop();
                    return;
                }
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(inputSupport!=null) inputSupport.stop();
            if(outputSupport!=null) outputSupport.stop();
            if(valueAlarmSupport!=null) valueAlarmSupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            dbValue = null;
            if(inputSupport!=null) inputSupport.uninitialize();
            if(outputSupport!=null) outputSupport.uninitialize();
            if(valueAlarmSupport!=null) valueAlarmSupport.uninitialize();
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequester==null) {
                throw new IllegalStateException("supportProcessRequester is null");
            }
            this.supportProcessRequester = supportProcessRequester;
            finalResult = RequestResult.success;
            if(inputSupport!=null) {
                processState = ProcessState.inputSupport;
                inputSupport.process(this);
                return;
            } else {
                int value = pvValueIndex.get();
                if(prevValueIndex!=value) {
                    prevRegisterValue = values[prevValueIndex];
                    pvRegisterValue.put(prevRegisterValue);
                    dbRegisterValue.postPut();
                }
            }
            int newValue = pvRegisterValue.get();
            if(newValue!=prevRegisterValue) {
                prevRegisterValue = newValue;
                newValue &= mask;
                for(int i=0; i< values.length; i++) {
                    if(values[i]==newValue) {
                        pvValueIndex.put(i);
                        dbValueIndex.postPut();
                        prevValueIndex = i;
                        break;
                    }
                }
            }
            if(outputSupport!=null) {
                processState = ProcessState.outputSupport;
                outputSupport.process(this);
                return;
            }
            if(valueAlarmSupport!=null) {
                processState = ProcessState.valueAlarmSupport;
                valueAlarmSupport.process(this);
                return;
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            Create create = dbField.getCreate();
            if(create==null || !(create instanceof Enumerated)) {
                dbField.getPVField().message("value is not an enumerated structure", MessageType.error);
                return;
            }
            dbValue = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult.compareTo(finalResult)>0) {
                finalResult = requestResult;
            }
            switch(processState) {
            case inputSupport: 
                {
                    int newValue = pvRegisterValue.get();
                    if(newValue!=prevRegisterValue) {
                        prevRegisterValue = newValue;
                        newValue &= mask;
                        for(int i=0; i< values.length; i++) {
                            if(values[i]==newValue) {
                                pvValueIndex.put(i);
                                dbValueIndex.postPut();
                                break;
                            }
                        }
                    }
                    if(outputSupport!=null) {
                        processState = ProcessState.outputSupport;
                        outputSupport.process(this);
                        return;
                    }
                }
                // fall through
            case outputSupport:
                if(valueAlarmSupport!=null) {
                    processState = ProcessState.valueAlarmSupport;
                    valueAlarmSupport.process(this);
                    return;
                }
                //fall through
            case valueAlarmSupport:
                supportProcessRequester.supportProcessDone(finalResult);
                return;
            }
            
        }
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
            pvStateSeverityArray = (PVStructureArray)pvArray;
            return true;
        }
        
        private boolean initFields() {
            DBField[] dbStatesFields = dbStates.getElementDBFields();
            int nstates = dbStatesFields.length;
            if(nstates<1) return false;
            String[] names = new String[nstates];
            PVStructure[] pvSeverities = new PVStructure[nstates];
            values = new int[nstates];
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
                pvSeverities[indState] = (PVStructure)pvField;
            }
            pvValueChoices.put(0, names.length, names, 0);
            pvStateSeverityArray.put(0, pvSeverities.length, pvSeverities, 0);
            return true;
        } 
    }
}
