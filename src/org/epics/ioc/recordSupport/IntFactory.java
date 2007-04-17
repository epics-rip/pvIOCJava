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
public class IntFactory {
    public static Support create(DBStructure dbStructure) {
        return new IntImpl(dbStructure);
    }
    
    private enum ProcessState {
        inputSupport,
        intAlarmSupport,
        outputSupport,
        linkArraySupport
    }
    
    static private class IntImpl extends AbstractSupport
    implements SupportProcessRequester
    {
        private static String supportName = "intRecord";
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBField valueDBField = null;
        private LinkSupport inputSupport = null;
        private LinkSupport intAlarmSupport = null;
        private LinkSupport outputSupport = null;
        private Support linkArraySupport = null;
        private SupportProcessRequester supportProcessRequester = null;
        private ProcessState processState = ProcessState.inputSupport;
        private RequestResult finalResult = RequestResult.success;
        
        private IntImpl(DBStructure dbStructure) {
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
            }
            index = structure.getFieldIndex("input");
            if(index>=0) {
                inputSupport = (LinkSupport)dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("intAlarm");
            if(index>=0) {
                intAlarmSupport = (LinkSupport)dbFields[index].getSupport();
            }
            index = structure.getFieldIndex("output");
            if(index>=0) {
                outputSupport = (LinkSupport)dbFields[index].getSupport();
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
            if(intAlarmSupport!=null) {
                intAlarmSupport.setField(valueDBField);
                intAlarmSupport.initialize();
                supportState = intAlarmSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    return;
                }
            }
            if(outputSupport!=null) {
                outputSupport.setField(valueDBField);
                outputSupport.initialize();
                supportState = outputSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    if(intAlarmSupport!=null) intAlarmSupport.uninitialize();
                    return;
                }
            }
            if(linkArraySupport!=null) {
                linkArraySupport.setField(valueDBField);
                linkArraySupport.initialize();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
                    if(intAlarmSupport!=null) intAlarmSupport.uninitialize();
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
            if(intAlarmSupport!=null) {
                intAlarmSupport.start();
                supportState = intAlarmSupport.getSupportState();
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
                    if(intAlarmSupport!=null) intAlarmSupport.stop();
                    return;
                }
            }
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                supportState = linkArraySupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    if(inputSupport!=null) inputSupport.stop();
                    if(intAlarmSupport!=null) intAlarmSupport.stop();
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
            if(intAlarmSupport!=null) intAlarmSupport.stop();
            if(outputSupport!=null) outputSupport.stop();
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(inputSupport!=null) inputSupport.uninitialize();
            if(intAlarmSupport!=null) intAlarmSupport.uninitialize();
            if(outputSupport!=null) outputSupport.uninitialize();
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            inputSupport = null;
            intAlarmSupport = null;
            outputSupport = null;
            valueDBField = null;
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
            } else if(intAlarmSupport!=null) {
                processState = ProcessState.intAlarmSupport;
                intAlarmSupport.process(this);
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
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(requestResult.compareTo(finalResult)>0) {
                finalResult = requestResult;
            }
            switch(processState) {
            case inputSupport:
                if(intAlarmSupport!=null) {
                    processState = ProcessState.intAlarmSupport;
                    intAlarmSupport.process(this);
                    return;
                }
            case intAlarmSupport:
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
    }
}
