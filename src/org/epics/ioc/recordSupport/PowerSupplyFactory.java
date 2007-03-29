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
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class PowerSupplyFactory {
    public static Support create(DBStructure dbStructure) {
        return new PowerSupplyImpl(dbStructure);
    }
    
    private enum ProcessState {
        inputSupport,
        voltageInputSupport,
        currentOutputSupport,
        doubleAlarmSupport,
        outputSupport,
        linkArraySupport
    }
    
    static private class PowerSupplyImpl extends AbstractSupport
    implements SupportProcessRequestor
    {
        private static String supportName = "doubleRecord";
        private PVStructure pvStructure;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private DBField powerDBField = null;
        private PVDouble powerPVField = null;
        private Support inputSupport = null;
        
        private DBField currentDBField = null;
        private PVDouble currentPVField = null;
        private DBField voltageDBField = null;
        private PVDouble voltagePVField = null;
        
        private Support voltageInputSupport = null;
        private Support currentOutputSupport = null;
        
        private Support doubleAlarmSupport = null;
        private Support outputSupport = null;
        private Support linkArraySupport = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private ProcessState processState = ProcessState.inputSupport;
        private RequestResult finalResult = RequestResult.success;
        
        private double power;
        private double voltage;
        private double current;
        
        private PowerSupplyImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            pvStructure = dbStructure.getPVStructure();
            dbRecord = dbStructure.getDBRecord();
            pvRecord = dbRecord.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            powerPVField = pvStructure.getDoubleField("power");
            powerDBField = dbRecord.findDBField(powerPVField);
            currentPVField = pvStructure.getDoubleField("current");
            currentDBField = dbRecord.findDBField(currentPVField);
            voltagePVField = pvStructure.getDoubleField("voltage");
            voltageDBField = dbRecord.findDBField(voltagePVField);
            Structure structure = (Structure)pvRecord.getField();
            DBField[] dbFields = dbRecord.getDBStructure().getFieldDBFields();
            int index;
            index = structure.getFieldIndex("input");
            if(index>=0) {
                inputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("voltageInput");
            if(index>=0) {
                voltageInputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("currentOutput");
            if(index>=0) {
                currentOutputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("doubleAlarm");
            if(index>=0) {
                doubleAlarmSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("output");
            if(index>=0) {
                outputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("linkArray");
            if(index>=0) {
                linkArraySupport = dbFields[index].getSupport();
            }
            
            if(voltageInputSupport!=null) {
                voltageInputSupport.setField(voltageDBField);
                voltageInputSupport.initialize();
                supportState = voltageInputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            
            if(currentOutputSupport!=null) {
                currentOutputSupport.setField(currentDBField);
                currentOutputSupport.initialize();
                supportState = currentOutputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            
            if(doubleAlarmSupport!=null) {
                doubleAlarmSupport.setField(powerDBField);
                doubleAlarmSupport.initialize();
                supportState = doubleAlarmSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    return;
                }
            }
            if(outputSupport!=null) {
                outputSupport.setField(powerDBField);
                outputSupport.initialize();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            if(linkArraySupport!=null) {
                linkArraySupport.setField(powerDBField);
                linkArraySupport.initialize();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    if(doubleAlarmSupport!=null) doubleAlarmSupport.uninitialize();
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
            if(voltageInputSupport!=null) {
                voltageInputSupport.start();
                supportState = voltageInputSupport.getSupportState();
                if(supportState!=SupportState.ready) return;
            }
            if(currentOutputSupport!=null) {
                currentOutputSupport.start();
                supportState = currentOutputSupport.getSupportState();
                if(supportState!=SupportState.ready) return;
            }
            if(doubleAlarmSupport!=null) {
                doubleAlarmSupport.start();
                supportState = doubleAlarmSupport.getSupportState();
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
                    if(doubleAlarmSupport!=null) doubleAlarmSupport.stop();
                    return;
                }
            }
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    if(doubleAlarmSupport!=null) doubleAlarmSupport.stop();
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
            if(voltageInputSupport!=null) voltageInputSupport.stop();
            if(currentOutputSupport!=null) currentOutputSupport.stop();
            if(doubleAlarmSupport!=null) doubleAlarmSupport.stop();
            if(linkArraySupport!=null) linkArraySupport.stop();
            if(outputSupport!=null) outputSupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(inputSupport!=null) inputSupport.uninitialize();
            if(voltageInputSupport!=null) voltageInputSupport.uninitialize();
            if(currentOutputSupport!=null) currentOutputSupport.uninitialize();
            if(doubleAlarmSupport!=null) doubleAlarmSupport.uninitialize();
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            if(outputSupport!=null) outputSupport.uninitialize();
            inputSupport = null;
            doubleAlarmSupport = null;
            powerDBField = null;
            linkArraySupport = null;
            outputSupport = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,"process")) {
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("supportProcessRequestor is null");
            }
            this.supportProcessRequestor = supportProcessRequestor;
            finalResult = RequestResult.success;
            if(inputSupport!=null) {
                processState = ProcessState.inputSupport;
                inputSupport.process(this);
            } else if(voltageInputSupport!=null) {
                processState = ProcessState.voltageInputSupport;
                voltageInputSupport.process(this);
            } else {
                computeCurrent();
                if(currentOutputSupport!=null) {
                    processState = ProcessState.currentOutputSupport;
                    currentOutputSupport.process(this);
                } else if(doubleAlarmSupport!=null) {
                    processState = ProcessState.doubleAlarmSupport;
                    doubleAlarmSupport.process(this);
                } else if(outputSupport!=null) {
                    processState = ProcessState.outputSupport;
                    outputSupport.process(this);
                } else if(linkArraySupport!=null) {
                    processState = ProcessState.linkArraySupport;
                    linkArraySupport.process(this);           
                } else {
                    supportProcessRequestor.supportProcessDone(RequestResult.success);
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult.compareTo(finalResult)>0) {
                finalResult = requestResult;
            }
            switch(processState) {
            case inputSupport:
                if(voltageInputSupport!=null) {
                    processState = ProcessState.voltageInputSupport;
                    voltageInputSupport.process(this);
                    return;
                }
            case voltageInputSupport:
                computeCurrent();
                if(currentOutputSupport!=null) {
                    processState = ProcessState.currentOutputSupport;
                    currentOutputSupport.process(this);
                    return;
                }
            case currentOutputSupport:
                if(doubleAlarmSupport!=null) {
                    processState = ProcessState.doubleAlarmSupport;
                    doubleAlarmSupport.process(this);
                    return;
                }
            case doubleAlarmSupport:
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
                supportProcessRequestor.supportProcessDone(finalResult);
                return;
            }
        }
        private void computeCurrent() {
            power = powerPVField.get();
            voltage = powerPVField.get();
            if(voltage==0.0) {
                current = 0.0;
            } else {
                current = power/voltage;
            }
            currentPVField.put(current);
            currentDBField.postPut();
        }
    }
}
