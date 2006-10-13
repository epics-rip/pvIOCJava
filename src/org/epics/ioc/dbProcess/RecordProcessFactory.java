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
    
    static private class ProcessInstance implements
    RecordProcess,RecordProcessSupport,RecordPreProcess,
    ProcessRequestListener
    {
        private boolean trace = false;
        private DBRecord dbRecord;
        private boolean disabled = false;
        private Support recordSupport = null;
        private Support scanSupport = null;
        
        private boolean active = false;
        private DBRecord rootForActive = null;
        private List<ProcessRequestListener> processListenerList =
            new ArrayList<ProcessRequestListener>();
        private boolean processIsRunning = false;
        private List<ProcessCallbackListener> processProcessCallbackListenerList =
            new ArrayList<ProcessCallbackListener>();
        private boolean processContinueIsRunning = false;
        private List<ProcessCallbackListener> continueProcessCallbackListenerList =
            new ArrayList<ProcessCallbackListener>();
        private boolean updateIsRunning = false;
        private List<ProcessCallbackListener> updateProcessCallbackListenerList =
            new ArrayList<ProcessCallbackListener>();
        
        private List<RecordPreProcessListener> recordPreProcessListenerList =
            new ArrayList<RecordPreProcessListener>();
        
        private boolean callCompleteProcessing = false;
        private boolean callStopAfterActive = false;
        private boolean callUninitializeAfterActive = false;
        
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
            dbRecord.lock();
            try {
                return dbRecord;
            } finally {
                dbRecord.unlock();
            }
        }     
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

        public void stop() {
            dbRecord.lock();
            try {
                if(scanSupport!=null) scanSupport.stop();
                if(active) {
                    callStopAfterActive = true;
                    if(trace) traceMessage("stop delayed because active");
                    return;
                }
                if(trace) traceMessage("stop");
                recordSupport.stop();
            } finally {
                dbRecord.unlock();
            }
        }

        public void uninitialize() {
            dbRecord.lock();
            try {
                if(scanSupport!=null) scanSupport.uninitialize();
                if(active) {
                    callUninitializeAfterActive = true;
                    if(trace) traceMessage("uninitialize delayed because active");
                    return;
                }
                if(trace) traceMessage("uninitialize");
                recordSupport.uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#process(org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public ProcessReturn process(ProcessRequestListener listener) {
            ProcessReturn processReturn = ProcessReturn.success;          
            dbRecord.lock();
            try {
                if(active) {
                    if(trace) traceMessage("process request when active");
                    processListenerList.add(listener);
                    return ProcessReturn.active;
                }
                processReturn = beforeProcessing();
                if(processReturn!=ProcessReturn.success) return processReturn;
                processIsRunning = true;
                processReturn = recordSupport.process(this);
                processIsRunning = false;
                if(processReturn==ProcessReturn.active) {
                    processListenerList.add(listener);
                } else {                    
                    callCompleteProcessing = true;
                }
            } finally {
                dbRecord.unlock();
            }
            afterProcessing();
            return processReturn;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#preProcess(org.epics.ioc.dbProcess.RecordPreProcessListener)
         */
        public ProcessReturn preProcess(RecordPreProcessListener listener) {
            ProcessReturn processReturn = ProcessReturn.noop;
            dbRecord.lock();
            try {
                SupportState supportState = recordSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    listener.failure("record support not ready");
                    return ProcessReturn.failure;
                }
                if(trace) traceMessage("preProcess");
                if(recordPreProcessListenerList.isEmpty() && !active) {
                    processReturn = beforeProcessing();
                    if(processReturn!=ProcessReturn.success) return processReturn;
                    processReturn = listener.readyForProcessing(this);
                } else {
                    recordPreProcessListenerList.add(listener);
                    return ProcessReturn.active;
                }
            } finally {
                dbRecord.unlock();
            }
           //this is only reached if recordPreProcessListener.readyForProcessing was called.
            afterProcessing();
            return processReturn;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#preProcess(org.epics.ioc.dbAccess.DBLink, org.epics.ioc.dbProcess.RecordPreProcessListener)
         */
        public ProcessReturn preProcess(DBLink dbLink, RecordPreProcessListener listener) {
            ProcessReturn processReturn = ProcessReturn.noop;
            dbRecord.lock();
            try {
                SupportState supportState = recordSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    listener.failure("record support not ready");
                    return ProcessReturn.failure;
                }
                ProcessInstance recordInstance = (ProcessInstance)dbLink.getRecord().getRecordProcess();
                if(trace) traceMessage("preProcess link record");
                if(recordPreProcessListenerList.isEmpty() && !active) {
                    rootForActive = recordInstance.rootForActive;
                    processReturn = beforeProcessing();
                    if(processReturn!=ProcessReturn.success) return processReturn;
                    processReturn = listener.readyForProcessing(this);
                } else {
                    if(rootForActive == recordInstance.rootForActive) {
                        DBRecord linkRecord = dbLink.getRecord();
                        linkRecord.message(
                                "Request to process record " + dbRecord.getRecordName()
                                + " is recursive request", IOCMessageType.fatalError);
                        return ProcessReturn.failure;
                    }
                    recordPreProcessListenerList.add(listener);
                    return ProcessReturn.active;
                }
            } finally {
                dbRecord.unlock();
            }
           //this is only reached if recordPreProcessListener.readyForProcessing was called.
            afterProcessing();
            return processReturn;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordPreProcess#processNow(org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public ProcessReturn processNow(ProcessRequestListener listener) {
            if(listener==null) {
                throw new IllegalStateException("process call with null listener");
            }
            processIsRunning = true;
            ProcessReturn processReturn = recordSupport.process(this);
            processIsRunning = false;
            if(processReturn==ProcessReturn.active) {
                processListenerList.add(listener);
            } else {
                callCompleteProcessing = true;
            }
            
            return processReturn;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#update()
         */
        public void update() {
            boolean callbackListEmpty = true;
            dbRecord.lock();
            try {
                if(!active) {
                    if(trace) traceMessage("update but record is not active");
                    return;
                }
                if(trace) traceMessage("update");
                updateIsRunning = true;
                recordSupport.update();
                updateIsRunning = false;
                if(!updateProcessCallbackListenerList.isEmpty()) callbackListEmpty = false;
            } finally {
                dbRecord.unlock();
            }
            if(callbackListEmpty) return;
            while(true) {
                ProcessCallbackListener processCallbackListener;
                /*
                 * Must lock because callback can again call RecordProcess.update
                 */
                dbRecord.lock();
                try {
                    if(updateProcessCallbackListenerList.size()<=0) {
                        return;
                    }
                    processCallbackListener = updateProcessCallbackListenerList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processCallbackListener.processCallback();
            }
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
         * @see org.epics.ioc.dbProcess.RecordProcess#removeCompletionListener(org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public void removeCompletionListener(ProcessRequestListener listener) {
            dbRecord.lock();
            try {
                processListenerList.remove(listener);
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
        public void processContinue(Support support) {
            ProcessContinueReturn processContinueReturn;
            boolean isRecordSupport = (support==recordSupport) ? true : false;
            dbRecord.lock();
            try {
                if(!active) {
                    if(trace) {
                        traceMessage(
                            "processContinue " + support.getName()
                            + " but record is not active");
                    }
                    return;
                }
                if(trace) {
                    traceMessage("processContinue " + support.getName());
                }
                processContinueIsRunning = true;
                processContinueReturn = support.processContinue();
                processContinueIsRunning = false;
                // If not record support then just act like it is active
                if(!isRecordSupport) processContinueReturn = ProcessContinueReturn.active;
                dbRecord.endSynchronous();
            } finally {
                dbRecord.unlock();
            }
            while(true) {
                ProcessCallbackListener processCallbackListener;
                /*
                 * Must lock because callback can again call RecordProcess.processContinue
                 */
                dbRecord.lock();
                try {
                    if(continueProcessCallbackListenerList.size()<=0) {
                        if(processContinueReturn!=ProcessContinueReturn.active) {
                            completeProcessing();
                        }
                        return;
                    }
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
            if(processIsRunning) {
                processProcessCallbackListenerList.add(processCallbackListener);
                return;
            }
            if(processContinueIsRunning) {
                continueProcessCallbackListenerList.add(processCallbackListener);
                return;
            }
            if(updateIsRunning) {
                updateProcessCallbackListenerList.add(processCallbackListener);
                return;
            }
            dbRecord.message("Support called requestProcessCallback illegally",IOCMessageType.fatalError);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#processLinkedRecord(org.epics.ioc.dbAccess.DBLink, org.epics.ioc.dbAccess.DBRecord, org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public ProcessReturn processLinkedRecord(DBRecord record, ProcessRequestListener listener) {            
            dbRecord.lockOtherRecord(record);
            try {
                if(trace) {
                    traceMessage("processLinkedRecord " + record.getRecordName());
                }
                ProcessInstance recordInstance = (ProcessInstance)record.getRecordProcess();
                if(rootForActive == recordInstance.rootForActive) {
                    dbRecord.message(
                            "Request to process record " + record.getRecordName()
                            + " is recursive request", IOCMessageType.fatalError);
                    return ProcessReturn.failure;
                }
                recordInstance.rootForActive = rootForActive;
                return record.getRecordProcess().process(listener); 
            } finally {
                record.unlock();
            }    
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
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#processComplete(org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete() {
            dbRecord.lock();
            try {
                if(!active) {
                    dbRecord.message("processComplete but record is not active",IOCMessageType.fatalError);
                    return;
                }
                if(trace) traceMessage("processComplete");
            } finally {
                dbRecord.unlock();
            }
            completeProcessing();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do
        }
        
        private void traceMessage(String message) {
            dbRecord.message(
                message + " thread " + Thread.currentThread().getName(),
                IOCMessageType.info);
        }
        
        private ProcessReturn beforeProcessing() {           
            RecordState recordState = dbRecord.getRecordState();
            if(recordState!=RecordState.constructed) {
                if(trace) traceMessage("not constructed");
                setStatusSeverity("recordState is " + recordState.toString(),AlarmSeverity.invalid);
                updateStatusSeverityTimeStamp();
                return ProcessReturn.failure;
            }
            if(isDisabled()) {
                if(trace) traceMessage("disabled");
                setStatusSeverity("record is disabled",AlarmSeverity.invalid);
                updateStatusSeverityTimeStamp();
                return ProcessReturn.failure;
            }
            SupportState supportState = recordSupport.getSupportState();
            if(supportState!=SupportState.ready) {
                if(trace) traceMessage("record support not ready");
                setStatusSeverity("supportState is " + supportState.toString(),AlarmSeverity.invalid);
                updateStatusSeverityTimeStamp();
                return ProcessReturn.failure;
            }
            if(trace) traceMessage("process");               
            if(pvStatus!=null) startStatus = pvStatus.get();
            if(pvSeverity!=null) startSeverity = pvSeverity.getIndex();
            newStatus = null;
            newSeverity = 0;
            if(rootForActive==null) {
                TimeUtility.set(timeStamp,System.currentTimeMillis());
                rootForActive = dbRecord;
            } else {
                rootForActive.getRecordProcess().getRecordProcessSupport().getTimeStamp(timeStamp);
            }
            callCompleteProcessing = false;
            active = true;
            dbRecord.beginSynchronous();
            return ProcessReturn.success;
        }
        
        private void afterProcessing() {
            while(true) {
                ProcessCallbackListener processCallbackListener;
                /*
                 * No need to lock because the list can only be modified by
                 * code that was called directly or indirectly by process
                 * AND process will only be called if the record is not active.
                 */
                if(processProcessCallbackListenerList.size()<=0) {
                    break;
                }
                processCallbackListener = processProcessCallbackListenerList.remove(0);
                processCallbackListener.processCallback();
            }
            if(callCompleteProcessing) completeProcessing();
            return;
        }
        
        private void completeProcessing() {
            if(!processProcessCallbackListenerList.isEmpty()
            || !continueProcessCallbackListenerList.isEmpty()
            || !updateProcessCallbackListenerList.isEmpty()) {
                dbRecord.message(
                    "completing processing but ProcessCallbackListeners are still present",
                    IOCMessageType.fatalError);
            }
            callCompleteProcessing = false;
            rootForActive = null;
            updateStatusSeverityTimeStamp();
            AlarmSeverity alarmSeverity = AlarmSeverity.none;
            if(pvSeverity!=null) alarmSeverity = AlarmSeverity.getSeverity(pvSeverity.getIndex());
            outerLoop:
            while(true) {
                ProcessRequestListener processListener;
                dbRecord.lock();
                try {
                    if(processListenerList.isEmpty()) {
                        dbRecord.endSynchronous();
                        if(callStopAfterActive) {
                            if(trace) traceMessage(" stop");
                            recordSupport.stop();
                            callStopAfterActive = false;
                        }
                        if(callUninitializeAfterActive) {
                            if(trace) traceMessage("uninitialize");
                            recordSupport.uninitialize();
                            callUninitializeAfterActive = false;
                        }
                        if(!recordPreProcessListenerList.isEmpty()) {
                            RecordPreProcessListener listener = recordPreProcessListenerList.get(0);
                            ProcessReturn processReturn = beforeProcessing();
                            if(processReturn==ProcessReturn.success) {
                                listener.readyForProcessing(this);
                                // must call afterProcessing unlocked
                                break outerLoop;
                            }
                            while(listener!=null) {
                                listener.failure(processReturn.toString());
                                if(recordPreProcessListenerList.isEmpty()) {
                                    listener = null;
                                } else {
                                    listener = recordPreProcessListenerList.get(0);
                                }
                            }
                        }
                        active = false;
                        return;
                    }
                    processListener = processListenerList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processListener.requestResult(alarmSeverity, newStatus, timeStamp);
                processListener.processComplete();
            }
            // this is only reached if recordPreProcessListener was called.
            afterProcessing();
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
    }
}
