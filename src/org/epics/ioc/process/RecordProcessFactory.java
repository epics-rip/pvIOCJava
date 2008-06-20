/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.ScanSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.PVTimeStamp;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.TimeStamp;
import org.epics.ioc.util.TimeUtility;

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
    
    static private class ProcessInstance implements
        RecordProcess,SupportProcessRequester,RecordProcessRequester
    {
        private boolean trace = false;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private String recordProcessSupportName = null;
        private boolean enabled = true;
        private Support recordSupport = null;
        private ScanSupport scanSupport = null;
        private PVBoolean pvProcessAfterStart = null;
        
        private boolean active = false;
        private boolean activeBySetActive = false;
        private boolean leaveActive = false;
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
        private boolean callRecordProcessComplete = false;
        private RequestResult requestResult = null;
        
        
        
        private PVTimeStamp pvTimeStamp = null;
        private TimeStamp timeStamp = new TimeStamp();
        
        private ProcessInstance(DBRecord record) {
            dbRecord = record;
            pvRecord = dbRecord.getPVRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isEnabled()
         */
        public boolean isEnabled() {
            return enabled;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setEnabled(boolean)
         */
        public boolean setEnabled(boolean value) {
            dbRecord.lock();
            try {
                boolean oldValue = enabled;
                enabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isActive()
         */
        public boolean isActive() {
            return active;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getRecord()
         */
        public DBRecord getRecord() {
            return dbRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isTrace()
         */
        public boolean isTrace() {
            return trace;
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
                PVField[] pvFields = pvRecord.getPVFields();
                DBField[] dbFields = dbStructure.getDBFields();
                PVField pvField;
                Structure structure = (Structure)pvRecord.getField();
                int index;
                index = structure.getFieldIndex("timeStamp");
                if(index>=0) {
                    pvTimeStamp = PVTimeStamp.create(dbFields[index]);
                }
                index = structure.getFieldIndex("scan");
                if(index>=0) {
                    pvField = pvFields[index];
                    if(pvField.getField().getType()==Type.pvStructure) {
                        scanSupport = (ScanSupport)dbFields[index].getSupport();
                        PVField pvf = pvField.findProperty("processAfterStart");
                        if(pvf!=null && pvf.getField().getType()==Type.pvBoolean) {
                            pvProcessAfterStart = (PVBoolean)pvf;
                        }
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
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            pvRecord.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setRecordProcessRequester(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean setRecordProcessRequester(RecordProcessRequester recordProcessRequester) {
            if(recordProcessRequester==null) {
                throw new IllegalArgumentException("must implement recordProcessRequester");
            }
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
                    activeBySetActive = true;
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
            dbRecord.lock();
            try {
                if(!activeBySetActive) {
                    isStarted = startCommon(recordProcessRequester);
                }
                if(!isStarted) {
                    if(trace) traceMessage(
                            "process "
                            + recordProcessRequester.getRequesterName()
                            + " request failed"); 
                    return false;
                }
                if(timeStamp==null) {
                    TimeUtility.set(this.timeStamp,System.currentTimeMillis());
                    if(trace) traceMessage(
                            "process with system timeStamp "
                            + recordProcessRequester.getRequesterName()); 
                } else {
                    this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
                    this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
                    if(trace) traceMessage(
                            "process with callers timeStamp "
                            + recordProcessRequester.getRequesterName()); 
                }
                this.leaveActive = leaveActive;
                processIsRunning = true;
                // NOTE: processContinue may be called before the following returns
                recordSupport.process(this);
                processIsRunning = false;
                if(processIsComplete && !processCompleteDone) {
                    completeProcessing();
                }
            } finally {
                dbRecord.unlock();
            }
            if(callRecordProcessComplete) {
                callRecordProcessComplete = false;
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
                if(!processIsComplete) {
                    throw new IllegalStateException("processing is not finished");
                }
                if(!processCompleteDone) {
                    throw new IllegalStateException("process complete is not done");
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
            ProcessCallbackRequester processCallbackRequester = null;
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
                if(!continueProcessCallbackRequesterList.isEmpty()) {
                    processCallbackRequester = continueProcessCallbackRequesterList.remove(0);
                }
                if(processIsComplete && !processCompleteDone) {
                    completeProcessing();
                }
            } finally {
                dbRecord.unlock();
            }
            if(callRecordProcessComplete) {
                callRecordProcessComplete = false;
                recordProcessRequester.recordProcessComplete();
                return;
            }
            while(processCallbackRequester!=null) {
                processCallbackRequester.processCallback();
                /*
                 * Must lock because processContinue can again call RecordProcess.requestProcessCallback
                 */
                dbRecord.lock();
                try {
                    if(continueProcessCallbackRequesterList.isEmpty()) return;
                    processCallbackRequester = continueProcessCallbackRequesterList.remove(0);
                } finally {
                    dbRecord.unlock();
                }

            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#requestProcessCallback(org.epics.ioc.process.ProcessCallbackRequester)
         */
        public void requestProcessCallback(ProcessCallbackRequester processCallbackRequester) {
            if(!active) {
                throw new IllegalStateException("requestProcessCallback called but record is not active");
            }
            if(processIsComplete) {
                throw new IllegalStateException("requestProcessCallback called but processIsComplete");
            }
            if(trace) {
                traceMessage("requestProcessCallback " + processCallbackRequester.getRequesterName());
            }
            if(processIsRunning) {
                if(processProcessCallbackRequesterList.contains(processCallbackRequester)) {
                    throw new IllegalStateException("requestProcessCallback called but already on list");
                }
                processProcessCallbackRequesterList.add(processCallbackRequester);
                return;
            }
            if(processContinueIsRunning) {
                if(continueProcessCallbackRequesterList.contains(processCallbackRequester)) {
                    throw new IllegalStateException("requestProcessCallback called but already on list");
                }
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
                throw new IllegalStateException("must be called from process or processContinue");
            }
            processIsComplete = true;
            this.requestResult = requestResult;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#allSupportStarted()
         */
        public void allSupportStarted() {
            dbRecord.lock();
            try {
                if(trace) traceMessage("allSupportStarted");
                recordSupport.allSupportStarted();
                if(scanSupport!=null) scanSupport.allSupportStarted();
                if(pvProcessAfterStart!=null) {
                    boolean process = pvProcessAfterStart.get();
                    if(process) {
                        if(recordProcessRequester==null) {
                            recordProcessRequester = this;
                            process(this,false,null);
                        } else if(!processSelfRequest(this)) {
                            pvRecord.message(" processAfterStart failed", MessageType.warning);
                        }
                    }
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            dbRecord.lock();
            try {
                recordProcessRequester = null;  
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
        }
        
        private void traceMessage(String message) {
            long secondPastEpochs = timeStamp.secondsPastEpoch;
            int nano = timeStamp.nanoSeconds;
            long milliPastEpoch = nano/1000000 + secondPastEpochs*1000;
            Date date = new Date(milliPastEpoch);
            String time = String.format("%tF %tT.%tL ", date,date,date);
            pvRecord.message(
                time + " " + message + " thread " + Thread.currentThread().getName(),
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
            if(!isEnabled()) {
                recordProcessRequester.message("record is disabled ",MessageType.error);
                return false;
            }
            SupportState supportState = recordSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                recordProcessRequester.message("record support is not ready",MessageType.error);
                return false;
            }
            active = true;
            processIsComplete = false;
            processCompleteDone = false;
            dbRecord.beginProcess();
            return true;
        }
        // called by process, preProcess, and processContinue with record locked.
        private void completeProcessing() {
            processCompleteDone = true;
            callRecordProcessComplete = true;
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
            if(pvTimeStamp!=null) {
                pvTimeStamp.put(timeStamp);
            }
            dbRecord.endProcess();
            recordProcessRequester.recordProcessResult(requestResult);
            if(!leaveActive) active = false;
            activeBySetActive = false;
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
