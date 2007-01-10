/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;

import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Factory for creating an IOCDB.
 * @author mrk
 *
 */
public class IOCDBFactory {
    private static IOCDBInstance master = new IOCDBInstance("master");
    /**
     * Create an IOCDB.
     * @param name The name for the IOCDB.
     * @return The newly created IOCDB or the master IOCDB if master is requested.
     */
    public static IOCDB create(String name) {
        if(name.equals("master")) return master;
        return new IOCDBInstance(name);
    }
    /**
     * Get the master IOC Database.
     * @return The master.
     */
    public static IOCDB getMaster() {
        return master;
    }
    
    private static class IOCDBInstance implements IOCDB
    {
        private String name;
        private ArrayList<IOCDBMergeListener> mergeListenerList =
            new ArrayList<IOCDBMergeListener>();
        private LinkedHashMap<String,DBRecord> recordMap = 
            new LinkedHashMap<String,DBRecord>();
        private ReentrantReadWriteLock rwLock = 
            new ReentrantReadWriteLock();
        private boolean workingRequestorListModified = false;
        private ArrayList<Requestor> workingRequestorList =
            new ArrayList<Requestor>();
        private ArrayList<Requestor> messageListenerList =
            new ArrayList<Requestor>();
        
        private IOCDBInstance(String name) {
            this.name = name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#getMaster()
         */
        public IOCDB getMaster() {
            return master;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#getName()
         */
        public String getName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#mergeIntoMaster()
         */
        public void mergeIntoMaster() {
            if(this==master) return;
            ArrayList<IOCDBMergeListener> mergeListenerList = null;
            rwLock.writeLock().lock();
            try {
                master.merge(recordMap);
                recordMap.clear();
                mergeListenerList = (ArrayList<IOCDBMergeListener>)(this.mergeListenerList.clone());
                this.mergeListenerList.clear();
            } finally {
                rwLock.writeLock().unlock();
            }
            Iterator<IOCDBMergeListener> iter = mergeListenerList.iterator();
            while(iter.hasNext()) {
                iter.next().merged();
                iter.remove();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#addIOCDBMergeListener(org.epics.ioc.db.IOCDBMergeListener)
         */
        public void addIOCDBMergeListener(IOCDBMergeListener listener) {
            if(this!=master) {
                rwLock.writeLock().lock();
                try {
                    mergeListenerList.add(listener);
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
        }
        // merge allows master to be locked once instead of for each record instance
        private void merge(LinkedHashMap<String,DBRecord> from) {
            Set<String> keys;
            rwLock.writeLock().lock();
            try {
                keys = from.keySet();
                for(String key: keys) {
                    DBRecord dbRecord = from.get(key);
                    dbRecord.setIOCDB(this);
                    recordMap.put(key,dbRecord);
                }
            }  finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#findRecord(java.lang.String)
         */
        public DBRecord findRecord(String recordName) {
            rwLock.readLock().lock();
            try {
                DBRecord record = null;
                record = recordMap.get(recordName);
                if(record==null && this!=master) record = master.findRecord(recordName);
                return record;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#addRecord(org.epics.ioc.db.DBRecord)
         */
        public boolean addRecord(DBRecord record) {
            rwLock.writeLock().lock();
            try {
                String key = record.getRecordName();
                if(recordMap.containsKey(key)) return false;
                if(this!=master && master.findRecord(key)!=null) return false;
                recordMap.put(key,record);
                record.setIOCDB(this);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#removeRecord(org.epics.ioc.db.DBRecord)
         */
        public boolean removeRecord(DBRecord record) {
            rwLock.writeLock().lock();
            try {
                String key = record.getRecordName();
                if(recordMap.remove(key)!=null) return true;
                return false;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#getRecordMap()
         */
        public Map<String,DBRecord> getRecordMap() {
            rwLock.readLock().lock();
            try {
                return (Map<String, DBRecord>)recordMap.clone();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            rwLock.writeLock().lock();
            try {                
                if(workingRequestorListModified) {
                    workingRequestorList = (ArrayList<Requestor>)messageListenerList.clone();
                    workingRequestorListModified = false;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            if(workingRequestorList.size()<=0) {
                System.out.println(messageType.toString() + " " + message);
                return;
            }
            Iterator<Requestor> iter = workingRequestorList.iterator();
            while(iter.hasNext()) {
                iter.next().message(message, messageType);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#addRequestor(org.epics.ioc.util.Requestor)
         */
        public void addRequestor(Requestor requestor) {
            rwLock.writeLock().lock();
            try {
                workingRequestorList.add(requestor);
                workingRequestorListModified = true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#removeRequestor(org.epics.ioc.util.Requestor)
         */
        public void removeRequestor(Requestor requestor) {
            rwLock.writeLock().lock();
            try {
                workingRequestorList.remove(requestor);
                workingRequestorListModified = true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#recordList(java.lang.String)
         */
        public String[] recordList(String regularExpression) {
            ArrayList<String> list = new ArrayList<String>();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return new String[0];
            }
            rwLock.readLock().lock();
            try {
                Set<Map.Entry<String,DBRecord>> recordSet = recordMap.entrySet();
                Iterator<Map.Entry<String,DBRecord>> iter = recordSet.iterator();
                while(iter.hasNext()) {
                    Map.Entry<String,DBRecord> entry = iter.next();
                    String name = entry.getKey();
                    if(pattern.matcher(name).matches()) {
                        list.add(name);
                    }
                }
                String[] result = new String[list.size()];
                for(int i=0; i< list.size(); i++) result[i] = list.get(i);
                return result;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#recordToString(java.lang.String)
         */
        public String recordToString(String regularExpression) {
            StringBuilder result = new StringBuilder();
            if(regularExpression==null) regularExpression = ".*";
            Pattern pattern;
            try {
                pattern = Pattern.compile(regularExpression);
            } catch (PatternSyntaxException e) {
                return "PatternSyntaxException: " + e;
            }
            rwLock.readLock().lock();
            try {
                Set<Map.Entry<String,DBRecord>> recordSet = recordMap.entrySet();
                Iterator<Map.Entry<String,DBRecord>> iter = recordSet.iterator();
                while(iter.hasNext()) {
                    Map.Entry<String,DBRecord> entry = iter.next();
                    String name = entry.getKey();
                    if(pattern.matcher(name).matches()) {
                        result.append(String.format("%nrecord %s%s",name,entry.getValue().toString()));
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }
}
