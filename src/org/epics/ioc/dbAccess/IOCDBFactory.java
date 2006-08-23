/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Array;
import org.epics.ioc.pvAccess.Type;
import org.epics.ioc.pvAccess.Field;

import java.util.*;
import java.util.regex.*;


/**
 * factory for creating an IOCDB.
 * @author mrk
 *
 */
public class IOCDBFactory {

    /**
     * Create an IOCDB.
     * @param dbd The reflection database.
     * @param name The name for the IOCDB.
     * @return The newly created IOCDB.
     */
    public static IOCDB create(DBD dbd, String name) {
        if(find(name)!=null) return null;
        IOCDB iocdb = new IOCDBInstance(dbd,name);
        iocdbList.addLast(iocdb);
        return iocdb;
    }
    
    /**
     * Find an IOCDB.
     * @param name The IOCDB name.
     * @return The IOCDB.
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
     * Get the complete collection of IOCDBs.
     * @return The collection.
     */
    public static Collection<IOCDB> getIOCDBList() {
        return iocdbList;
    }

    /**
     * Remove an IOCDB from the collection.
     * @param iocdb The iocdb to remove;
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
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#createAccess(java.lang.String)
         */
        public DBAccess createAccess(String recordName) {
            DBRecord dbRecord = findRecord(recordName);
            if(dbRecord!=null) return new Access(dbRecord);
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#createRecord(java.lang.String, org.epics.ioc.dbDefinition.DBDRecordType)
         */
        public boolean createRecord(String recordName,
            DBDRecordType dbdRecordType)
        {
            if(recordMap.containsKey(recordName)) return false;
            DBRecord record = FieldDataFactory.createRecord(
                recordName,dbdRecordType);
            recordMap.put(recordName,record);
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#getDBD()
         */
        public DBD getDBD() {
            return dbd;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#getName()
         */
        public String getName() {
            return name;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#findRecord(java.lang.String)
         */
        public DBRecord findRecord(String recordName) {
            return recordMap.get(recordName);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.IOCDB#getRecordMap()
         */
        public Map<String,DBRecord> getRecordMap() {
            return recordMap;
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
                    DBType elementDBType = dbArray.getElementDBType();
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
                        Type type = dbArray.getElementType();
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
