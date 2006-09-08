/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.lang.reflect.*;
import java.util.*;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class ProcessDBFactory {
    private static IOCDB iocdb;
    /**
     * create a process database.
     * @param iocdb the iocdb associated with the record processing.
     * @return the ProcessDB.
     */
    static public ProcessDB createProcessDB(IOCDB iocdb) {
        return new ProcessDatabase(iocdb);
    }
    
    static private class ProcessDatabase implements ProcessDB{
        private static Map<String,RecordProcess> recordProcessMap;
        static {
            recordProcessMap = new HashMap<String,RecordProcess>();
        }
        
        private ProcessDatabase(IOCDB iocdbin) {
            iocdb = iocdbin;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#getIOCDB()
         */
        public IOCDB getIOCDB() {
            return iocdb;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#findRecordProcess(java.lang.String)
         */
        public RecordProcess getRecordProcess(String recordName) {
            return recordProcessMap.get(recordName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#createRecordProcess(java.lang.String)
         */
        public boolean createRecordProcess(String recordName) {
            if(recordProcessMap.containsKey(recordName)) return false;
            DBRecord dbRecord = iocdb.findRecord(recordName);
            RecordProcess recordProcess =
                RecordProcessFactory.createRecordProcess(this,dbRecord);
            if(recordProcessMap.put(recordName,recordProcess)==null) {
                dbRecord.setRecordProcess(recordProcess);
                return true;
            }
            System.out.printf("ProcessDB.createRecordProcess failure. Why??\n");
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#removeRecordProcess(java.lang.String)
         */
        public void removeRecordProcess(String recordName) {
            recordProcessMap.remove(recordName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#createSupport(java.lang.String)
         */
        public boolean createSupport(String recordName) {
            boolean result = true;
            DBRecord record = iocdb.findRecord(recordName);
            if(record==null) {
                System.err.printf("%s not found%n",recordName);
                return false;
            }
            if(!createRecordSupport(record)) result = false;
            if(!createStructureSupport(record)) result = false;
            if(!recordProcessMap.containsKey(recordName)) {
                if(!createRecordProcess(recordName)) {
                    return false;
                }
            }
            return result;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#createSupport()
         */
        public boolean createSupport() {
            boolean result = true;
            Map<String,DBRecord> recordMap = iocdb.getRecordMap();
            Set<String> keys = recordMap.keySet();
            for(String key: keys) {
                if(!createSupport(key)) result = false;
            }
            return result;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessDB#getRecordProcessMap()
         */
        public Map<String, RecordProcess> getRecordProcessMap() {
            return recordProcessMap;
        }
        
        private boolean createRecordSupport(DBRecord dbRecord) {
            if(dbRecord.getSupport()!=null) return true;
            String supportName = dbRecord.getSupportName();
            if(supportName==null) {
                System.err.printf("%s no support found\n",
                        dbRecord.getRecordName());
                return false;
            }
            boolean result = createSupport(dbRecord);
            if(!result) return result;
            return true;
        }
        
        
        private boolean createStructureSupport(DBStructure dbStructure) {
            boolean result = createSupport(dbStructure);
            DBData[] dbDatas = dbStructure.getFieldDBDatas();
            for(DBData dbData : dbDatas) {
                DBType dbType = dbData.getDBDField().getDBType();
                if(dbType==DBType.dbStructure) {
                    if(!createStructureSupport((DBStructure)dbData)) result = false;
                } else if(dbType==DBType.dbArray) {
                    if(!createArraySupport((DBArray)dbData)) result = false;
                } else {
                    if(!createSupport(dbData)) result = false;
                }
            }
            return result;
        }
        
        private boolean createArraySupport(DBArray dbArray) {
            boolean result = true;
            if(!createSupport(dbArray)) result = false;
            DBDArrayField dbdArrayField = (DBDArrayField)dbArray.getDBDField();
            DBType elementType = dbdArrayField.getElementDBType();
            if(elementType==DBType.dbStructure) {
                DBStructureArray dbStructureArray = (DBStructureArray)dbArray;
                int len = dbStructureArray.getLength();
                DBStructureArrayData data = new DBStructureArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = dbStructureArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    DBStructure[] dbStructures = data.data;
                    for(int i=0; i<n; i++) {
                        if(dbStructures[i]==null) continue;
                        if(!createStructureSupport(dbStructures[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==DBType.dbLink) {
                DBLinkArray dbLinkArray = (DBLinkArray)dbArray;
                int len = dbLinkArray.getLength();
                LinkArrayData data = new LinkArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = dbLinkArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    DBLink[] dbLink = data.data;
                    for(int i=0; i<n; i++) {
                        if(dbLink[i]==null) continue;
                        if(!createSupport(dbLink[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==DBType.dbArray) {
                DBArrayArray dbArrayArray = (DBArrayArray)dbArray;
                int len = dbArrayArray.getLength();
                DBArrayArrayData data = new DBArrayArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = dbArrayArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    DBArray[] db = data.data;
                    for(int i=0; i<n; i++) {
                        if(db[i]==null) continue;
                        if(!createArraySupport(db[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } 
            return result;
        }
        
        private void printError(DBData dbData,String message) {
            String name = dbData.getFullFieldName();
            name = dbData.getRecord().getRecordName() + "." + name;
            System.err.printf("%s %s\n",name,message);
        }
        
        private boolean createSupport(DBData dbData) {
            if(dbData.getSupport()!=null) return true;
            String supportName = dbData.getSupportName();
            if(supportName==null) return true;
            DBDSupport dbdSupport = iocdb.getDBD().getSupport(supportName);
            if(dbdSupport==null) {
                printError(dbData,"support " + supportName + " does not exist");
                return false;
            }
            String factoryName = dbdSupport.getFactoryName();
            if(factoryName==null) {
                printError(dbData,"support " + supportName + " does not define a factory name");
                return false;
            }
            Class supportClass;
            Support support = null;
            Method method = null;
            try {
                supportClass = Class.forName(factoryName);
            }catch (ClassNotFoundException e) {
                printError(dbData,
                        "support " + supportName 
                        + " factory " + e.getLocalizedMessage()
                        + " class not found");
                return false;
            }
            String data = null;
            DBType dbType = dbData.getDBDField().getDBType();
            if(dbType==DBType.dbArray) {
                data = "DBArray";
            } else if (dbType==DBType.dbLink) {
                data = "DBLink";
            } else if (dbType==DBType.dbStructure) {
                data = "DBStructure";
            } else {
                data = "DBData";
            }
            data = "org.epics.ioc.dbAccess." + data;
            try {
                method = supportClass.getDeclaredMethod("create",
                        Class.forName(data));    
            } catch (NoSuchMethodException e) {
                printError(dbData,
                        "support "
                        + supportName
                        + " no factory method "
                        + e.getLocalizedMessage());
                return false;
            } catch (ClassNotFoundException e) {
                printError(dbData,
                        "support "
                        + factoryName
                        + " arg class "
                        + e.getLocalizedMessage());
                return false;
            }
            if(!Modifier.isStatic(method.getModifiers())) {
                printError(dbData,
                        "support "
                        + factoryName
                        + " create is not a static method ");
                return false;
            }
            try {
                support = (Support)method.invoke(null,dbData);
            } catch(IllegalAccessException e) {
                printError(dbData,
                        "support "
                        + supportName
                        + " create IllegalAccessException "
                        + e.getLocalizedMessage());
                return false;
            } catch(IllegalArgumentException e) {
                printError(dbData,
                        "support "
                        + supportName
                        + " create IllegalArgumentException "
                        + e.getLocalizedMessage());
                return false;
            } catch(InvocationTargetException e) {
                printError(dbData,
                        "support "
                        + supportName
                        + " create InvocationTargetException "
                        + e.getLocalizedMessage());
                return false;
            }
            dbData.setSupport(support);
            return true;
        }
    }
    
}
