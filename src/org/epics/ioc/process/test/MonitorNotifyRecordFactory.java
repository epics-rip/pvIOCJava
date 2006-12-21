/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process.test;

import org.epics.ioc.dbd.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Record that holds a double value, an input link, and an array of process or output links.
 * @author mrk
 *
 */
public class MonitorNotifyRecordFactory {
    public static Support create(PVStructure pvStructure) {
        return new MonitorNotifyRecordSupport(pvStructure);
    }
    
    private enum ProcessState {
        input,
        output
    }
    
    static private class MonitorNotifyRecordSupport extends AbstractSupport
    implements SupportProcessRequestor
    {
        private static String supportName = "monitorNotifyRecord";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBRecord dbRecord;
        private PVBoolean notify = null;
        private LinkSupport inputSupport = null;
        private LinkSupport outputSupport = null;
        private SupportProcessRequestor supportProcessRequestor = null;
        private ProcessState processState = ProcessState.input;
        
        private MonitorNotifyRecordSupport(PVStructure pvStructure) {
            super(supportName,(DBData)pvStructure);
            dbRecord = ((DBData)pvStructure).getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getRequestorName() {
            return dbRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            dbRecord.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            if(dbRecord.getRecordProcess().getRecordProcessRequestorName()!=null) {
                dbRecord.message(
                        "a recordProcessRequestor already exists",
                        MessageType.error);
                return;
            }
            IOCDB iocdb = dbRecord.getIOCDB();
            DBAccess dbAccess = iocdb.createAccess(dbRecord.getRecordName());
            PVData pvData;
            AccessSetResult result;
            result = dbAccess.setField("notify");
            if(result!=AccessSetResult.thisRecord) {
                dbRecord.message(
                        "field notify does not exist",
                        MessageType.error);
                return;
            }
            pvData = dbAccess.getField();
            if(pvData.getField().getType()!=Type.pvBoolean) {
                dbRecord.message(
                        "field notify is not boolean",
                        MessageType.error);
                return;
            }
            PVData oldField = dbAccess.getField();
            PVData parent = oldField.getParent();
            Field field = oldField.getField();
            notify = new BooleanData((DBData)parent,field);
            dbAccess.replaceField(oldField,notify);
            result = dbAccess.setField("inputArray");
            if(result==AccessSetResult.thisRecord) {
                inputSupport = (LinkSupport)dbAccess.getField().getSupport();
                if(inputSupport!=null) {
                    inputSupport.setField(notify);
                    inputSupport.initialize();
                    supportState = inputSupport.getSupportState();
                }
                if(supportState!=SupportState.readyForStart) {
                    return;
                }
            } else {
                dbRecord.message(
                        "field input does not exist",
                        MessageType.error);
                return;
            }
            
            result = dbAccess.setField("outputArray");
            if(result==AccessSetResult.thisRecord) {
                outputSupport = (LinkSupport)dbAccess.getField().getSupport();
                if(outputSupport!=null) {
                    outputSupport.setField(notify);
                    outputSupport.initialize();
                    supportState = outputSupport.getSupportState();
                }
                if(supportState!=SupportState.readyForStart) {
                    if(inputSupport!=null) inputSupport.uninitialize();
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
            inputSupport.start();
            supportState = inputSupport.getSupportState();
            if(supportState==SupportState.ready) {
                outputSupport.start();
                supportState = outputSupport.getSupportState();
            }
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            inputSupport.stop();
            outputSupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            inputSupport.uninitialize();
            outputSupport.uninitialize();
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
            processState = ProcessState.input;
            inputSupport.process(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(processState==ProcessState.input && requestResult==RequestResult.success) {
                notify.put(false);
                processState = ProcessState.output;
                outputSupport.process(this);
                return;
            }
            supportProcessRequestor.supportProcessDone(requestResult);
            return;
        }
    }
    
    private static class BooleanData extends AbstractDBData
    implements PVBoolean, Runnable, RecordProcessRequestor
    {
        private static IOCExecutor iocExecutor
            = IOCExecutorFactory.create("monitorNotifyRecord");
        private static Convert convert = ConvertFactory.getConvert();
        private boolean value;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private boolean processActive = false;
        private boolean processAgain = false;
        
        private BooleanData(DBData parent,Field field) {
            super(parent,field);
            dbRecord = parent.getRecord();
            recordProcess = dbRecord.getRecordProcess();
            boolean result = recordProcess.setRecordProcessRequestor(this);
            if(!result) {
                throw new IllegalStateException("setRecordProcessRequestor failed");
            }
            value = false;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBoolean#get()
         */
        public boolean get() {
            return value;
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            this.value = value;
            postPut();
            if(value) {
                if(processActive) {
                    processAgain = true;
                } else {
                    processActive = true;
                    iocExecutor.execute(this, ScanPriority.low);
                }
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            recordProcess.process(this, false, null);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            if(processAgain) {
                processAgain = false;
                iocExecutor.execute(this, ScanPriority.low);
            } else {
                processActive = false;
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return dbRecord.getRecordName();
        }
    }
}
