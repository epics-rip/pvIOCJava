/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.regex.*;


/**
 * Factory for creating an IOCDB.
 * @author mrk
 *
 */
public class IOCDBFactory {
    private static TreeMap<String,IOCDB> iocdbMap = new TreeMap<String,IOCDB>();
    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    /**
     * Create an IOCDB.
     * @param dbd The reflection database.
     * @param name The name for the IOCDB.
     * @param masterIOCDB The masterIOCDB or null if no master or this is the master.
     * @return The newly created IOCDB or null if an IOCDB with this name already exists.
     */
    public static IOCDB create(DBD dbd, String name, IOCDB masterIOCDB) {
        rwLock.writeLock().lock();
        try {
            if(iocdbMap.get(name)!=null) return null;
            IOCDB iocdb = new IOCDBInstance(dbd,name,masterIOCDB);
            iocdbMap.put(name,iocdb);
            return iocdb;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    /**
     * Remove the IOCDB from the list.
     * @param iocdb The IOCDB to remove.
     * @return (false,true) if the iocdb (was not,was) removed.
     */
    public static boolean remove(IOCDB iocdb) {
        rwLock.writeLock().lock();
        try {
            if(iocdbMap.remove(iocdb.getName())!=null) return true;
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    /**
     * Find an IOCDB.
     * @param name The IOCDB name.
     * @return The IOCDB.
     */
    public static IOCDB find(String name) {
        rwLock.readLock().lock();
        try {
            return iocdbMap.get(name);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * Get a map of the IOCDBs.
     * @return the Collection.
     */
    public static Map<String,IOCDB> getIOCDBMap() {
        rwLock.readLock().lock();
        try {
            return (Map<String,IOCDB>)iocdbMap.clone();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    /**
     * List each IOCDB that has a name that matches the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the list.
     */
    public static String list(String regularExpression) {
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
            Set<String> keys = iocdbMap.keySet();
            for(String key: keys) {
                IOCDB iocdb = iocdbMap.get(key);
                String name = iocdb.getName();
                if(pattern.matcher(name).matches()) {
                    result.append(" " + name);
                }
            }
            return result.toString();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    private static class IOCDBInstance implements IOCDB
    {
        private DBD dbd;
        private String name;
        private IOCDBInstance masterIOCDB;
        private LinkedHashMap<String,DBRecord> recordMap = new LinkedHashMap<String,DBRecord>();
        private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        
        private IOCDBInstance(DBD dbd, String name, IOCDB masterIOCDB) {
            this.dbd = dbd;
            this.name = name;
            this.masterIOCDB = (IOCDBInstance)masterIOCDB;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#getName()
         */
        public String getName() {
            return name;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#getDBD()
         */
        public DBD getDBD() {
            return dbd;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#getMasterIOCDB()
         */
        public IOCDB getMasterIOCDB() {
            return masterIOCDB;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#mergeIntoMaster()
         */
        public void mergeIntoMaster() {
            if(masterIOCDB==null) return;
            rwLock.readLock().lock();
            try {
                masterIOCDB.merge(recordMap);
            } finally {
                rwLock.readLock().unlock();
            }
        }
        // merge allows master to be locked once instead of for each record instance
        private void merge(LinkedHashMap<String,DBRecord> from) {
            Set<String> keys;
            rwLock.writeLock().lock();
            try {
                keys = from.keySet();
                for(String key: keys) {
                    recordMap.put(key,from.get(key));
                }
            }  finally {
                rwLock.writeLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#findRecord(java.lang.String)
         */
        public DBRecord findRecord(String recordName) {
            rwLock.readLock().lock();
            try {
                DBRecord record = null;
                record = recordMap.get(recordName);
                if(record==null && masterIOCDB!=null) record = masterIOCDB.findRecord(recordName);
                return record;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#addRecord(org.epics.ioc.dbAccess.DBRecord)
         */
        public boolean addRecord(DBRecord record) {
            rwLock.writeLock().lock();
            try {
                String key = record.getRecordName();
                if(recordMap.containsKey(key)) return false;
                if(masterIOCDB!=null && masterIOCDB.findRecord(key)!=null) return false;
                recordMap.put(key,record);
                return true;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
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
         * @see org.epics.ioc.dbAccess.IOCDB#getRecordMap()
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
         * @see org.epics.ioc.dbAccess.IOCDB#createAccess(java.lang.String)
         */
        public DBAccess createAccess(String recordName) {
            DBRecord dbRecord = findRecord(recordName);
            if(dbRecord!=null) return new Access(dbRecord);
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#recordList(java.lang.String)
         */
        public String recordList(String regularExpression) {
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
                Set<String> keys = recordMap.keySet();
                for(String key: keys) {
                    DBRecord record = recordMap.get(key);
                    String name = record.getRecordName();
                    if(pattern.matcher(name).matches()) {
                        result.append(" " + name);
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#recordToString(java.lang.String)
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
                Set<String> keys = recordMap.keySet();
                for(String key: keys) {
                    DBRecord record = recordMap.get(key);
                    String name = record.getRecordName();
                    if(pattern.matcher(name).matches()) {
                        result.append(String.format("%nrecord %s%s",name,record.toString()));
                    }
                }
                return result.toString();
            } finally {
                rwLock.readLock().unlock();
            }
        }
    }

    private static class Access implements DBAccess {
        private DBRecord dbRecord;
        private DBData dbDataSetField;
        static private Pattern periodPattern = Pattern.compile("[.]");
        //following are for setName(String name)
        private String otherRecord = null;
        private String otherField = null;

        
        private Access(DBRecord dbRecord) {
            this.dbRecord = dbRecord;
            dbDataSetField = null;
        }
              
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#replaceField(org.epics.ioc.dbAccess.DBData, org.epics.ioc.dbAccess.DBData)
         */
        public void replaceField(DBData oldField, DBData newField) {
            if(oldField.getField().getType()!=newField.getField().getType()) {
                throw new IllegalArgumentException(
                    "newField is not same type as oldField");
            }
            if(oldField.getDBDField().getDBType()
            !=newField.getDBDField().getDBType()) {
                throw new IllegalArgumentException(
                    "newField is not same DBtype as oldField");
            }
            DBData parent = oldField.getParent();
            if(parent==null) throw new IllegalArgumentException("no parent");
            DBType parentType = parent.getDBDField().getDBType();
            if(parentType==DBType.dbStructure) {
                DBData[] fields = ((DBStructure)parent).getFieldDBDatas();
                for(int i=0; i<fields.length; i++) {
                    if(fields[i]==oldField) {
                        fields[i] = newField;
                        return;
                    }
                }
            }
            throw new IllegalArgumentException("oldField not found in parent");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#getDbRecord()
         */
        public DBRecord getDbRecord() {
            return dbRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#getField()
         */
        public DBData getField() {
            return dbDataSetField;
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#setField(java.lang.String)
         */
        public AccessSetResult setField(String fieldName) {
            if(fieldName==null || fieldName.length()==0) {
                dbDataSetField = dbRecord;
                return AccessSetResult.thisRecord;
            }
            String[] names = periodPattern.split(fieldName,2);
            if(names.length<=0) {
                return AccessSetResult.notFound;
            }
            DBData currentData = dbDataSetField;
            if(currentData==null) currentData = dbRecord;
            if(lookForRemote(currentData,fieldName)) return AccessSetResult.otherRecord;
            while(true) {
                String name = names[0];
                int arrayIndex = -1;
                int startBracket = name.indexOf('[');
                if(startBracket>=0) {
                    String arrayIndexString = name.substring(startBracket+1);
                    name = name.substring(0,startBracket);
                    int endBracket = arrayIndexString.indexOf(']');
                    if(endBracket<0) break;
                    arrayIndexString = arrayIndexString.substring(0,endBracket);
                    arrayIndex = Integer.parseInt(arrayIndexString);
                }
                DBData newData = findField(currentData,name);
                currentData = newData;
                if(currentData==null) break;
                if(arrayIndex>=0) {
                    DBType dbType = currentData.getDBDField().getDBType();
                    if(dbType!=DBType.dbArray) break;
                    DBArray dbArray = (DBArray)currentData;
                    DBDArrayField dbdArrayField = (DBDArrayField)dbArray.getDBDField();
                    DBType elementDBType = dbdArrayField.getElementDBType();
                    if(elementDBType==DBType.dbStructure) {
                        DBStructureArray dbStructureArray =
                            (DBStructureArray)currentData;
                        if(arrayIndex>=dbStructureArray.getLength()) break;
                        DBStructureArrayData data = new DBStructureArrayData();
                        int n = dbStructureArray.get(arrayIndex,1,data);
                        DBStructure[] structureArray = data.data;
                        int offset = data.offset;
                        if(n<1 || structureArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = structureArray[offset];
                    } else if(elementDBType==DBType.dbArray) {
                        DBArrayArray dbArrayArray = (DBArrayArray)currentData;
                        if(arrayIndex>=dbArrayArray.getLength()) break;
                        DBArrayArrayData data = new DBArrayArrayData();
                        int n = dbArrayArray.get(arrayIndex,1,data);
                        DBArray[] arrayArray = data.data;
                        int offset = data.offset;
                        if(n<1 || arrayArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = arrayArray[offset];
                        break;
                    } else if(elementDBType==DBType.dbLink) {
                        DBLinkArray dbLinkArray = (DBLinkArray)currentData;
                        if(arrayIndex>=dbLinkArray.getLength()) break;
                        LinkArrayData data = new LinkArrayData();
                        int n = dbLinkArray.get(arrayIndex,1,data);
                        DBLink[] linkArray = data.data;
                        int offset = data.offset;
                        if(n<1 || linkArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = linkArray[offset];
                        break;
                    } else if(elementDBType==DBType.dbMenu) {
                        DBMenuArray dbMenuArray = (DBMenuArray)currentData;
                        if(arrayIndex>=dbMenuArray.getLength()) break;
                        MenuArrayData data = new MenuArrayData();
                        int n = dbMenuArray.get(arrayIndex,1,data);
                        DBMenu[] menuArray = data.data;
                        int offset = data.offset;
                        if(n<1 || menuArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = menuArray[offset];
                        break;
                    } else {
                        Array array = (Array)dbArray.getField();
                        Type type = array.getElementType();
                        if(type==Type.pvEnum) {
                            PVEnumArray pvEnumArray = (PVEnumArray)currentData;
                            if(arrayIndex>=pvEnumArray.getLength()) break;
                            EnumArrayData data = new EnumArrayData();
                            int n = pvEnumArray.get(arrayIndex,1,data);
                            PVEnum[] enumArray = data.data;
                            int offset = data.offset;
                            if(n<1 || enumArray[offset]==null) {
                                currentData = null;
                                break;
                            }
                            currentData = (DBData)enumArray[offset];
                            break;
                        }
                        currentData = null;
                        break;
                    }
                }
                if(currentData==null) break;
                if(names.length<=1) break;
                names = periodPattern.split(names[1],2);
            }
            if(currentData==null) return AccessSetResult.notFound;
            dbDataSetField = currentData;
            return AccessSetResult.thisRecord;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#setField(org.epics.ioc.dbAccess.DBData)
         */
        public void setField(DBData dbData) {
            if(dbData==null) {
                dbDataSetField = dbRecord;
                return;
            }
            if(dbData.getRecord()!=dbRecord) 
                throw new IllegalArgumentException (
                    "field is not in this record instance");
            dbDataSetField = dbData;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#getOtherField()
         */
        public String getOtherField() {
            return otherField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#getOtherRecord()
         */
        public String getOtherRecord() {
            return otherRecord;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#getPropertyField(org.epics.ioc.pvAccess.Property)
         */
        public DBData getPropertyField(Property property) {
            if(property==null) return null;
            DBData currentData = dbDataSetField;
            if(currentData==null) currentData = dbRecord;
            return findPropertyField(currentData,property);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBAccess#getPropertyField(java.lang.String)
         */
        public DBData getPropertyField(String propertyName) {
            DBData currentData = dbDataSetField;
            if(currentData==null) currentData = dbRecord;
            Property property = currentData.getField().getProperty(propertyName);
            if(property==null) return null;
            return findPropertyField(currentData,property);
        }
        
        private boolean lookForRemote(DBData dbData,String fieldName)
        {
            String[]names = periodPattern.split(fieldName,2);
            while(names.length>0) {
                DBData nextField = getField(dbData,names[0]);
                if(nextField==null) break;
                dbData = nextField;
                if(names.length==1) break;
                names = periodPattern.split(names[1],2);
            }
            if(names.length==0) return false;
            Property property = dbData.getField().getProperty(names[0]);
            if(property==null) return false;
            String[] fieldNames = periodPattern.split(property.getFieldName(),2);
            dbData = findField(dbData,fieldNames[0]);
            if(dbData==null) return false;
            if(dbData.getDBDField().getDBType()!=DBType.dbLink) return false;
            DBLink dbLink = (DBLink)dbData;
            PVStructure config = dbLink.getConfigurationStructure();
            DBString pvname = null;
            if(config!=null) for(PVData pvdata: config.getFieldPVDatas()) {
                DBData data = (DBData)pvdata;
                DBDFieldAttribute attribute = data.getDBDField().getFieldAttribute();
                if(attribute.isLink()) {
                    if(data.getField().getType()==Type.pvString) {
                        pvname = (DBString)data;
                        break;
                    }
                }
            }
            if(pvname==null) return false;
            String[] subFields = periodPattern.split(pvname.get(),2);
            otherRecord = subFields[0];
            if(fieldNames.length>1) {
                otherField = fieldNames[1];
            } else {
                otherField = null;
            }
            if(names.length>1) otherField += "." + names[1];
            return true;
        }
        
        static private DBData findField(DBData dbData,String name) {
            DBData newField = getField(dbData,name);
            if(newField!=null) return newField;
            Property property = getProperty(dbData,name);
            return findPropertyField(dbData,property);
            
        }
        
        static private DBData  findPropertyField(DBData dbData,
            Property property)
        {
            if(property==null) return null;
            String propertyFieldName = property.getFieldName();
            if(propertyFieldName.charAt(0)=='/') {
                propertyFieldName = propertyFieldName.substring(1);
                dbData = dbData.getRecord();
            }
            String[] names = periodPattern.split(propertyFieldName,0);
            int length = names.length;
            if(length<1 || length>2) {
                DBRecord dbRecord = dbData.getRecord();
                System.err.printf(
                    "somewhere in recordType %s field %s " +
                    "has bad property fieldName %s%n",
                    ((Structure)dbRecord.getField()).getStructureName(),
                    dbData.getField().getName(),propertyFieldName);
                return null;
            }
            DBData newField = getField(dbData,names[0]);
            if(newField==dbData) {
                DBRecord dbRecord = dbData.getRecord();
                System.err.printf(
                    "somewhere in recordType %s field %s " +
                    "has recursive property fieldName %s%n",
                    ((Structure)dbRecord.getField()).getStructureName(),
                    dbData.getField().getName(),propertyFieldName);
                return null;
            }
            dbData = newField;
            if(dbData!=null && length==2
            && dbData.getDBDField().getDBType()!=DBType.dbLink) {
                newField = getField(dbData,names[1]);
                if(newField!=null) {
                    dbData = newField;
                } else {
                    property = getProperty(dbData,names[1]);
                    if(property!=null) {
                        dbData = findPropertyField(dbData,property);
                    }
                }
            }
            return dbData;            
        }
        
        static private DBData getField(DBData dbData, String fieldName) {
            if(dbData.getField().getType()==Type.pvStructure) {
                DBStructure structure = (DBStructure)dbData;
                DBData[] dbDatas = structure.getFieldDBDatas();
                int dataIndex = structure.getFieldDBDataIndex(fieldName);
                if(dataIndex>=0) {
                    return dbDatas[dataIndex];
                }
            }
            DBData parent = dbData.getParent();
            if(parent==null) return null;
            DBType parentType = parent.getDBDField().getDBType();
            if(parentType!=DBType.dbStructure) return null;
            DBStructure structure = (DBStructure)parent;
            if(structure!=null) {
                DBData[]dbDatas = structure.getFieldDBDatas();
                int dataIndex = structure.getFieldDBDataIndex(fieldName);
                if(dataIndex>=0) {
                    return dbDatas[dataIndex];
                }
            }
            return null;
        }
        static private Property getProperty(DBData dbData,String name) {
            Property property = null;
            // Look first for field property
            property = dbData.getField().getProperty(name);
            if(property!=null) return property;
            // if structure look for structure property
            DBDField dbdField = dbData.getDBDField();
            DBType dbType = dbdField.getDBType();
            if(dbType==DBType.dbStructure) {
                DBDStructureField structureField = (DBDStructureField)dbdField;
                DBDStructure structure = structureField.getDBDStructure();
                property = structure.getProperty(name);
                if(property!=null) return property;
            }
            // look for parent property
            DBData parent = dbData.getParent();
            if(parent==null) return null;
            property = parent.getField().getProperty(name);
            return property;                
        }
  
    }
}
