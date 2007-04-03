/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import java.util.*;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
import org.epics.ioc.support.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class RecordProcessFactory {
    
    /**
     * Create RecordProcess for a record instance.
     * @param dbRecord The record instance.
     * @return The interface for the newly created RecordProcess.
     */
    static public RecordProcess createRecordProcess(DBRecord dbRecord) {
        return new ProcessInstance(dbRecord);
    }
    
    static private class ProcessInstance implements RecordProcess,SupportProcessRequestor
    {
        private boolean trace = false;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private String recordProcessSupportName;
        private boolean disabled = false;
        private Support recordSupport = null;
        private ScanSupport scanSupport = null;
        private AlarmSupport alarmSupport = null;
        
        private boolean active = false;
        private boolean setActiveBySetActive = false;
        private boolean leaveActive;
        private RecordProcessRequestor recordProcessRequestor = null;
        private boolean processIsRunning = false;
        private List<ProcessCallbackRequestor> processProcessCallbackRequestorList =
            new ArrayList<ProcessCallbackRequestor>();
        private boolean processContinueIsRunning = false;
        private List<ProcessCallbackRequestor> continueProcessCallbackRequestorList =
            new ArrayList<ProcessCallbackRequestor>();
        
        private boolean removeRecordProcessRequestorAfterActive = false;
        private boolean callStopAfterActive = false;
        private boolean callUninitializeAfterActive = false;
        private boolean processIsComplete = false;
        private RequestResult requestResult = null;
        
        
        
        private PVTimeStamp pvTimeStamp = null;
        private TimeStamp timeStamp = new TimeStamp();
        
        private ProcessInstance(DBRecord record) {
            dbRecord = record;
            pvRecord = dbRecord.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isDisabled()
         */
        public boolean isDisabled() {
            dbRecord.lock();
            try {
                return disabled;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setDisabled(boolean)
         */
        public boolean setDisabled(boolean value) {
            dbRecord.lock();
            try {
                boolean oldValue = disabled;
                disabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isActive()
         */
        public boolean isActive() {
            dbRecord.lock();
            try {
                return active;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getRecord()
         */
        public DBRecord getRecord() {
            return dbRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#setTrace(boolean)
         */
        public boolean setTrace(boolean value) {
            dbRecord.lock();
            try {
                boolean oldValue = trace;
                trace = value;
                if(value!=oldValue) return true;
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getSupportState()
         */
        public SupportState getSupportState() {
            dbRecord.lock();
            try {
                return recordSupport.getSupportState();
            } finally {
                dbRecord.unlock();
            }
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#initialize()
         */
        public void initialize() {
            dbRecord.lock();
            try {
                if(trace) traceMessage(" initialize");
                DBStructure dbStructure = dbRecord.getDBStructure();
                recordProcessSupportName = "recordProcess(" + pvRecord.getRecordName() + ")";
                recordSupport = dbStructure.getSupport();
                if(recordSupport==null) {
                    throw new IllegalStateException(
                        pvRecord.getRecordName() + " has no support");
                }
                PVField[] pvFields = pvRecord.getFieldPVFields();
                DBField[] dbFields = dbStructure.getFieldDBFields();
                PVField pvField;
                Structure structure = (Structure)pvRecord.getField();
                int index = structure.getFieldIndex("alarm");
                if(index>=0) {
                    pvField = pvFields[index];
                    if(pvField.getField().getType()==Type.pvStructure)
                        alarmSupport = (AlarmSupport)dbFields[index].getSupport();
                }
                index = structure.getFieldIndex("timeStamp");
                if(index>=0) {
                    pvTimeStamp = PVTimeStamp.create(dbFields[index]);
                }
                index = structure.getFieldIndex("scan");
                if(index>=0) {
                    pvField = pvFields[index];
                    if(pvField.getField().getType()==Type.pvStructure) {
                        scanSupport = (ScanSupport)dbFields[index].getSupport();
                    }
                }
                if(scanSupport!=null) scanSupport.initialize();
                recordSupport.initialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#start()
         */
        public void start() {
            dbRecord.lock();
            try {
                if(trace) traceMessage(" start");
                if(scanSupport!=null) scanSupport.start();
                recordSupport.start();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#stop()
         */
        public void stop() {
            dbRecord.lock();
            try {
                if(active) {
                    callStopAfterActive = true;
                    if(trace) traceMessage("stop delayed because active");
                    return;
                }
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                recordSupport.stop();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#uninitialize()
         */
        public void uninitialize() {
            dbRecord.lock();
            try {
                if(active) {
                    callUninitializeAfterActive = true;
                    if(trace) traceMessage("uninitialize delayed because active");
                    return;
                }
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                recordSupport.uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setRecordProcessRequestor(org.epics.ioc.process.RecordProcessRequestor)
         */
        public boolean setRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(this.recordProcessRequestor==null) {
                    this.recordProcessRequestor = recordProcessRequestor;
                    return true;
                }
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getRecordProcessRequestorName()
         */
        public String getRecordProcessRequestorName() {
            dbRecord.lock();
            try {
                if(recordProcessRequestor==null) return null;
                return recordProcessRequestor.getRequestorName();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#releaseRecordProcessRequestor(org.epics.ioc.process.RecordProcessRequestor)
         */
        public boolean releaseRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(recordProcessRequestor==this.recordProcessRequestor) {
                    if(active) {
                        removeRecordProcessRequestorAfterActive = true;
                    } else {
                        this.recordProcessRequestor = null;
                    }
                    return true;
                }
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#releaseRecordProcessRequestor()
         */
        public void releaseRecordProcessRequestor() {
            dbRecord.lock();
            try {
                pvRecord.message("recordProcessRequestor is being released", MessageType.error);
                if(active) {
                    removeRecordProcessRequestorAfterActive = true;
                } else {
                    recordProcessRequestor = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setActive(org.epics.ioc.process.RecordProcessRequestor, org.epics.ioc.util.TimeStamp)
         */
        public void setActive(RecordProcessRequestor recordProcessRequestor) {
            boolean isStarted;
            dbRecord.lock();
            try {
                isStarted = startCommon(recordProcessRequestor);
                if(isStarted) {
                    if(trace) traceMessage(
                        "setActive " + recordProcessRequestor.getRequestorName()); 
                    setActiveBySetActive = true;
                } else {
                    recordProcessRequestor.recordProcessResult(RequestResult.failure);
                }
            } finally {
                dbRecord.unlock();
            }
            if(!isStarted) {
                recordProcessRequestor.recordProcessComplete();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#canProcessSelf()
         */
        public boolean canProcessSelf() {
            dbRecord.lock();
            try {
                return scanSupport.canProcessSelf();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelf(org.epics.ioc.process.RecordProcessRequestor)
         */
        public boolean processSelfRequest(RecordProcessRequestor recordProcessRequestor) {
            boolean result = false;
            dbRecord.lock();
            try {
                if(!scanSupport.canProcessSelf()) return false;
                result = scanSupport.processSelfRequest(recordProcessRequestor);
                if(!result) return false;
                
            } finally {
                dbRecord.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfSetActive()
         */
        public void processSelfSetActive(RecordProcessRequestor recordProcessRequestor) {
            scanSupport.processSelfSetActive(recordProcessRequestor);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfProcess()
         */
        public void processSelfProcess(RecordProcessRequestor recordProcessRequestor, boolean leaveActive) {
            scanSupport.processSelfProcess(recordProcessRequestor, leaveActive);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfSetInactive()
         */
        public void processSelfSetInactive(RecordProcessRequestor recordProcessRequestor) {
            scanSupport.processSelfSetInactive(recordProcessRequestor);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#process(org.epics.ioc.process.RecordProcessRequestor, boolean, org.epics.ioc.util.TimeStamp)
         */
        public void process(RecordProcessRequestor recordProcessRequestor, boolean leaveActive, TimeStamp timeStamp)
        {
            boolean isStarted = true;
            boolean callComplete = false;
            dbRecord.lock();
            try {
                if(!setActiveBySetActive) {
                    isStarted = startCommon(recordProcessRequestor);
                }
                if(isStarted) {
                    if(timeStamp==null) {
                        if(trace) traceMessage(
                            "process with system timeStamp "
                            + recordProcessRequestor.getRequestorName()); 
                        TimeUtility.set(this.timeStamp,System.currentTimeMillis());
                    } else {
                        if(trace) traceMessage(
                            "process with callers timeStamp "
                            + recordProcessRequestor.getRequestorName()); 
                        this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
                        this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
                    }
                    this.leaveActive = leaveActive;
                    processIsRunning = true;
                    recordSupport.process(this);
                    processIsRunning = false;
                    if(processIsComplete) {
                        completeProcessing();
                        callComplete = true;
                    }
                } else {
                    recordProcessRequestor.recordProcessResult(RequestResult.failure);
                }
            } finally {
                dbRecord.unlock();
            }
            if(!isStarted) {
                recordProcessRequestor.recordProcessComplete();
                return;
            }
            if(callComplete) {
                recordProcessRequestor.recordProcessComplete();
                return;
            }
            while(true) {
                ProcessCallbackRequestor processCallbackRequestor;
                /*
                 * No need to lock because the list can only be modified by
                 * code that was called directly or indirectly by process
                 * AND process will only be called if the record is not active.
                 */
                if(processProcessCallbackRequestorList.size()<=0) break;
                processCallbackRequestor = processProcessCallbackRequestorList.remove(0);
                processCallbackRequestor.processCallback();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setInactive(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void setInactive(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(trace) traceMessage("setInactive" + recordProcessRequestor.getRequestorName());
                if(!active) {
                    throw new IllegalStateException("record is not active");
                }
                if(this.recordProcessRequestor==null) {
                    throw new IllegalStateException("no registered requestor");
                }
                if(this.recordProcessRequestor != recordProcessRequestor) {
                    throw new IllegalStateException("not registered requestor");
                }
                active = false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#processContinue()
         */
        public void processContinue(ProcessContinueRequestor processContinueRequestor) {
            boolean callbackListEmpty = true;
            dbRecord.lock();
            try {
                if(!active) {
                    throw new IllegalStateException("processContinue called but record is not active");
                }
                if(trace) {
                    traceMessage("processContinue ");
                }
                processContinueIsRunning = true;
                processContinueRequestor.processContinue();
                processContinueIsRunning = false;
                if(!continueProcessCallbackRequestorList.isEmpty()) callbackListEmpty = false;
                if(processIsComplete) {
                    completeProcessing();
                }
            } finally {
                dbRecord.unlock();
            }
            if(processIsComplete) {
                if(recordProcessRequestor!=null) {
                    recordProcessRequestor.recordProcessComplete();
                }
                return;
            }
            if(!callbackListEmpty) while(true) {
                ProcessCallbackRequestor processCallbackRequestor;
                /*
                 * Must lock because callback can again call RecordProcess.processContinue
                 */
                dbRecord.lock();
                try {
                    if(continueProcessCallbackRequestorList.size()<=0) break;
                    processCallbackRequestor = continueProcessCallbackRequestorList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processCallbackRequestor.processCallback();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#requestProcessCallback(org.epics.ioc.process.ProcessCallbackRequestor)
         */
        public void requestProcessCallback(ProcessCallbackRequestor processCallbackRequestor) {
            if(!active) {
                throw new IllegalStateException("requestProcessCallback called but record is not active");
            }
            if(trace) {
                traceMessage("requestProcessCallback " + processCallbackRequestor.getRequestorName());
            }
            if(processIsRunning) {
                processProcessCallbackRequestorList.add(processCallbackRequestor);
                return;
            }
            if(processContinueIsRunning) {
                continueProcessCallbackRequestorList.add(processCallbackRequestor);
                return;
            }
            throw new IllegalStateException("Support called requestProcessCallback illegally");
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#setTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void setTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            if(trace) traceMessage("setTimeStamp");
            this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
            this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            timeStamp.secondsPastEpoch = this.timeStamp.secondsPastEpoch;
            timeStamp.nanoSeconds = this.timeStamp.nanoSeconds;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#getSupportProcessRequestorName()
         */
        public String getRequestorName() {
            return recordProcessSupportName;
        }
      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(!processIsRunning && !processContinueIsRunning) {
                throw new IllegalStateException("must be called from process or pocessContinue");
            }
            processIsComplete = true;
            this.requestResult = requestResult;
        }
        
        private void traceMessage(String message) {
            pvRecord.message(
                message + " thread " + Thread.currentThread().getName(),
                MessageType.info);
        }
        
        private boolean startCommon(RecordProcessRequestor recordProcessRequestor) {
            if(this.recordProcessRequestor==null) {
                throw new IllegalStateException("no registered requestor");
            }
            if(this.recordProcessRequestor != recordProcessRequestor) {
                throw new IllegalStateException("not registered requestor");
            }
            if(active) {
                recordProcessRequestor.message("record already active",MessageType.error);
                return false;
            }
            if(isDisabled()) {
                recordProcessRequestor.message("record is disabled ",MessageType.error);
                return false;
            }
            SupportState supportState = recordSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                recordProcessRequestor.message("record support is not ready",MessageType.error);
                return false;
            }
            if(alarmSupport!=null) alarmSupport.beginProcess();
            active = true;
            processIsComplete = false;
            dbRecord.beginProcess();
            return true;
        }
        // called by process, preProcess, and processContinue with record locked.
        private void completeProcessing() {         
            if(removeRecordProcessRequestorAfterActive) {
                if(trace) traceMessage("remove recordProcessRequestor");
                recordProcessRequestor = null;
            }
            if(callStopAfterActive) {
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                recordSupport.stop();
                callStopAfterActive = false;
            }
            if(callUninitializeAfterActive) {
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                recordSupport.uninitialize();
                callUninitializeAfterActive = false;
            }
            if(!processProcessCallbackRequestorList.isEmpty()
            || !continueProcessCallbackRequestorList.isEmpty()){
                pvRecord.message(
                    "completing processing but ProcessCallbackRequestors are still present",
                    MessageType.fatalError);
            }
            if(alarmSupport!=null) alarmSupport.endProcess();
            if(pvTimeStamp!=null) {
                pvTimeStamp.put(timeStamp);
            }
            dbRecord.endProcess();
            recordProcessRequestor.recordProcessResult(requestResult);
            if(!leaveActive) active = false;
            setActiveBySetActive = false;
            if(trace) traceMessage("process completion " + recordSupport.getRequestorName());
        }
        
        private void checkForIllegalRequest() {
            if(active && (processIsRunning||processContinueIsRunning)) return;
            if(!active) {
                pvRecord.message("illegal request because record is not active",
                     MessageType.info);
                throw new IllegalStateException("record is not active");
            } else {
                pvRecord.message("illegal request because neither process or processContinue is running",
                        MessageType.info);
                throw new IllegalStateException("neither process or processContinue is running");
            }
        }
       
    }
    
}
