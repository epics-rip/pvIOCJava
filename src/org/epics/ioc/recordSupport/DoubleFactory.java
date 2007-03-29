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
public class DoubleFactory {
    public static Support create(DBStructure dbStructure) {
        return new DoubleImpl(dbStructure);
    }
    
    private enum ProcessState {
        inputSupport,
        doubleAlarmSupport,
        outputSupport,
        linkArraySupport
    }
    
    static private class DoubleImpl extends AbstractSupport
    implements SupportProcessRequestor
    {
        private static String supportName = "doubleRecord";
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private DBField valueDBField = null;
        private Support inputSupport = null;
        private Support doubleAlarmSupport = null;
        private Support outputSupport = null;
        private Support linkArraySupport = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private ProcessState processState = ProcessState.inputSupport;
        private RequestResult finalResult = RequestResult.success;
        
        private DoubleImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getDBRecord();
            pvRecord = dbRecord.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            Structure structure = (Structure)pvRecord.getField();
            DBField[] dbFields = dbRecord.getDBStructure().getFieldDBFields();
            int index;
            if(valueDBField==null) {
                index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("no value field", MessageType.error);
                    return;
                }
                valueDBField = dbFields[index];
            }
            index = structure.getFieldIndex("input");
            if(index>=0) {
                inputSupport = dbFields[index].getSupport();
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
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
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
    }
}
