/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanField;
import org.epics.ioc.util.ScanFieldFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Structure;



/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class RecordProcessFactory {
    
    /**
     * Create RecordProcess for a record instance.
     * @param pvRecord The record instance.
     * @return The interface for the newly created RecordProcess.
     */
    static public RecordProcess createRecordProcess(RecordSupport recordSupport,PVRecord pvRecord) {
        return new ProcessInstance(recordSupport,pvRecord);
    }
    
    static private class ProcessInstance implements RecordProcess,SupportProcessRequester,RecordProcessRequester
    {
        private boolean trace = false;
        private PVRecord pvRecord;
        private RecordSupport recordSupport;
        private String recordProcessSupportName = null;
        private boolean enabled = true;
        private Support fieldSupport = null;
        private ScanField  scanField = null;
        private PVBoolean pvProcessSelf = null;
        private ProcessSelf processSelf = null;
        private PVBoolean pvProcessAfterStart = null;
        private Support scanSupport = null;
        
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
        
        
        private TimeStamp timeStamp = null;
        
        private ProcessInstance(RecordSupport recordSupport,PVRecord pvRecord) {
            this.recordSupport = recordSupport;
            this.pvRecord = pvRecord;
            recordSupport.setRecordProcess(this);
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
            pvRecord.lock();
            try {
                boolean oldValue = enabled;
                enabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isActive()
         */
        public boolean isActive() {
            return active;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#getRecord()
         */
        public PVRecord getRecord() {
            return pvRecord;
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
            pvRecord.lock();
            try {
                boolean oldValue = trace;
                trace = value;
                if(value!=oldValue) return true;
                return false;
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getSupportState()
         */
        public SupportState getSupportState() {
            pvRecord.lock();
            try {
                return fieldSupport.getSupportState();
            } finally {
                pvRecord.unlock();
            }
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#initialize()
         */
        public void initialize() {
            pvRecord.lock();
            try {
                if(trace) traceMessage(" initialize");
                PVStructure pvStructure = pvRecord.getPVStructure();
                recordProcessSupportName = "recordProcess(" + pvRecord.getRecordName() + ")";
                fieldSupport = recordSupport.getSupport(pvRecord);
                if(fieldSupport==null) {
                    throw new IllegalStateException(
                        pvRecord.getRecordName() + " has no support");
                }
                PVField[] pvFields = pvStructure.getPVFields();
                Structure structure = (Structure)pvRecord.getField();
                int index;
                index = structure.getFieldIndex("timeStamp");
                if(index>=0) {
                    timeStamp = TimeStampFactory.getTimeStamp((PVStructure)pvFields[index]);
                }
                index = structure.getFieldIndex("scan");
                if(index>=0) {
                    scanSupport = recordSupport.getSupport(pvFields[index]);
                    scanField = ScanFieldFactory.create(pvRecord);
                    if(scanField!=null) {
                        pvProcessSelf = scanField.getProcessSelfPV();
                        processSelf = new ProcessSelf(this,pvRecord,scanField);

                        pvProcessAfterStart = scanField.getProcessAfterStartPV();
                    }
                }
                fieldSupport.initialize(recordSupport);
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#start()
         */
        public void start() {
            pvRecord.lock();
            try {
                if(trace) traceMessage(" start");
                fieldSupport.start();
                if(scanSupport!=null) scanSupport.start();
                if(processSelf!=null) processSelf.start();
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#stop()
         */
        public void stop() {
            pvRecord.lock();
            try {
                if(active) {
                    callStopAfterActive = true;
                    if(trace) traceMessage("stop delayed because active");
                    return;
                }
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                fieldSupport.stop();
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#uninitialize()
         */
        public void uninitialize() {
            pvRecord.lock();
            try {
                if(active) {
                    callUninitializeAfterActive = true;
                    if(trace) traceMessage("uninitialize delayed because active");
                    return;
                }
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                fieldSupport.uninitialize();
            } finally {
                pvRecord.unlock();
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
            pvRecord.lock();
            try {
                if(this.recordProcessRequester==null) {
                    this.recordProcessRequester = recordProcessRequester;
                    return true;
                }
                return false;
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getRecordProcessRequesterName()
         */
        public String getRecordProcessRequesterName() {
            pvRecord.lock();
            try {
                if(recordProcessRequester==null) return null;
                return recordProcessRequester.getRequesterName();
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#releaseRecordProcessRequester(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean releaseRecordProcessRequester(RecordProcessRequester recordProcessRequester) {
            pvRecord.lock();
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
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#releaseRecordProcessRequester()
         */
        public void releaseRecordProcessRequester() {
            pvRecord.lock();
            try {
                pvRecord.message("recordProcessRequester is being released", MessageType.error);
                if(active) {
                    removeRecordProcessRequesterAfterActive = true;
                } else {
                    recordProcessRequester = null;
                }
            } finally {
                pvRecord.unlock();
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setActive(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean setActive(RecordProcessRequester recordProcessRequester) {
            boolean isStarted;
            pvRecord.lock();
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
                pvRecord.unlock();
            }
            return isStarted;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#canProcessSelf()
         */
        public boolean canProcessSelf() {
            pvRecord.lock();
            try {
                if(processSelf==null) return false;
                return pvProcessSelf.get();
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelf(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean processSelfRequest(RecordProcessRequester recordProcessRequester) {
            boolean result = false;
            pvRecord.lock();
            try {
                if(processSelf==null) return false;
                result = processSelf.processSelfRequest(recordProcessRequester);
                if(!result) return false;
                
            } finally {
                pvRecord.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfSetActive()
         */
        public boolean processSelfSetActive(RecordProcessRequester recordProcessRequester) {
            return processSelf.setActive(recordProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfProcess()
         */
        public boolean processSelfProcess(RecordProcessRequester recordProcessRequester, boolean leaveActive) {
            return processSelf.process(recordProcessRequester, leaveActive);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#processSelfSetInactive()
         */
        public void processSelfSetInactive(RecordProcessRequester recordProcessRequester) {
            processSelf.setInactive(recordProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#process(org.epics.ioc.process.RecordProcessRequester, boolean, org.epics.ioc.util.TimeStamp)
         */
        public boolean process(RecordProcessRequester recordProcessRequester, boolean leaveActive, TimeStamp timeStamp)
        {
            boolean isStarted = true;
            pvRecord.lock();
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
                if(this.timeStamp!=null) {
                    if(timeStamp==null) {
                        this.timeStamp.put(System.currentTimeMillis());
                        if(trace) traceMessage(
                                "process with system timeStamp "
                                + recordProcessRequester.getRequesterName()); 
                    } else {
                        this.timeStamp.put(timeStamp.getSecondsPastEpoch(),timeStamp.getNanoSeconds());
                        if(trace) traceMessage(
                                "process with callers timeStamp "
                                + recordProcessRequester.getRequesterName()); 
                    }
                } else {
                    if(trace) {
                        if(timeStamp==null) {
                            traceMessage("process no TimeStamp no Caller TimeStamp "
                                 + recordProcessRequester.getRequesterName()); 
                        } else {
                            traceMessage("process no TimeStamp Caller supplied TimeStamp "
                                    + recordProcessRequester.getRequesterName()); 
                        }
                    }
                }
                this.leaveActive = leaveActive;
                processIsRunning = true;
                // NOTE: processContinue may be called before the following returns
                fieldSupport.process(this);
                processIsRunning = false;
                if(processIsComplete && !processCompleteDone) {
                    completeProcessing();
                }
            } finally {
                pvRecord.unlock();
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
            pvRecord.lock();
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
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#processContinue()
         */
        public void processContinue(ProcessContinueRequester processContinueRequester) {
            ProcessCallbackRequester processCallbackRequester = null;
            pvRecord.lock();
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
                pvRecord.unlock();
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
                pvRecord.lock();
                try {
                    if(continueProcessCallbackRequesterList.isEmpty()) return;
                    processCallbackRequester = continueProcessCallbackRequesterList.remove(0);
                } finally {
                    pvRecord.unlock();
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
            if(this.timeStamp!=null) this.timeStamp.put(timeStamp.getSecondsPastEpoch(),timeStamp.getNanoSeconds());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            if(this.timeStamp==null) return;
            timeStamp.put(this.timeStamp.getSecondsPastEpoch(), this.timeStamp.getNanoSeconds());
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
            pvRecord.lock();
            try {
                if(trace) traceMessage("allSupportStarted");
                fieldSupport.allSupportStarted();
                if(scanSupport!=null) scanSupport.allSupportStarted();   
            } finally {
                pvRecord.unlock();
            }
            if(pvProcessAfterStart!=null) {
                boolean process = pvProcessAfterStart.get();
                if(process) {
                    if(recordProcessRequester==null) {
                        boolean ok = setRecordProcessRequester(this);
                        if(ok) {
                            process(this,false,null);
                        } else {
                            pvRecord.message(" processAfterStart failed", MessageType.warning);
                        }
                    } else if(!processSelfRequest(this)) {
                        pvRecord.message(" processAfterStart failed", MessageType.warning);
                    }
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            releaseRecordProcessRequester(this);  
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
        }
        
        private void traceMessage(String message) {
            String time = "";
            if(timeStamp!=null) {
                long milliPastEpoch = timeStamp.getMilliSeconds();
                Date date = new Date(milliPastEpoch);
                time = String.format("%tF %tT.%tL ", date,date,date);
            }
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
                recordProcessRequester.message("record already active",MessageType.warning);
                return false;
            }
            if(!isEnabled()) {
                recordProcessRequester.message("record is disabled ",MessageType.warning);
                return false;
            }
            SupportState supportState = fieldSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                recordProcessRequester.message("record support is not ready",MessageType.warning);
                return false;
            }
            active = true;
            processIsComplete = false;
            processCompleteDone = false;
            pvRecord.beginGroupPut();
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
                fieldSupport.stop();
                callStopAfterActive = false;
            }
            if(callUninitializeAfterActive) {
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                fieldSupport.uninitialize();
                callUninitializeAfterActive = false;
            }
            if(!processProcessCallbackRequesterList.isEmpty()
            || !continueProcessCallbackRequesterList.isEmpty()){
                pvRecord.message(
                    "completing processing but ProcessCallbackRequesters are still present",
                    MessageType.fatalError);
            }
            pvRecord.endGroupPut();
            recordProcessRequester.recordProcessResult(requestResult);
            if(!leaveActive) active = false;
            activeBySetActive = false;
            if(trace) traceMessage("process completion " + fieldSupport.getRequesterName());
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
        
        private static class ProcessSelf implements RecordProcessRequester,PVListener
        {
            private PVRecord pvRecord = null;
            private RecordProcess recordProcess = null;
            private PVBoolean pvProcessSelf = null;
            private boolean isProcessor = false;
            private boolean isActive = false;
            private boolean leaveActive = false;
            private RecordProcessRequester recordProcessRequester = null;
            
            private ProcessSelf(RecordProcess recordProcess,PVRecord pvRecord,ScanField scanField) {
                this.recordProcess = recordProcess;
                this.pvRecord = pvRecord;
                pvProcessSelf = scanField.getProcessSelfPV();
            }
            
            private void start() {
                pvRecord.addListener(this);
                pvProcessSelf.addListener(this);
                if(pvProcessSelf.get()==false) return;
                isProcessor = recordProcess.setRecordProcessRequester(this);
            }

            private boolean processSelfRequest(RecordProcessRequester recordProcessRequester) {
                if(!isProcessor) return false;
                if(isActive) return false;
                isActive = true;
                this.recordProcessRequester = recordProcessRequester;
                return true;
            }
            
            private boolean setActive(RecordProcessRequester recordProcessRequester) {
                if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                    throw new IllegalStateException("not the recordProcessRequester");
                }
                boolean result = recordProcess.setActive(this);
                if(!result) {
                    isActive = false;
                }
                return result;
            }
            
            private boolean process(RecordProcessRequester recordProcessRequester,boolean leaveActive) {
                if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                    throw new IllegalStateException("not the recordProcessRequester");
                }
                this.leaveActive = leaveActive;
                boolean result = recordProcess.process(this, leaveActive, null);
                if(!result) {
                    isActive = false;
                }
                return result;
            }
            
            private void setInactive(RecordProcessRequester recordProcessRequester) {
                if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                    throw new IllegalStateException("not the recordProcessRequester");
                }
                leaveActive = false;
                recordProcess.setInactive(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return recordProcess.getRecord().getPVRecord().getRecordName();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                recordProcess.getRecord().getPVRecord().message(message, messageType);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                isActive = false;
                recordProcessRequester.recordProcessComplete();
                if(!leaveActive) this.recordProcessRequester = null;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                recordProcessRequester.recordProcessResult(requestResult);
            }

            /* (non-Javadoc)
             * @see org.epics.pvData.pv.PVListener#beginGroupPut(org.epics.pvData.pv.PVRecord)
             */
            public void beginGroupPut(PVRecord pvRecord) {}

            /* (non-Javadoc)
             * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVField)
             */
            public void dataPut(PVField pvField) {
                boolean value = pvProcessSelf.get();
                if(value) {
                    if(!isProcessor) {
                        isProcessor = recordProcess.setRecordProcessRequester(this);
                    }
                } else {
                    if(isProcessor) {
                        isProcessor = false;
                        recordProcess.releaseRecordProcessRequester(this);
                    }
                }
            }

            /* (non-Javadoc)
             * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVField)
             */
            public void dataPut(PVStructure requested, PVField pvField) {}

            /* (non-Javadoc)
             * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
             */
            public void endGroupPut(PVRecord pvRecord) {}

            /* (non-Javadoc)
             * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
             */
            public void unlisten(PVRecord pvRecord) {
                if(isProcessor) {
                    isProcessor = false;
                    recordProcess.releaseRecordProcessRequester(this);
                }
            }
        }
    }
}
