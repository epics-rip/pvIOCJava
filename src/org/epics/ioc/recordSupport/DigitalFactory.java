/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.recordSupport;

import org.epics.ioc.pv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.support.*;
import org.epics.ioc.util.*;

/**
 * Record that holds a int value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class DigitalFactory {
    public static Support create(DBStructure dbStructure) {
        return new DigitalImpl(dbStructure);
    }
    
    private enum ProcessState {
        inputSupport,
        digitalAlarmSupport,
        outputSupport,
        linkArraySupport
    }
    
    static private class DigitalImpl extends AbstractSupport
    implements SupportProcessRequester
    {
        private static String supportName = "digitalRecord";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private AlarmSupport alarmSupport;
        private DBEnum dbValue = null;
        private DBField dbRawValue = null;
        private PVInt pvRawValue = null;
        private DBNonScalarArray dbStates = null;
        private LinkSupport inputSupport = null;
        private LinkSupport digitalAlarmSupport = null;
        private LinkSupport outputSupport = null;
        private Support linkArraySupport = null;
        private int[] values = null;
        private int numberOfBits = 0;
        private int mask = 0;
        
        private int prevValue;
        private SupportProcessRequester supportProcessRequester = null;
        private ProcessState processState = ProcessState.inputSupport;
        private RequestResult finalResult = RequestResult.success;
        
        
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
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            int index;
            DBField dbField = null;
            if(dbValue==null) {
                index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("no value field", MessageType.error);
                    return;
                }
                dbField = dbFields[index];
                if(dbField.getPVField().getField().getType()!=Type.pvEnum) {
                    super.message("value is not an enum", MessageType.error);
                    return;
                }
                dbValue = (DBEnum)dbFields[index];
            }
            index = structure.getFieldIndex("rawValue");
            if(index<0) {
                super.message("no rawValue field", MessageType.error);
                return;
            }
            dbField = dbFields[index];
            if(dbField.getPVField().getField().getType()!=Type.pvInt) {
                super.message("value is not an int", MessageType.error);
                return;
            }
            dbRawValue = dbFields[index];
            pvRawValue = (PVInt)dbRawValue.getPVField();
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
                inputSupport = (LinkSupport)dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("digitalAlarm");
            if(index>=0) {
                digitalAlarmSupport = (LinkSupport)dbFields[index].getSupport();
            }
            if(!initFields()) return;
            index = structure.getFieldIndex("output");
            if(index>=0) {
                outputSupport = (LinkSupport)dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("linkArray");
            if(index>=0) {
                linkArraySupport = dbFields[index].getSupport();
            }
            if(inputSupport!=null) {
                inputSupport.setField(dbRawValue);
                inputSupport.initialize();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }            
            if(digitalAlarmSupport!=null) {
                digitalAlarmSupport.setField(dbValue);
                digitalAlarmSupport.initialize();
                supportState = digitalAlarmSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    return;
                }
            }
            if(outputSupport!=null) {
                outputSupport.setField(dbValue);
                outputSupport.initialize();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    if(digitalAlarmSupport!=null) digitalAlarmSupport.uninitialize();
                    return;
                }
            }
            if(linkArraySupport!=null) {
                linkArraySupport.setField(dbValue);
                linkArraySupport.initialize();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    if(digitalAlarmSupport!=null) digitalAlarmSupport.uninitialize();
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
            if(inputSupport!=null) {
                inputSupport.start();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.ready) return;
            }
            if(digitalAlarmSupport!=null) {
                digitalAlarmSupport.start();
                supportState = digitalAlarmSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    return;
                }
            }
            if(outputSupport!=null) {
                outputSupport.start();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    if(digitalAlarmSupport!=null) digitalAlarmSupport.stop();
                    return;
                }
            }
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    if(digitalAlarmSupport!=null) digitalAlarmSupport.stop();
                    if(outputSupport!=null) outputSupport.stop();
                    return;
                }
            }
            prevValue = pvRawValue.get();
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(inputSupport!=null) inputSupport.stop();
            if(digitalAlarmSupport!=null) digitalAlarmSupport.stop();
            if(outputSupport!=null) outputSupport.stop();
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(inputSupport!=null) inputSupport.uninitialize();
            if(digitalAlarmSupport!=null) digitalAlarmSupport.uninitialize();
            if(outputSupport!=null) outputSupport.uninitialize();
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            inputSupport = null;
            digitalAlarmSupport = null;
            outputSupport = null;
            dbValue = null;
            linkArraySupport = null;
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
            } else if(digitalAlarmSupport!=null) {
                processState = ProcessState.digitalAlarmSupport;
                digitalAlarmSupport.process(this);
            } else if(outputSupport!=null) {
                processState = ProcessState.outputSupport;
                outputSupport.process(this);
            } else if(linkArraySupport!=null) {
                processState = ProcessState.linkArraySupport;
                linkArraySupport.process(this);           
            } else {
                supportProcessRequester.supportProcessDone(RequestResult.success);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            if(dbField.getPVField().getField().getType()!=Type.pvEnum) {
                super.message("setField: field type is not enum", MessageType.error);
                return;
            }
            dbValue = (DBEnum)dbField;
            
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
                int newValue = pvRawValue.get();
                if(newValue!=prevValue) {
                    prevValue = newValue;
                    newValue &= mask;
                    for(int i=0; i< values.length; i++) {
                        if(values[i]==newValue) {
                            dbValue.setIndex(i);
                            break;
                        }
                    }
                }
                if(digitalAlarmSupport!=null) {
                    processState = ProcessState.digitalAlarmSupport;
                    digitalAlarmSupport.process(this);
                    return;
                }
            case digitalAlarmSupport:
                if(outputSupport!=null) {
                    processState = ProcessState.outputSupport;
                    outputSupport.process(this);
                    return;
                }
            case outputSupport:
                if(linkArraySupport!=null) {
                    processState = ProcessState.linkArraySupport;
                    linkArraySupport.process(this);
                    return;
                }
            case linkArraySupport:
                supportProcessRequester.supportProcessDone(finalResult);
                return;
            }
        }
        
        private boolean initFields() {
            DBField[] dbFields = dbStates.getElementDBFields();
            int nstates = dbFields.length;
            if(nstates<1) return false;
            String[] names = new String[nstates];
            PVMenu[] menus = new PVMenu[nstates];
            values = new int[nstates];
            for(int indState=0; indState<nstates; indState++) {
                DBField dbField = dbFields[indState];
                if(dbFields==null) {
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
                Structure structure = (Structure)field;
                PVStructure pvStructure = (PVStructure)dbField.getPVField();
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
                if(pvField.getField().getType()!=Type.pvMenu) {
                    super.message(
                            "states index " + indState + " field name is not a menu",
                            MessageType.error);
                    return false;
                }
                Menu menu = (Menu)pvField.getField();
                if(!menu.getMenuName().equals("alarmSeverity")) {
                    super.message(
                            "states index " + indState + " field name is not an alarmSeverity menu",
                            MessageType.error);
                    return false;
                }
                menus[indState] = (PVMenu)pvField;
            }
            PVEnum pvEnum = dbValue.getPVEnum();
            pvEnum.setChoices(names);
            if(digitalAlarmSupport==null) return true;
            PVStructure configStructure = digitalAlarmSupport.getConfigStructure("digitalAlarm",false);
            if(configStructure==null) return true;
            PVArray pvStateSeverity = configStructure.getArrayField("stateSeverity", Type.pvMenu);
            if(pvStateSeverity==null) return true;
            if(pvStateSeverity.getArray().getElementType()!=Type.pvMenu) return true;
            ((PVMenuArray)pvStateSeverity).put(0, nstates, menus, 0);
            return true;
        } 
    }
}
