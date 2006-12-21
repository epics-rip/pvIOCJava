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
         * @see org.epics.ioc.db.IOCDB#createAccess(java.lang.String)
         */
        public DBAccess createAccess(String recordName) {
            DBRecord dbRecord = findRecord(recordName);
            if(dbRecord!=null) return new Access(dbRecord);
            return null;
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
         * @see org.epics.ioc.db.DBAccess#replaceField(org.epics.ioc.pv.PVData, org.epics.ioc.pv.PVData)
         */
        public void replaceField(PVData oldField, PVData newField) {
            if(oldField.getField().getType()!=newField.getField().getType()) {
                throw new IllegalArgumentException(
                    "newField is not same type as oldField");
            }
            if(oldField.getField().getType()
            !=newField.getField().getType()) {
                throw new IllegalArgumentException(
                    "newField is not same DBtype as oldField");
            }
            PVData parent = ((DBData)oldField).getParent();
            if(parent==null) throw new IllegalArgumentException("no parent");
            Type parentType = parent.getField().getType();
            if(parentType==Type.pvStructure) {
                PVData[] fields = ((PVStructure)parent).getFieldPVDatas();
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
         * @see org.epics.ioc.db.DBAccess#getDbRecord()
         */
        public DBRecord getDbRecord() {
            return dbRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBAccess#getField()
         */
        public DBData getField() {
            return dbDataSetField;
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBAccess#setField(java.lang.String)
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
                    Type type = currentData.getField().getType();
                    if(type!=Type.pvArray) break;
                    PVArray pvArray = (PVArray)currentData;
                    Array field = (Array)pvArray.getField();
                    Type elementType = field.getElementType();
                    if(elementType==Type.pvStructure) {
                        PVStructureArray pvStructureArray =
                            (PVStructureArray)currentData;
                        if(arrayIndex>=pvStructureArray.getLength()) break;
                        StructureArrayData data = new StructureArrayData();
                        int n = pvStructureArray.get(arrayIndex,1,data);
                        PVStructure[] structureArray = data.data;
                        int offset = data.offset;
                        if(n<1 || structureArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (DBData)structureArray[offset];
                    } else if(elementType==Type.pvArray) {
                        PVArrayArray pvArrayArray = (PVArrayArray)currentData;
                        if(arrayIndex>=pvArrayArray.getLength()) break;
                        ArrayArrayData data = new ArrayArrayData();
                        int n = pvArrayArray.get(arrayIndex,1,data);
                        PVArray[] arrayArray = data.data;
                        int offset = data.offset;
                        if(n<1 || arrayArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (DBData)arrayArray[offset];
                        break;
                    } else if(elementType==Type.pvLink) {
                        PVLinkArray pvLinkArray = (PVLinkArray)currentData;
                        if(arrayIndex>=pvLinkArray.getLength()) break;
                        LinkArrayData data = new LinkArrayData();
                        int n = pvLinkArray.get(arrayIndex,1,data);
                        PVLink[] linkArray = data.data;
                        int offset = data.offset;
                        if(n<1 || linkArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (DBData)linkArray[offset];
                        break;
                    } else if(elementType==Type.pvMenu) {
                        PVMenuArray pvMenuArray = (PVMenuArray)currentData;
                        if(arrayIndex>=pvMenuArray.getLength()) break;
                        MenuArrayData data = new MenuArrayData();
                        int n = pvMenuArray.get(arrayIndex,1,data);
                        PVMenu[] menuArray = data.data;
                        int offset = data.offset;
                        if(n<1 || menuArray[offset]==null) {
                            currentData = null;
                            break;
                        }
                        currentData = (DBData)menuArray[offset];
                        break;
                    } else if(elementType==Type.pvEnum){
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
                    } else {
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
        
        public void setField(PVData pvData) {
            if(pvData==null) {
                dbDataSetField = dbRecord;
                return;
            }
            if(((DBData)pvData).getRecord()!=dbRecord) 
                throw new IllegalArgumentException (
                    "field is not in this record instance");
            dbDataSetField = (DBData)pvData;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBAccess#getOtherField()
         */
        public String getOtherField() {
            return otherField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBAccess#getOtherRecord()
         */
        public String getOtherRecord() {
            return otherRecord;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBAccess#getPropertyField(org.epics.ioc.pv.Property)
         */
        public DBData getPropertyField(Property property) {
            if(property==null) return null;
            DBData currentData = dbDataSetField;
            if(currentData==null) currentData = dbRecord;
            return findPropertyField(currentData,property);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBAccess#getPropertyField(java.lang.String)
         */
        public DBData getPropertyField(String propertyName) {
            DBData currentData = dbDataSetField;
            if(currentData==null) currentData = dbRecord;
            Property property = currentData.getField().getProperty(propertyName);
            if(property==null) return null;
            return findPropertyField(currentData,property);
        }
        
        private boolean lookForRemote(PVData pvData,String fieldName)
        {
            DBData dbData = (DBData)pvData;
            String[]names = periodPattern.split(fieldName,2);
            while(names.length>0) {
                DBData nextField = getField(dbData,names[0]);
                if(nextField==null) break;
                pvData = nextField;
                if(names.length==1) break;
                names = periodPattern.split(names[1],2);
            }
            if(names.length==0) return false;
            Property property = pvData.getField().getProperty(names[0]);
            if(property==null) return false;
            String[] fieldNames = periodPattern.split(property.getFieldName(),2);
            pvData = findField(dbData,fieldNames[0]);
            if(pvData==null) return false;
            if(pvData.getField().getType()!=Type.pvLink) return false;
            PVLink pvLink = (PVLink)pvData;
            PVStructure config = pvLink.getConfigurationStructure();
            PVString pvname = null;
            if(config!=null) for(PVData pvdata: config.getFieldPVDatas()) {
                FieldAttribute attribute = pvdata.getField().getFieldAttribute();
                if(attribute.isLink()) {
                    if(pvdata.getField().getType()==Type.pvString) {
                        pvname = (PVString)pvdata;
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
            DBRecord dbRecord = dbData.getRecord();
            String propertyFieldName = property.getFieldName();
            if(propertyFieldName.charAt(0)=='/') {
                propertyFieldName = propertyFieldName.substring(1);
                dbData = dbRecord;
            }
            String[] names = periodPattern.split(propertyFieldName,0);
            int length = names.length;
            if(length<1 || length>2) {
                System.err.printf(
                    "somewhere in recordType %s field %s " +
                    "has bad property fieldName %s%n",
                    ((Structure)dbRecord.getField()).getStructureName(),
                    dbData.getField().getFieldName(),propertyFieldName);
                return null;
            }
            DBData newField = getField(dbData,names[0]);
            if(newField==dbData) {
                System.err.printf(
                    "somewhere in recordType %s field %s " +
                    "has recursive property fieldName %s%n",
                    ((Structure)dbRecord.getField()).getStructureName(),
                    dbData.getField().getFieldName(),propertyFieldName);
                return null;
            }
            dbData = newField;
            if(dbData!=null && length==2
            && dbData.getField().getType()!=Type.pvLink) {
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
                PVStructure pvStructure = (PVStructure)dbData;
                PVData[] dbDatas = pvStructure.getFieldPVDatas();
                Structure structure = (Structure)pvStructure.getField();
                int dataIndex = structure.getFieldIndex(fieldName);
                if(dataIndex>=0) {
                    return (DBData)dbDatas[dataIndex];
                }
            }
            DBData parent = (DBData)dbData.getParent();
            if(parent==null) return null;
            Type parentType = parent.getField().getType();
            if(parentType!=Type.pvStructure) return null;
            PVStructure pvStructure = (PVStructure)parent;
            if(pvStructure!=null) {
                PVData[]pvDatas = pvStructure.getFieldPVDatas();
                Structure structure = (Structure)pvStructure.getField();
                int dataIndex = structure.getFieldIndex(fieldName);
                if(dataIndex>=0) {
                    return (DBData)pvDatas[dataIndex];
                }
            }
            return null;
        }
        
        static private Property getProperty(PVData pvData,String name) {
            Property property = null;
            // Look first for field property
            property = pvData.getField().getProperty(name);
            if(property!=null) return property;
            DBData record = ((DBData)pvData).getRecord();
            if(pvData==record) return null;
            // if structure look for structure property
            Field field = pvData.getField();
            Type type = field.getType();
            if(type==Type.pvStructure) {
                Structure structure = (Structure)field;
                DBD dbd = DBDFactory.getMasterDBD();
                DBDStructure dbdStructure = dbd.getStructure(structure.getStructureName());
                property = dbdStructure.getProperty(name);
                if(property!=null) return property;
            }
            // look for parent property
            PVData parent = pvData.getParent();
            if(parent==null) return null;
            property = parent.getField().getProperty(name);
            return property;                
        }
  
    }
}
