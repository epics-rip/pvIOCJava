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
    
    static private class ProcessInstance implements RecordProcess,SupportProcessRequester
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
        private RecordProcessRequester recordProcessRequester = null;
        private boolean processIsRunning = false;
        private List<ProcessCallbackRequester> processProcessCallbackRequesterList =
            new ArrayList<ProcessCallbackRequester>();
        private boolean processContinueIsRunning = false;
        private List<ProcessCallbackRequester> continueProcessCallbackRequesterList =
            new ArrayList<ProcessCallbackRequester>();
        
        private boolean removeRecordProcessRequesterAfterActive = false;
        private boolean callStopAfterActive = false;
        private boolean callUninitializeAfterActive = false;
        private boolean processIsComplete = false;
        private boolean processCompleteDone = false;
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
         * @see org.epics.ioc.process.RecordProcess#setRecordProcessRequester(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean setRecordProcessRequester(RecordProcessRequester recordProcessRequester) {
            dbRecord.lock();
            try {
                if(this.recordProcessRequester==null) {
                    this.recordProcessRequester = recordProcessRequester;
                    return true;
                }
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getRecordProcessRequesterName()
         */
        public String getRecordProcessRequesterName() {
            dbRecord.lock();
            try {
                if(recordProcessRequester==null) return null;
                return recordProcessRequester.getRequesterName();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#releaseRecordProcessRequester(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean releaseRecordProcessRequester(RecordProcessRequester recordProcessRequester) {
            dbRecord.lock();
            try {
                if(recordProcessRequester==this.recordProcessRequester) {
                    if(active) {
                        removeRecordProcessRequesterAfterActive = true;
                    } else {
                        this.recordProcessRequester = null;
                    }
                    return true;
                }
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#releaseRecordProcessRequester()
         */
        public void releaseRecordProcessRequester() {
            dbRecord.lock();
            try {
                pvRecord.message("recordProcessRequester is being released", MessageType.error);
                if(active) {
                    removeRecordProcessRequesterAfterActive = true;
                } else {
                    recordProcessRequester = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setActive(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean setActive(RecordProcessRequester recordProcessRequester) {
            boolean isStarted;
            dbRecord.lock();
            try {
                isStarted = startCommon(recordProcessRequester);
                if(isStarted) {
                    if(trace) traceMessage(
                        "setActive " + recordProcessRequester.getRequesterName()); 
                    setActiveBySetActive = true;
                } else {
                    if(trace) traceMessage(
                            "setActive " + recordProcessRequester.getRequesterName() + " failed"); 
                }
            } finally {
                dbRecord.unlock();
            }
            return isStarted;
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
         * @see org.epics.ioc.process.RecordProcess#processSelf(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean processSelfRequest(RecordProcessRequester recordProcessRequester) {
            boolean result = false;
            dbRecord.lock();
            try {
                if(!scanSupport.canProcessSelf()) return false;
                result = scanSupport.processSelfRequest(recordProcessRequester);
                if(!result) return false;
                
            } finally {
                dbRecord.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfSetActive()
         */
        public void processSelfSetActive(RecordProcessRequester recordProcessRequester) {
            scanSupport.processSelfSetActive(recordProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfProcess()
         */
        public void processSelfProcess(RecordProcessRequester recordProcessRequester, boolean leaveActive) {
            scanSupport.processSelfProcess(recordProcessRequester, leaveActive);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfSetInactive()
         */
        public void processSelfSetInactive(RecordProcessRequester recordProcessRequester) {
            scanSupport.processSelfSetInactive(recordProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#process(org.epics.ioc.process.RecordProcessRequester, boolean, org.epics.ioc.util.TimeStamp)
         */
        public boolean process(RecordProcessRequester recordProcessRequester, boolean leaveActive, TimeStamp timeStamp)
        {
            boolean isStarted = true;
            boolean callComplete = false;
            dbRecord.lock();
            try {
                if(!setActiveBySetActive) {
                    isStarted = startCommon(recordProcessRequester);
                }
                if(isStarted) {
                    if(timeStamp==null) {
                        if(trace) traceMessage(
                            "process with system timeStamp "
                            + recordProcessRequester.getRequesterName()); 
                        TimeUtility.set(this.timeStamp,System.currentTimeMillis());
                    } else {
                        if(trace) traceMessage(
                            "process with callers timeStamp "
                            + recordProcessRequester.getRequesterName()); 
                        this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
                        this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
                    }
                    this.leaveActive = leaveActive;
                    processIsRunning = true;
                    recordSupport.process(this);
                    processIsRunning = false;
                    if(processIsComplete && !processCompleteDone) {
                        processCompleteDone = true;
                        completeProcessing();
                        callComplete = true;
                    }
                } else {
                    if(trace) traceMessage(
                            "process "
                            + recordProcessRequester.getRequesterName()
                            + " request failed"); 
                }
            } finally {
                dbRecord.unlock();
            }
            if(!isStarted) return false;
            if(callComplete) {
                recordProcessRequester.recordProcessComplete();
                return true;
            }
            while(true) {
                ProcessCallbackRequester processCallbackRequester;
                /*
                 * No need to lock because the list can only be modified by
                 * code that was called directly or indirectly by process
                 * AND process will only be called if the record is not active.
                 */
                if(processProcessCallbackRequesterList.size()<=0) break;
                processCallbackRequester = processProcessCallbackRequesterList.remove(0);
                processCallbackRequester.processCallback();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setInactive(org.epics.ioc.process.RecordProcessRequester)
         */
        public void setInactive(RecordProcessRequester recordProcessRequester) {
            dbRecord.lock();
            try {
                if(trace) traceMessage("setInactive" + recordProcessRequester.getRequesterName());
                if(!active) {
                    throw new IllegalStateException("record is not active");
                }
                if(this.recordProcessRequester==null) {
                    throw new IllegalStateException("no registered requester");
                }
                if(this.recordProcessRequester != recordProcessRequester) {
                    throw new IllegalStateException("not registered requester");
                }
                active = false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#processContinue()
         */
        public void processContinue(ProcessContinueRequester processContinueRequester) {
            boolean callbackListEmpty = true;
            dbRecord.lock();
            try {
                if(!active) {
                    throw new IllegalStateException(
                        "processContinue called but record "
                         + pvRecord.getRecordName()
                         + " is not active");
                }
                if(trace) {
                    traceMessage("processContinue ");
                }
                processContinueIsRunning = true;
                processContinueRequester.processContinue();
                processContinueIsRunning = false;
                if(!continueProcessCallbackRequesterList.isEmpty()) callbackListEmpty = false;
                if(processIsComplete && !processCompleteDone) {
                    processCompleteDone = true;
                    completeProcessing();
                }
            } finally {
                dbRecord.unlock();
            }
            if(processIsComplete) {
                if(recordProcessRequester!=null) {
                    recordProcessRequester.recordProcessComplete();
                }
                return;
            }
            if(!callbackListEmpty) while(true) {
                ProcessCallbackRequester processCallbackRequester;
                /*
                 * Must lock because callback can again call RecordProcess.processContinue
                 */
                dbRecord.lock();
                try {
                    if(continueProcessCallbackRequesterList.size()<=0) break;
                    processCallbackRequester = continueProcessCallbackRequesterList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processCallbackRequester.processCallback();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#requestProcessCallback(org.epics.ioc.process.ProcessCallbackRequester)
         */
        public void requestProcessCallback(ProcessCallbackRequester processCallbackRequester) {
            if(!active) {
                throw new IllegalStateException("requestProcessCallback called but record is not active");
            }
            if(trace) {
                traceMessage("requestProcessCallback " + processCallbackRequester.getRequesterName());
            }
            if(processIsRunning) {
                processProcessCallbackRequesterList.add(processCallbackRequester);
                return;
            }
            if(processContinueIsRunning) {
                continueProcessCallbackRequesterList.add(processCallbackRequester);
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
         * @see org.epics.ioc.process.SupportProcessRequester#getSupportProcessRequesterName()
         */
        public String getRequesterName() {
            return recordProcessSupportName;
        }
      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
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
        
        private boolean startCommon(RecordProcessRequester recordProcessRequester) {
            if(this.recordProcessRequester==null) {
                throw new IllegalStateException("no registered requester");
            }
            if(this.recordProcessRequester != recordProcessRequester) {
                recordProcessRequester.message("not the registered requester",MessageType.error);
                return false;
            }
            if(active) {
                recordProcessRequester.message("record already active",MessageType.error);
                return false;
            }
            if(isDisabled()) {
                recordProcessRequester.message("record is disabled ",MessageType.error);
                return false;
            }
            SupportState supportState = recordSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                recordProcessRequester.message("record support is not ready",MessageType.error);
                return false;
            }
            if(alarmSupport!=null) alarmSupport.beginProcess();
            active = true;
            processIsComplete = false;
            processCompleteDone = false;
            dbRecord.beginProcess();
            return true;
        }
        // called by process, preProcess, and processContinue with record locked.
        private void completeProcessing() {         
            if(removeRecordProcessRequesterAfterActive) {
                if(trace) traceMessage("remove recordProcessRequester");
                recordProcessRequester = null;
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
            if(!processProcessCallbackRequesterList.isEmpty()
            || !continueProcessCallbackRequesterList.isEmpty()){
                pvRecord.message(
                    "completing processing but ProcessCallbackRequesters are still present",
                    MessageType.fatalError);
            }
            if(alarmSupport!=null) alarmSupport.endProcess();
            if(pvTimeStamp!=null) {
                pvTimeStamp.put(timeStamp);
            }
            dbRecord.endProcess();
            recordProcessRequester.recordProcessResult(requestResult);
            if(!leaveActive) active = false;
            setActiveBySetActive = false;
            if(trace) traceMessage("process completion " + recordSupport.getRequesterName());
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
