/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class RecordProcessFactory {
    
    /**
     * Create RecordProcess for a record instance.
     * @param dbRecord The record instance.
     * @return The interrace for the newly created RecordProcess.
     */
    static public RecordProcess createRecordProcess(DBRecord dbRecord) {
        return new ProcessInstance(dbRecord);
    }
    
    static private class ProcessInstance
    implements RecordProcess,RecordProcessSupport
    {
        private boolean trace = false;
        private DBRecord dbRecord;
        private String recordProcessSupportName;
        private boolean disabled = false;
        private Support recordSupport = null;
        private Support scanSupport = null;
        
        private boolean active = false;
        private RecordProcessRequestor recordProcessRequestor = null;
        private boolean processIsRunning = false;
        private List<ProcessCallbackListener> processProcessCallbackListenerList =
            new ArrayList<ProcessCallbackListener>();
        private boolean processContinueIsRunning = false;
        private List<ProcessCallbackListener> continueProcessCallbackListenerList =
            new ArrayList<ProcessCallbackListener>();
        
        private boolean removeRecordProcessRequestorAfterActive = false;
        private boolean callStopAfterActive = false;
        private boolean callUninitializeAfterActive = false;
        private boolean processIsComplete = false;
        private RequestResult requestResult = null;
        
        private PVString pvStatus = null;
        private String startStatus = null;
        private String newStatus = null;
        
        private PVEnum pvSeverity = null;
        private int startSeverity = 0;
        private int newSeverity = 0;
        
        private PVTimeStamp pvTimeStamp = null;
        private TimeStamp timeStamp = new TimeStamp();
        
        private ProcessInstance(DBRecord record) {
            dbRecord = record;
            recordProcessSupportName = "recordProcess(" + dbRecord.getRecordName() + ")";
            recordSupport = dbRecord.getSupport();
            if(recordSupport==null) {
                throw new IllegalStateException(
                    dbRecord.getRecordName() + " has no support");
            }
            DBData[] dbDatas = dbRecord.getFieldDBDatas();
            DBData dbData;
            int index = dbRecord.getFieldDBDataIndex("status");
            if(index>=0) {
                dbData = dbDatas[index];
                if(dbData.getField().getType()==Type.pvString)
                    pvStatus = (PVString)dbData;
            }
            index = dbRecord.getFieldDBDataIndex("severity");
            if(index>=0) {
                dbData = dbDatas[index];
                if(dbData.getField().getType()==Type.pvEnum)
                    pvSeverity = (PVEnum)dbData;
            }
            index = dbRecord.getFieldDBDataIndex("timeStamp");
            if(index>=0) {
                pvTimeStamp = PVTimeStamp.create(dbDatas[index]);
            }
            index = dbRecord.getFieldDBDataIndex("scan");
            if(index>=0) {
                dbData = dbDatas[index];
                if(dbData.getField().getType()==Type.pvStructure) {
                    scanSupport = dbData.getSupport();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isDisabled()
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
         * @see org.epics.ioc.dbProcess.RecordProcess#setDisabled(boolean)
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
         * @see org.epics.ioc.dbProcess.RecordProcess#isActive()
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
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecord()
         */
        public DBRecord getRecord() {
            return dbRecord;
        }     
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#initialize()
         */
        public void initialize() {
            dbRecord.lock();
            try {
                if(trace) traceMessage(" initialize");
                if(scanSupport!=null) scanSupport.initialize();
                recordSupport.initialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#start()
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
         * @see org.epics.ioc.dbProcess.RecordProcess#stop()
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
         * @see org.epics.ioc.dbProcess.RecordProcess#uninitialize()
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
         * @see org.epics.ioc.dbProcess.RecordProcess#setRecordProcessRequestor(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public boolean setRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(this.recordProcessRequestor==null) {
                    this.recordProcessRequestor = recordProcessRequestor;
                    return true;
                }
                dbRecord.message(
                    recordProcessRequestor.getRecordProcessRequestorName()
                    + " called setRecordProcessRequestor but recordProcessRequestor is "
                    + this.recordProcessRequestor.getRecordProcessRequestorName(),
                    IOCMessageType.error);
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecordProcessRequestorName()
         */
        public String getRecordProcessRequestorName() {
            dbRecord.lock();
            try {
                if(recordProcessRequestor==null) return null;
                return recordProcessRequestor.getRecordProcessRequestorName();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#releaseRecordProcessRequestor(org.epics.ioc.dbProcess.RecordProcessRequestor)
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
         * @see org.epics.ioc.dbProcess.RecordProcess#releaseRecordProcessRequestor()
         */
        public void releaseRecordProcessRequestor() {
            dbRecord.lock();
            try {
                dbRecord.message("recordProcessRequestor is being released", IOCMessageType.error);
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
         * @see org.epics.ioc.dbProcess.RecordProcess#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public RequestResult process(RecordProcessRequestor recordProcessRequestor)
        {
            return process(recordProcessRequestor,null);
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#process(org.epics.ioc.dbProcess.RecordProcessRequestor, org.epics.ioc.util.TimeStamp)
         */
        public RequestResult process(RecordProcessRequestor recordProcessRequestor,
        TimeStamp timeStamp)
        {
            RequestResult requestResult = RequestResult.success;
            dbRecord.lock();
            try {
                if(active) {
                    if(trace) traceMessage(
                        "process request when active "
                        + recordProcessRequestor.getRecordProcessRequestorName());
                    return RequestResult.failure;
                }
                requestResult = beforeProcessing(recordProcessRequestor);
                if(requestResult!=RequestResult.success) return requestResult;
                if(timeStamp==null) {
                    if(trace) traceMessage(
                        "process with system timeStamp "
                        + recordProcessRequestor.getRecordProcessRequestorName()); 
                    TimeUtility.set(this.timeStamp,System.currentTimeMillis());
                } else {
                    if(trace) traceMessage(
                        "process with callers timeStamp "
                        + recordProcessRequestor.getRecordProcessRequestorName()); 
                    this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
                    this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
                }
                processIsRunning = true;
                requestResult = recordSupport.process(this);
                processIsRunning = false;
                if(requestResult!=RequestResult.active) {
                    finishStatusSeverityTimeStamp();
                    active = false;
                    if(trace) traceMessage("process completion");
                }
            } finally {
                dbRecord.unlock();
            }
            if(requestResult==RequestResult.active) {
                afterProcessing(requestResult);
            }
            return requestResult;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#preProcess(org.epics.ioc.dbProcess.RecordPreProcessRequestor)
         */
        public RequestResult preProcess(RecordProcessRequestor recordProcessRequestor,
        SupportPreProcessRequestor supportPreProcessRequestor)
        {
            return preProcess(recordProcessRequestor,supportPreProcessRequestor, null);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#preProcess(org.epics.ioc.dbProcess.RecordProcessRequestor, org.epics.ioc.dbProcess.SupportPreProcessRequestor, org.epics.ioc.util.TimeStamp)
         */
        public RequestResult preProcess(RecordProcessRequestor recordProcessRequestor,
        SupportPreProcessRequestor supportPreProcessRequestor, TimeStamp timeStamp)
        {
            RequestResult requestResult = RequestResult.success;
            dbRecord.lock();
            try {
                if(active) {
                    if(trace) traceMessage(
                        "preProcess request when active "
                        + recordProcessRequestor.getRecordProcessRequestorName());
                    return RequestResult.failure;
                }
                requestResult = beforeProcessing(recordProcessRequestor);
                if(requestResult!=RequestResult.success) return requestResult;
                if(timeStamp==null) {
                    if(trace) traceMessage(
                        "preProcess with system timeStamp "
                        + recordProcessRequestor.getRecordProcessRequestorName()); 
                    TimeUtility.set(this.timeStamp,System.currentTimeMillis());
                } else {
                    if(trace) traceMessage(
                        "preProcess with callers timeStamp "
                        + recordProcessRequestor.getRecordProcessRequestorName()); 
                    this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
                    this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
                }
                requestResult = supportPreProcessRequestor.ready();
                if(requestResult!=RequestResult.active) {
                    finishStatusSeverityTimeStamp();
                    active = false;
                    if(trace) traceMessage("process completion");
                }
            } finally {
                dbRecord.unlock();
            }
            if(requestResult==RequestResult.active) {
                afterProcessing(requestResult);
            }
            return requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#processNow(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public RequestResult processNow(RecordProcessRequestor recordProcessRequestor) {
            if(!active || recordProcessRequestor!=this.recordProcessRequestor) {
                throw new IllegalStateException("illegal processNow request");
            }
            processIsRunning = true;
            RequestResult requestResult = recordSupport.process(this);
            processIsRunning = false;
            return requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setTrace(boolean)
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
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecordProcessSupport()
         */
        public RecordProcessSupport getRecordProcessSupport() {
            return this;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#processContinue()
         */
        public void processContinue(ProcessContinueListener processContinueListener) {
            boolean callbackListEmpty = true;
            RecordProcessRequestor saveRecordProcessRequestor = null;
            dbRecord.lock();
            try {
                if(!active) {
                    throw new IllegalStateException("processContinue called but record is not active");
                }
                if(trace) {
                    traceMessage("processContinue " + processContinueListener.getName());
                }
                processContinueIsRunning = true;
                processContinueListener.processContinue();
                processContinueIsRunning = false;
                if(!continueProcessCallbackListenerList.isEmpty()) callbackListEmpty = false;
                if(processIsComplete) {
                    updateStatusSeverityTimeStamp();          
                    saveRecordProcessRequestor = recordProcessRequestor;
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
                    finishStatusSeverityTimeStamp();
                    active = false;
                    if(trace) traceMessage("process completion " + recordSupport.getName());
                }
            } finally {
                dbRecord.unlock();
            }
            if(saveRecordProcessRequestor!=null) {
                saveRecordProcessRequestor.recordProcessComplete(requestResult);
                return;
            }
            if(!callbackListEmpty) while(true) {
                ProcessCallbackListener processCallbackListener;
                /*
                 * Must lock because callback can again call RecordProcess.processContinue
                 */
                dbRecord.lock();
                try {
                    if(continueProcessCallbackListenerList.size()<=0) break;
                    processCallbackListener = continueProcessCallbackListenerList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processCallbackListener.processCallback();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#requestProcessCallback(org.epics.ioc.dbProcess.ProcessCallbackListener)
         */
        public void requestProcessCallback(ProcessCallbackListener processCallbackListener) {
            if(!active) {
                throw new IllegalStateException("requestProcessCallback called but record is not active");
            }
            if(trace) {
                traceMessage("requestProcessCallback " + processCallbackListener.getName());
            }
            if(processIsRunning) {
                processProcessCallbackListenerList.add(processCallbackListener);
                return;
            }
            if(processContinueIsRunning) {
                continueProcessCallbackListenerList.add(processCallbackListener);
                return;
            }
            throw new IllegalStateException("Support called requestProcessCallback illegally");
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity) {
            if(trace) traceMessage("setStatusSeverity " + status + " " + alarmSeverity.toString());
            if(alarmSeverity.ordinal()>newSeverity) {  
                newStatus = status;
                newSeverity = alarmSeverity.ordinal();
                return true;
            }
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setStatus(java.lang.String)
         */
        public boolean setStatus(String status) {
            if(trace) traceMessage("setStatus " + status);
            if(newSeverity>0) return false;
            newStatus = status;
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getAlarmSeverity()
         */
        public AlarmSeverity getAlarmSeverity() {
            if(newSeverity<0) return AlarmSeverity.none;
            return AlarmSeverity.getSeverity(newSeverity);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getStatus()
         */
        public String getStatus() {
            return newStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void setTimeStamp(TimeStamp timeStamp) {
            if(trace) traceMessage("setTimeStamp");
            this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
            this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            timeStamp.secondsPastEpoch = this.timeStamp.secondsPastEpoch;
            timeStamp.nanoSeconds = this.timeStamp.nanoSeconds;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getSupportProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return recordProcessSupportName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#processComplete(org.epics.ioc.dbProcess.RequestResult)
         */
        public void processComplete(RequestResult requestResult) {
            if(!processContinueIsRunning) {
                throw new IllegalStateException("processComplete not called via processContinue");
            }
            processIsComplete = true;
            this.requestResult = requestResult;
        }       
        
        private void traceMessage(String message) {
            dbRecord.message(
                message + " thread " + Thread.currentThread().getName(),
                IOCMessageType.info);
        }
        
        private RequestResult beforeProcessing(RecordProcessRequestor recordProcessRequestor) {
            if(isDisabled()) {
                if(trace) traceMessage("disabled");
                setStatusSeverity("record is disabled",AlarmSeverity.invalid);
                updateStatusSeverityTimeStamp();
                return RequestResult.failure;
            }
            SupportState supportState = recordSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                if(trace) traceMessage("record support not ready");
                setStatusSeverity("supportState is " + supportState.toString(),AlarmSeverity.invalid);
                updateStatusSeverityTimeStamp();
                return RequestResult.failure;
            }
            if(this.recordProcessRequestor==null || recordProcessRequestor!=recordProcessRequestor) {
                String requestorName = null;
                if(recordProcessRequestor!=null) {
                    requestorName = recordProcessRequestor.getRecordProcessRequestorName();
                }
                String message;
                if(this.recordProcessRequestor!=null) {
                    message = "no processRequestor registered but process request by " + requestorName;
                } else {
                    message = "process request by " + requestorName
                    + " but " + this.recordProcessRequestor + " is registered requestor"; 
                }
                if(trace) traceMessage(message);
                setStatusSeverity(message,AlarmSeverity.minor);
                updateStatusSeverityTimeStamp();
                return RequestResult.failure;
            }
            if(pvStatus!=null) {
                startStatus = pvStatus.get();
            } else {
                startStatus = newStatus;
            }
            if(pvSeverity!=null) {
                startSeverity = pvSeverity.getIndex();
            } else {
                startSeverity = newSeverity;
            }
            newStatus = null;
            newSeverity = 0;
            active = true;
            processIsComplete = false;
            dbRecord.beginSynchronous();
            return RequestResult.success;
        }
        
        private void afterProcessing(RequestResult requestResult) {
            // called only by process and preProcess after unlocking record
            while(true) {
                ProcessCallbackListener processCallbackListener;
                /*
                 * No need to lock because the list can only be modified by
                 * code that was called directly or indirectly by process
                 * AND process will only be called if the record is not active.
                 */
                if(processProcessCallbackListenerList.size()<=0) break;
                processCallbackListener = processProcessCallbackListenerList.remove(0);
                processCallbackListener.processCallback();
            }
            return;
        }
        
        private void updateStatusSeverityTimeStamp() {
            if(newSeverity!=startSeverity) {
                if(pvSeverity!=null) pvSeverity.setIndex(newSeverity);
            }
            if(newStatus!=startStatus) {
                if(pvStatus!=null) pvStatus.put(newStatus);
            }
            if(pvTimeStamp!=null) {
                pvTimeStamp.put(timeStamp);
            }
        }  
        
        private void finishStatusSeverityTimeStamp() {
            if(!processProcessCallbackListenerList.isEmpty()
            || !continueProcessCallbackListenerList.isEmpty()){
                dbRecord.message(
                    "completing processing but ProcessCallbackListeners are still present",
                    IOCMessageType.fatalError);
            }
            updateStatusSeverityTimeStamp();
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(newSeverity); 
            recordProcessRequestor.recordProcessResult(alarmSeverity, newStatus, timeStamp);
            dbRecord.endSynchronous();
        }
    }
}
