
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

import java.util.*;


public class IOCDBFactory {

    public static IOCDB create(DBD dbd, String name) {
        if(find(name)!=null) return null;
        IOCDB iocdb = new IOCDBInstance(dbd,name);
        iocdbList.addLast(iocdb);
        return iocdb;
    }
    
    public static IOCDB find(String name) {
        ListIterator<IOCDB> iter = iocdbList.listIterator();
        while(iter.hasNext()) {
            IOCDB iocdb = iter.next();
            if(name.equals(iocdb.getName())) return iocdb;
        }
        return null;
    }
    
    public static Collection<IOCDB> getDBDList() {
        return iocdbList;
    }

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
                if(recordName.equals(record.getField().getName())) return record;
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
