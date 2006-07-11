/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class ProcessFactory {
    /**
     * create a process database.
     * @param iocdb the iocdb associated with the record processing.
     * @return the ProcessDB.
     */
    static public ProcessDB createProcessDB(IOCDB iocdb) {
        return new ProcessDatabase(iocdb);
    }
    
    /**
     * create a RecordProcess for a record instance.
     * @param dbRecord the record instance.
     * @return the RecordProcess interface.
     */
    static public RecordProcess createRecordProcess(DBRecord dbRecord) {
        return new Process(dbRecord);
    }
    
    static private class ProcessDatabase implements ProcessDB{
        
        private IOCDB iocdb;
        private static Map<String,RecordProcess> recordProcessMap;
        static {
            recordProcessMap = new HashMap<String,RecordProcess>();
        }
        
        /**
         * Constructor for ProcessDatabase.
         * @param iocdb the IOCDB for which RecordProcess will be created.
         */
        ProcessDatabase(IOCDB iocdb) {
            this.iocdb = iocdb;
        }
        
        public boolean addRecordProcess(RecordProcess recordProcess) {
            String recordName = recordProcess.getRecord().getRecordName();
            if(recordProcessMap.get(recordName)!=null) return false;
            if(recordProcessMap.put(recordName,recordProcess)==null) return true;
            System.out.printf("ProcessDB.addRecordProcess called twice for the record\n");
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#findRecordProcess(java.lang.String)
         */
        public RecordProcess findRecordProcess(String recordName) {
            return recordProcessMap.get(recordName);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#getIOCDB()
         */
        public IOCDB getIOCDB() {
            return iocdb;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#getRecordProcessMap()
         */
        public Map<String, RecordProcess> getRecordProcessMap() {
            return recordProcessMap;
        }

        
    }
    
    static private class Process implements RecordProcess {
        private DBRecord dbRecord;
        ReentrantLock lock;
        private boolean disabled = false;
        private RecordSupport recordSupport = null;
        
        private boolean active = false;
        private ConcurrentLinkedQueue<ProcessComplete> completionListener =
            new ConcurrentLinkedQueue<ProcessComplete>();
        
        private PVString status = null;
        private PVEnum severity = null;
        
        private String startStatus;
        private String newStatus;
        private int startSeverity;
        private int newSeverity;
        
        private List<RecordProcess> linkedRecordList = new ArrayList<RecordProcess>();
        private List<ProcessComplete> linkedProcessCompleteList = new ArrayList<ProcessComplete>();
        
        Process(DBRecord record) {
            dbRecord = record;
            lock = record.getLock();
            PVData[] pvData = dbRecord.getFieldPVDatas();
            PVData data;
            data = pvData[record.getFieldDBDataIndex("status")];
            assert(data.getField().getType()==Type.pvString);
            status = (PVString)data;
            data = pvData[record.getFieldDBDataIndex("alarmSeverity")];
            assert(data.getField().getType()==Type.pvEnum);
            severity = (PVEnum)data;
        }
        
        // Record Process
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isDisabled()
         */
        public boolean isDisabled() {
            lock.lock();
            try {
                boolean result = disabled;
                return result;
            } finally {
                lock.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setDisabled(boolean)
         */
        public boolean setDisabled(boolean value) {
            lock.lock();
            try {
                boolean oldValue = disabled;
                disabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                lock.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isActive()
         */
        public boolean isActive() {
            lock.lock();
            try {
                boolean result = active;
                return result;
            } finally {
                lock.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecord()
         */
        public DBRecord getRecord() {
            lock.lock();
            try {
                DBRecord result = dbRecord;
                return result;
            } finally {
                lock.unlock();
            }
        }

        public RecordSupport getRecordSupport() {
            lock.lock();
            try {
                RecordSupport result = recordSupport;
                return result;
            } finally {
                lock.unlock();
            }
        }

        public boolean setRecordSupport(RecordSupport support) {
            lock.lock();
            try {
                if(recordSupport!=null) return false;
                recordSupport = support;
                return true;
            } finally {
                lock.unlock();
            }
        }

        public RequestProcessReturn requestProcess(ProcessComplete listener) {
            lock.lock();
            try {
                if(active) {
                    if(listener!=null) {
                        if(completionListener.add(listener)) {
                            return RequestProcessReturn.listenerAdded;
                        } else {
                            return RequestProcessReturn.failure;
                        }
                    }
                    return RequestProcessReturn.alreadyActive;
                }
                active = true;
                return RequestProcessReturn.success;
            } finally {
                lock.unlock();
            }
        }

        public ProcessReturn process(ProcessComplete listener) {
            ProcessReturn result = ProcessReturn.abort;
            int numberLinkedRecords = 0;
            lock.lock();
            try {
                if(isDisabled()) {
                    active = false;
                    return ProcessReturn.abort;
                }
                if(recordSupport==null) {
                    active = false;
                    return ProcessReturn.noop;
                }
                dbRecord.beginSynchronous();
                startStatus = status.get();
                startSeverity = severity.getIndex();
                newStatus = null;
                newSeverity = -1;
                result = recordSupport.process(this);
                numberLinkedRecords = linkedRecordList.size();
                recordSupportDone(result);
                if(result==ProcessReturn.active) {
                    if(listener!=null) completionListener.add(listener);
                } else if(numberLinkedRecords>0) {
                    active = false;
                }
                dbRecord.endSynchronous();
            } finally {
                lock.unlock();
            }
            if(numberLinkedRecords>0) {
                for(int i=0; i < numberLinkedRecords; i++) {
                    RecordProcess linkedRecord = linkedRecordList.get(i);
                    ProcessComplete linkedListener = linkedProcessCompleteList.get(i);
                    ProcessReturn linkedResult = linkedRecord.process(linkedListener);
                    if(linkedResult!=ProcessReturn.active) {
                        linkedListener.complete(linkedResult);
                    }
                }
                linkedRecordList.clear();
                linkedProcessCompleteList.clear();
                if(result!=ProcessReturn.active) {
                    lock.lock();
                    active = false;
                    lock.unlock();
                }
            }
            return result;
        }
        
        public void removeCompletionListener(ProcessComplete listener) {
            completionListener.remove(listener);
        }

        public RequestProcessReturn requestProcessLinkedRecord(DBRecord record, ProcessComplete listener) {
            RecordProcess linkedRecordProcess = record.getRecordProcess();
            ReentrantLock otherLock = record.getLock();
            dbRecord.lockOtherRecord(record);
            try {
                RequestProcessReturn result = linkedRecordProcess.requestProcess(listener);
                if(result==RequestProcessReturn.success) {
                    linkedRecordList.add(record.getRecordProcess());
                    linkedProcessCompleteList.add(listener);
                }
                return result;
            } finally {
                otherLock.unlock();
            }
        }

        public void removeLinkedCompletionListener(ProcessComplete listener) {
            lock.lock();
            try {
                int index = linkedProcessCompleteList.indexOf(listener);
                linkedRecordList.remove(index);
                linkedProcessCompleteList.remove(index);
            } finally {
                lock.unlock();
            }
            
        }

        public void recordSupportDone(ProcessReturn result) {
            if(newSeverity!=startSeverity) {
                if(newSeverity<0) newSeverity = 0;
                severity.setIndex(newSeverity);
            }
            if(newStatus!=startStatus) {
                status.put(newStatus);
            }
            if(result==ProcessReturn.active) {
                startSeverity = newSeverity;
                startStatus = newStatus;
                newStatus = null;
                newSeverity = -1;
                callLinkedProcessListeners(result);
            } else {
                removeAndCallLinkedProcessListeners(result);
                active = false;
            }
        }
        

        public boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity) {
            if(newSeverity<0 || alarmSeverity.ordinal()>newSeverity) {  
                newStatus = status;
                newSeverity = alarmSeverity.ordinal();
                return true;
            }
            return false;
        }

        private void callLinkedProcessListeners(ProcessReturn result)
        {
            if(completionListener.isEmpty()) return;
            Iterator<ProcessComplete> iter = completionListener.iterator();
            while(iter.hasNext()) {
                iter.next().complete(result);
            }
        }
    
        private void removeAndCallLinkedProcessListeners(ProcessReturn result)
        {
            if(completionListener.isEmpty()) return;
            ProcessComplete processComplete;
            while((processComplete = completionListener.poll())!=null) {
                processComplete.complete(result);
            }
        }
        
    }
}
