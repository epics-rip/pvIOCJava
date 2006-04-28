
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;

import java.util.*;


/**
 * factory for creating an IOCDB.
 * @author mrk
 *
 */
public class IOCDBFactory {

    /**
     * create an IOCDB.
     * @param dbd the reflection database.
     * @param name the name for the IOCDB.
     * @return the newly created IOCDB.
     */
    public static IOCDB create(DBD dbd, String name) {
        if(find(name)!=null) return null;
        IOCDB iocdb = new IOCDBInstance(dbd,name);
        iocdbList.addLast(iocdb);
        return iocdb;
    }
    
    /**
     * find an IOCDB.
     * @param name the IOCDB name.
     * @return the IOCDB.
     */
    public static IOCDB find(String name) {
        ListIterator<IOCDB> iter = iocdbList.listIterator();
        while(iter.hasNext()) {
            IOCDB iocdb = iter.next();
            if(name.equals(iocdb.getName())) return iocdb;
        }
        return null;
    }
    
    /**
     * get the complete collection of IOCDBs.
     * @return the collection.
     */
    public static Collection<IOCDB> getDBDList() {
        return iocdbList;
    }

    /**
     * remove an IOCDB from the collection.
     * @param iocdb
     */
    public static void remove(IOCDB iocdb) {
        iocdbList.remove(iocdb);
    }
    
    private static LinkedList<IOCDB> iocdbList;
    static {
        iocdbList = new LinkedList<IOCDB>();
    }

    private static class IOCDBInstance implements IOCDB
    {
        public boolean createRecord(String recordName, DBDRecordType dbdRecordType) {
            DBRecord record = FieldDataFactory.createRecord(recordName,dbdRecordType);
            recordList.add(record);
            return true;
        }
        public DBD getDBD() {
            return dbd;
        }
        public String getName() {
            return name;
        }
        
        public DBRecord getRecord(String recordName) {
            ListIterator<DBRecord> iter = recordList.listIterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                if(recordName.equals(record.getRecordName())) return record;
            }
            return null;
        }
        public Collection<DBRecord> getRecordList() {
            return recordList;
        }
        IOCDBInstance(DBD dbd, String name) {
            this.dbd = dbd;
            this.name = name;
        }
        
        private DBD dbd;
        private String name;
        private static LinkedList<DBRecord> recordList;
        static {
            recordList = new LinkedList<DBRecord>();
        }
    }
}
