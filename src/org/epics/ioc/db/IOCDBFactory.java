/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;

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
        private LinkedHashMap<String,DBRecord> recordMap = 
            new LinkedHashMap<String,DBRecord>();
        private ReentrantReadWriteLock rwLock = 
            new ReentrantReadWriteLock();
        private boolean workingRequesterListModified = false;
        private ArrayList<Requester> workingRequesterList =
            new ArrayList<Requester>();
        private ArrayList<Requester> messageListenerList =
            new ArrayList<Requester>();
        
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
            rwLock.writeLock().lock();
            try {
                master.merge(recordMap);
                recordMap.clear();
            } finally {
                rwLock.writeLock().unlock();
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
                String key = record.getPVRecord().getRecordName();
                if(recordMap.containsKey(key)) {
                    message("record already exists",MessageType.warning);
                    return false;
                }
                if(this!=master && master.findRecord(key)!=null) {
                    message("record already exists in master",MessageType.warning);
                    return false;
                }
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
                String key = record.getPVRecord().getRecordName();
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
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            rwLock.writeLock().lock();
            try {                
                if(workingRequesterListModified) {
                    workingRequesterList = (ArrayList<Requester>)messageListenerList.clone();
                    workingRequesterListModified = false;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            if(workingRequesterList.size()<=0) {
                System.out.println(messageType.toString() + " " + message);
                return;
            }
            Iterator<Requester> iter = workingRequesterList.iterator();
            while(iter.hasNext()) {
                iter.next().message(message, messageType);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#addRequester(org.epics.ioc.util.Requester)
         */
        public void addRequester(Requester requester) {
            rwLock.writeLock().lock();
            try {
                workingRequesterList.add(requester);
                workingRequesterListModified = true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.IOCDB#removeRequester(org.epics.ioc.util.Requester)
         */
        public void removeRequester(Requester requester) {
            rwLock.writeLock().lock();
            try {
                workingRequesterList.remove(requester);
                workingRequesterListModified = true;
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
