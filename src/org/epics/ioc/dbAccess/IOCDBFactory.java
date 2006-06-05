/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

import java.util.*;
import java.util.regex.*;


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
    public static Collection<IOCDB> getIOCDBList() {
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
        public DBAccess createAccess(String recordName) {
            DBRecord dbRecord = findRecord(recordName);
            if(dbRecord!=null) return new Access(dbRecord);
            return null;
        }
        public boolean createRecord(String recordName,
            DBDRecordType dbdRecordType)
        {
            if(recordMap.containsKey(recordName)) return false;
            DBRecord record = FieldDataFactory.createRecord(
                recordName,dbdRecordType);
            recordMap.put(recordName,record);
            return true;
        }
        public DBD getDBD() {
            return dbd;
        }
        public String getName() {
            return name;
        }
        
        public DBRecord findRecord(String recordName) {
            return recordMap.get(recordName);
        }

        public Map<String,DBRecord> getRecordMap() {
            return recordMap;
        }
        IOCDBInstance(DBD dbd, String name) {
            this.dbd = dbd;
            this.name = name;
        }
        
        private DBD dbd;
        private String name;
        private static Map<String,DBRecord> recordMap;
        static {
            recordMap = new HashMap<String,DBRecord>();
        }
    }

    private static class Access implements DBAccess {
        private DBRecord dbRecord;
        private DBData dbDataSetField;
        static private Pattern periodPattern = Pattern.compile("[.]");
        //following are for setName(String name)
        private String otherRecord = null;
        private String otherField = null;

        
        Access(DBRecord dbRecord) {
            this.dbRecord = dbRecord;
            dbDataSetField = null;
        }
        
        
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
            DBStructure parent = oldField.getParent();
            if(parent==null) throw new IllegalArgumentException("no parent");
            DBData[] fields = parent.getFieldDBDatas();
            for(int i=0; i<fields.length; i++) {
                if(fields[i]==oldField) {
                    fields[i] = newField;
                    return;
                }
            }
            throw new IllegalArgumentException("oldField not found in parent");
        }
        public DBRecord getDbRecord() {
            return dbRecord;
        }
        public DBData getField() {
            return dbDataSetField;
        }
        
        
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
                    Field field = currentData.getField();
                    if(field.getType()!=Type.pvArray) break;
                    Array array = (Array)field;
                    if(array.getElementType()!=Type.pvStructure) break;
                    DBStructureArray dbStructureArray =
                        (DBStructureArray)currentData;
                    if(arrayIndex>=dbStructureArray.getLength()) break;
                    DBStructure[] structureArray = new DBStructure[1];
                    int n = dbStructureArray.get(arrayIndex,1,structureArray,0);
                    if(n<1 || structureArray[0]==null) {
                        currentData = null;
                        break;
                    }
                    currentData = structureArray[0];
                }
                if(currentData==null) break;
                if(names.length<=1) break;
                names = periodPattern.split(names[1],2);
            }
            if(currentData==null) return AccessSetResult.notFound;
            dbDataSetField = currentData;
            return AccessSetResult.thisRecord;
        }
        
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
        
        public String getOtherField() {
            return otherField;
        }


        public String getOtherRecord() {
            return otherRecord;
        }

        
        public DBData getPropertyField(Property property) {
            if(property==null) return null;
            DBData currentData = dbDataSetField;
            if(currentData==null) currentData = dbRecord;
            return findPropertyField(currentData,property);
        }

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
            DBStructure config = dbLink.getConfigStructure();
            DBString pvname = null;
            if(config!=null) for(DBData data: config.getFieldDBDatas()) {
                DBDAttribute attribute = data.getDBDField().getAttribute();
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
                    "has bad property fieldName %s\n",
                    ((Structure)dbRecord.getField()).getStructureName(),
                    dbData.getField().getName(),propertyFieldName);
                return null;
            }
            DBData newField = getField(dbData,names[0]);
            if(newField==dbData) {
                DBRecord dbRecord = dbData.getRecord();
                System.err.printf(
                    "somewhere in recordType %s field %s " +
                    "has recursive property fieldName %s\n",
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
            DBData newData = null;
            if(dbData.getField().getType()==Type.pvStructure) {
                DBStructure structure = (DBStructure)dbData;
                DBData[] dbDatas = structure.getFieldDBDatas();
                int dataIndex = structure.getFieldDBDataIndex(fieldName);
                if(dataIndex>=0) {
                    newData = dbDatas[dataIndex];
                }
            }
            if(newData==null) {
                DBStructure structure = dbData.getParent();
                if(structure!=null) {
                    DBData[]dbDatas = structure.getFieldDBDatas();
                    int dataIndex = structure.getFieldDBDataIndex(fieldName);
                    if(dataIndex>=0) {
                        newData = dbDatas[dataIndex];
                    }
                }
            }
            return newData;
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
