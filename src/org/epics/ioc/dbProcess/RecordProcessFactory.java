/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;
import java.util.concurrent.locks.*;

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
    RecordProcess,RecordProcessSupport,ProcessCompleteListener
    {
        private boolean trace = false;
        private DBRecord dbRecord;
        private boolean disabled = false;
        private Support recordSupport = null;
        private Support scanSupport = null;
        
        private boolean active = false;
        private List<ProcessCompleteListener> processListenerList =
            new ArrayList<ProcessCompleteListener>();
        private List<ProcessCallbackListener> processCallbackListenerList =
            new ArrayList<ProcessCallbackListener>();
        private PVString status = null;
        private PVEnum severity = null;
        
        private String startStatus;
        private String newStatus;
        private int startSeverity;
        private int newSeverity;
        private boolean timeStampSet;
        private TimeStamp timeStamp = new TimeStamp();
        private TimeStampField timeStampField = null;
        
        private ProcessInstance(DBRecord record) {
            dbRecord = record;
            recordSupport = dbRecord.getSupport();
            if(recordSupport==null) {
                throw new IllegalStateException(
                    record.getRecordName() + " has no support");
            }
            DBData[] dbData = dbRecord.getFieldDBDatas();
            DBData data;
            int index = record.getFieldDBDataIndex("status");
            if(index>=0) {
                data = dbData[index];
                if(data.getField().getType()==Type.pvString)
                    status = (PVString)data;
            }
            index = record.getFieldDBDataIndex("alarmSeverity");
            if(index>=0) {
                data = dbData[index];
                if(data.getField().getType()==Type.pvEnum)
                    severity = (PVEnum)data;
            }
            index = record.getFieldDBDataIndex("timeStamp");
            if(index>=0) {
                timeStampField = TimeStampField.create(dbData[index]);
            }
            timeStampSet = false;
            TimeUtility.set(timeStamp,System.currentTimeMillis());
            index = record.getFieldDBDataIndex("scan");
            if(index>=0) {
                data = dbData[index];
                if(data.getField().getType()==Type.pvStructure) {
                    scanSupport = data.getSupport();
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
                if(scanSupport!=null) scanSupport.initialize();
                recordSupport.initialize();
            } finally {
                dbRecord.unlock();
            }
        }

        public void start() {
            dbRecord.lock();
            try {
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
                recordSupport.stop();
            } finally {
                dbRecord.unlock();
            }
        }

        public void uninitialize() {
            dbRecord.lock();
            try {
                if(scanSupport!=null) scanSupport.uninitialize();
                recordSupport.uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#process(org.epics.ioc.dbProcess.ProcessCompleteListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            ProcessReturn result = ProcessReturn.success;
            dbRecord.lock();
            try {
                if(trace) {
                    dbRecord.message("process",IOCMessageType.info);
                }
                if(active) {
                    if(listener!=null) {
                        processListenerList.add(listener);
                    }
                    return ProcessReturn.alreadyActive;
                }
                RecordState recordState = dbRecord.getRecordState();
                if(recordState!=RecordState.constructed) {
                    setStatusSeverity("recordState is " + recordState.toString(),AlarmSeverity.invalid);
                    return ProcessReturn.failure;
                }
                if(isDisabled()) {
                    setStatusSeverity("record is disabled",AlarmSeverity.invalid);
                    return ProcessReturn.failure;
                }
                SupportState supportState = recordSupport.getSupportState();
                if(supportState!=SupportState.ready) {
                    setStatusSeverity("supportState is " + supportState.toString(),AlarmSeverity.invalid);
                    return ProcessReturn.failure;
                }
                dbRecord.beginSynchronous();
                if(status!=null) startStatus = status.get();
                if(severity!=null) startSeverity = severity.getIndex();
                newStatus = null;
                newSeverity = -1;
                active = true;
                result = recordSupport.process(this);
                dbRecord.endSynchronous();
                switch(result) {
                case zombie:
                case noop:
                case success:
                case failure:
                    break;
                case active:
                    if(listener!=null) {
                        processListenerList.add(listener);
                    }
                    break;
                case alreadyActive:
                    throw new IllegalStateException("RecordProcess.process why was recordSupport already active");
                default:
                    throw new IllegalStateException("RecordProcess.process logic error");
                }
                
            } finally {
                dbRecord.unlock();
            }
            // Since processCallbackListenerList can only be called while process is active no need to lock
            if(!processCallbackListenerList.isEmpty()) {
                while(processCallbackListenerList.size()>0) {
                    ProcessCallbackListener processCallbackListener = processCallbackListenerList.remove(0);
                    processCallbackListener.callback();
                }
            }
            if(result!=ProcessReturn.active) {
                dbRecord.lock();
                updateStatusSeverityTimeStamp();
                active = false;
                dbRecord.unlock();
            }
            return result;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#update()
         */
        public void update() {
            dbRecord.lock();
            try {
                if(trace) {
                    dbRecord.message("update",IOCMessageType.info);
                }
                if(!active) {
                    return;
                }
                recordSupport.update();
            } finally {
                dbRecord.unlock();
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
         * @see org.epics.ioc.dbProcess.RecordProcess#removeCompletionListener(org.epics.ioc.dbProcess.ProcessCompleteListener)
         */
        public void removeCompletionListener(ProcessCompleteListener listener) {
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
            dbRecord.lock();
            try {
                if(trace) {
                    dbRecord.message("processContinue",IOCMessageType.info);
                }
                if(!active) {
                    return;
                }
                support.processContinue();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#requestProcessCallback(org.epics.ioc.dbProcess.ProcessCallbackListener)
         */
        public void requestProcessCallback(ProcessCallbackListener processCallbackListener) {
            processCallbackListenerList.add(processCallbackListener);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity) {
            if(newSeverity<0 || alarmSeverity.ordinal()>newSeverity) {  
                newStatus = status;
                newSeverity = alarmSeverity.ordinal();
                return true;
            }
            return false;
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
        public void setTimeStamp(TimeStamp newTimeStamp) {
            timeStamp.secondsPastEpoch = newTimeStamp.secondsPastEpoch;
            timeStamp.nanoSeconds = newTimeStamp.nanoSeconds;
            timeStampSet = true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            timeStamp.secondsPastEpoch = this.timeStamp.secondsPastEpoch;
            timeStamp.nanoSeconds = this.timeStamp.nanoSeconds;
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            dbRecord.lock();
            try {
                if(trace) {
                    dbRecord.message("processComplete",IOCMessageType.info);
                }
                if(!active) return;
                updateStatusSeverityTimeStamp();
                // In most cases processListenerList is empty.
                // checking here saves an extra lock/unlock
                if(processListenerList.isEmpty()) {
                    active = false;
                    return;
                }
            } finally {
                dbRecord.unlock();
            }
            while(true) {
                ProcessCompleteListener processListener;
                dbRecord.lock();
                try {
                    if(processListenerList.isEmpty()) {
                        active = false;
                        return;
                    }
                    processListener = processListenerList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processListener.processComplete(support,result);
            }
        }
        
        private void updateStatusSeverityTimeStamp() {
            if(newSeverity!=startSeverity) {
                if(newSeverity<0) newSeverity = 0;
                if(severity!=null) severity.setIndex(newSeverity);
            }
            if(newStatus!=startStatus) {
                if(status!=null) status.put(newStatus);
            }
            if(timeStampField!=null) {
                if(!timeStampSet) {
                    TimeUtility.set(timeStamp,System.currentTimeMillis());
                }
                timeStampField.put(timeStamp);
            }
        }
    }
}
