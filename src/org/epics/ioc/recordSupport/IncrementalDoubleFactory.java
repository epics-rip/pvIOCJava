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
 * Record that holds a double value, an input link, and output link,
 * and an array of process or output links.
 * It provides alarm support.
 * @author mrk
 *
 */
public class IncrementalDoubleFactory {
    public static Support create(DBStructure dbStructure) {
        return new IncrementalDoubleImpl(dbStructure);
    }
    
    private enum ProcessState {
        inputSupport,
        doubleAlarmSupport,
        outputSupport,
        linkArraySupport
    }
    
    static private class IncrementalDoubleImpl extends AbstractSupport
    implements IncrementalSupport, SupportProcessRequester
    {
        private static String supportName = "doubleRecord";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBField valueDBField = null;
        private PVDouble valuePVField = null;
        private DBField desiredValueDBField = null;
        private PVDouble desiredValuePVField = null;
        private Support inputSupport = null;
        private PVBoolean incrementalOutputPVField = null;
        private PVDouble rateOfChangePVField = null;
        private PVDouble lowLimitPVField = null;
        private PVDouble highLimitPVField = null;
        private Support doubleAlarmSupport = null;
        private Support outputSupport = null;
        private Support linkArraySupport = null;
        
        private double desiredValue = 0.0;
        private double value = 0.0;
        private boolean incrementalOutput = true;
        private double rateOfChange = 0.0;
        private double lowLimit = 0.0;
        private double highLimit = 0.0;
        
        private SupportProcessRequester supportProcessRequester = null;
        private ProcessState processState = ProcessState.inputSupport;
        private RequestResult finalResult = RequestResult.success;
        
        private IncrementalDoubleImpl(DBStructure dbStructure) {
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
            if(valueDBField==null) {
                index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("no value field", MessageType.error);
                    return;
                }
                valueDBField = dbFields[index];
                valuePVField = (PVDouble)valueDBField.getPVField();
            }
            if(desiredValueDBField==null) {
                index = structure.getFieldIndex("desiredValue");
                if(index<0) {
                    super.message("no desiredValue field", MessageType.error);
                    return;
                }
                desiredValueDBField = dbFields[index];
                desiredValuePVField = (PVDouble)desiredValueDBField.getPVField();
            }
            index = structure.getFieldIndex("input");
            if(index>=0) {
                inputSupport = dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("incrementalOutput");
            if(index>=0) {
                incrementalOutputPVField = (PVBoolean)dbFields[index].getPVField();
            }
            index = structure.getFieldIndex("rateOfChange");
            if(index>=0) {
                rateOfChangePVField = (PVDouble)dbFields[index].getPVField();
            }
            index = structure.getFieldIndex("controlLimit");
            if(index>=0) {
                getControlLimits((PVStructure)dbFields[index].getPVField());
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
            if(inputSupport!=null) {
                inputSupport.setField(valueDBField);
                inputSupport.initialize();
                supportState = inputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            
            if(doubleAlarmSupport!=null) {
                doubleAlarmSupport.setField(valueDBField);
                doubleAlarmSupport.initialize();
                supportState = doubleAlarmSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    return;
                }
            }
            if(outputSupport!=null) {
                outputSupport.setField(valueDBField);
                outputSupport.initialize();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) return;
            }
            if(linkArraySupport!=null) {
                linkArraySupport.setField(valueDBField);
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
            if(doubleAlarmSupport!=null) doubleAlarmSupport.uninitialize();
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            if(outputSupport!=null) outputSupport.uninitialize();
            inputSupport = null;
            doubleAlarmSupport = null;
            valueDBField = null;
            linkArraySupport = null;
            outputSupport = null;
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
            }
            computeValue();
            if(doubleAlarmSupport!=null) {
                processState = ProcessState.doubleAlarmSupport;
                doubleAlarmSupport.process(this);
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
            valueDBField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.recordSupport.IncrementalSupport#setDesiredField(org.epics.ioc.db.DBField)
         */
        public void setDesiredField(DBField dbField) {
            desiredValueDBField = dbField;
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
                computeValue();
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
                supportProcessRequester.supportProcessDone(finalResult);
                return;
            }
        }
        
        private void getControlLimits(PVStructure pvStructure) {
            PVField[] pvFields = pvStructure.getFieldPVFields();
            if(pvFields.length!=2) return;
            PVField pvField = pvFields[0];
            Field field = pvField.getField();
            if(!field.getFieldName().equals("low")) return;
            if(field.getType()!=Type.pvDouble) return;
            lowLimitPVField = (PVDouble)pvField;
            pvField = pvFields[1];
            field = pvField.getField();
            if(!field.getFieldName().equals("high")) {
                lowLimitPVField = null;
                return;
            }
            if(field.getType()!=Type.pvDouble) {
                lowLimitPVField = null;
                return;
            }
            highLimitPVField = (PVDouble)pvField;
        }
        
        private void computeValue() {
            value = valuePVField.get();
            desiredValue = desiredValuePVField.get();
            if(incrementalOutputPVField!=null) incrementalOutput = incrementalOutputPVField.get();
            if(rateOfChangePVField!=null) rateOfChange = rateOfChangePVField.get();
            if(lowLimitPVField!=null) lowLimit = lowLimitPVField.get();
            if(highLimitPVField!=null) highLimit = highLimitPVField.get();
            if(desiredValue==value) return;
            if(lowLimit<highLimit) {
                if(desiredValue<lowLimit) {
                    desiredValue = lowLimit;
                    desiredValuePVField.put(lowLimit);
                    desiredValueDBField.postPut();
                }
                if(desiredValue>highLimit) {
                    desiredValue = highLimit;
                    desiredValuePVField.put(highLimit);
                    desiredValueDBField.postPut();
                }
            }
            double newValue = desiredValue;
            if(incrementalOutput) {
                double diff = desiredValue - value;
                if(diff<0.0) {
                    newValue = value - rateOfChange;
                    if(newValue<desiredValue) newValue = desiredValue;
                } else {
                    newValue = value + rateOfChange;
                    if(newValue>desiredValue) newValue = desiredValue;
                }
            }
            valuePVField.put(newValue);
            valueDBField.postPut();
        }
    }
}
