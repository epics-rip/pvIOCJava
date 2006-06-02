/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.dbAccess.*;

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

        /**
         * Constructor for ProcessDatabase.
         * @param iocdb the IOCDB for which RecordProcess will be created.
         */
        ProcessDatabase(IOCDB iocdb) {
            this.iocdb = iocdb;
        }
        
        private IOCDB iocdb;
        private static Map<String,RecordProcess> recordProcessMap;
        static {
            recordProcessMap = new HashMap<String,RecordProcess>();
        }
    }
    
    static private class Process implements RecordProcess, DBMasterListener {

        public void newData(DBData dbData) {
            dbDataListenerList.add(dbData);
        }

        public void readLock() {
            readLock.lock();
        }

        public void readUnlock() {
            readLock.unlock();
        }
        
        public void writeLock() {
            writeLock.lock();
        }

        public void writeUnlock() {
            writeLock.unlock();
        }

        public boolean isDisabled() {
            return disabled.get();
        }
        
        public boolean setDisabled(boolean value) {
            return disabled.getAndSet(value); 
        }
        
        public boolean isActive() {
            return active.get();
        }
        
        public DBRecord getRecord() {
            return dbRecord;
        }

        public RecordSupport getRecordSupport() {
            return recordSupport.get();
        }

        public boolean setRecordSupport(RecordSupport support) {
            return recordSupport.compareAndSet(null,support);
        }
        
        public boolean addCompletionListener(ProcessComplete listener) {
            if(!active.get()) return false;
            return completionListener.add(listener);
        }

        public void removeCompletionListener(ProcessComplete listener) {
            completionListener.remove(listener);
        }

        public ProcessReturn requestProcess(ProcessComplete listener) {
            if(!active.compareAndSet(false,true)) {
                return ProcessReturn.alreadyActive;
            }
            RecordSupport support = recordSupport.get();
            if(support==null) {
                active.set(false);
                return ProcessReturn.noop;
            }
            if(!dbRecord.insertMasterListener(this)) {
                throw new UnsupportedOperationException(
                    "RecordProcess.requestProcess"
                    + " dbRecord.insertMasterListener failed");
            }
            ProcessReturn result = support.process(this);
            Process process;
            while((process = linkedProcess.poll())!=null) {
                ProcessComplete processComplete = linkedListener.poll();
                ProcessReturn linkedResult = process.delayedRequestProcess(
                    processComplete);
                if(linkedResult!=ProcessReturn.active) {
                    processComplete.complete(linkedResult);
                }
            }
            recordSupportDone(result);
            if(result==ProcessReturn.active && listener!=null) {
                completionListener.add(listener);
            }
            return result;
        }
        
 
        public boolean requestProcess(RecordProcess linkedRecord, ProcessComplete listener) {
            Process process = (Process)linkedRecord;
            if(!process.reserveDelayedProcess()) return false;
            linkedProcess.add(process);
            linkedListener.add(listener);
            return true;
        }
        

        public void recordSupportDone(ProcessReturn result) {
            if(result==ProcessReturn.active) {
                callLinkedProcessListeners(result);
                callDbDataListeners();
            } else {
                removeAndCallLinkedProcessListeners(result);
                removeAndCallDbDataListeners();
                dbRecord.removeMasterListener(this);
                active.set(false);
            }
        }

        
        Process(DBRecord dbRecord) {
            this.dbRecord = dbRecord;
            readWriteLock = new ReentrantReadWriteLock();
            readLock = readWriteLock.readLock();
            writeLock = readWriteLock.writeLock();
        }
        
        private AtomicBoolean disabled = new AtomicBoolean(false);
        private AtomicBoolean active = new AtomicBoolean(false);
        private AtomicReference<RecordSupport> recordSupport = 
            new AtomicReference<RecordSupport>(null);
        private DBRecord dbRecord = null;
        private ConcurrentLinkedQueue<ProcessComplete> completionListener =
            new ConcurrentLinkedQueue<ProcessComplete>();
        private ReentrantReadWriteLock readWriteLock;
        private ReentrantReadWriteLock.ReadLock readLock;
        private ReentrantReadWriteLock.WriteLock writeLock;
        private ConcurrentLinkedQueue<ProcessComplete> linkedListener =
            new ConcurrentLinkedQueue<ProcessComplete>();
        private ConcurrentLinkedQueue<Process> linkedProcess =
            new ConcurrentLinkedQueue<Process>();
        private ConcurrentLinkedQueue<DBData> dbDataListenerList =
            new ConcurrentLinkedQueue<DBData>();
        
        private boolean reserveDelayedProcess() {
            if(!active.compareAndSet(false,true)) return false;
            return true;
        }
        
        private ProcessReturn delayedRequestProcess(ProcessComplete listener) {
            if(!active.get()) return ProcessReturn.abort;
            RecordSupport support = recordSupport.get();
            if(support==null) {
                active.set(false);
                return ProcessReturn.noop;
            }
            ProcessReturn result = support.process(this);
            if(result==ProcessReturn.active) {
                if(listener!=null) completionListener.add(listener);
                return result;
            }
            callLinkedProcessListeners(result);
            active.set(false);
            return result;
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
        
        private void callDbDataListeners()
        {
            if(dbDataListenerList.isEmpty()) return;
            dbRecord.beginSynchronous();
            Iterator<DBData> iter = dbDataListenerList.iterator();
            while(iter.hasNext()) {
                DBData dbData =iter.next();
                dbData.postPut(dbData);
            }
            dbRecord.stopSynchronous();
        }
    
        private void removeAndCallDbDataListeners()
        {
            if(dbDataListenerList.isEmpty()) return;
            dbRecord.beginSynchronous();
            DBData dbData;
            while((dbData = dbDataListenerList.poll())!=null) {
                dbData.postPut(dbData);
            }
            dbRecord.stopSynchronous();
        }
    }
}
