/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import java.lang.reflect.*;
import java.util.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Type;
import org.epics.ioc.pv.Array;
import org.epics.ioc.db.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.util.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class SupportCreationFactory {
    /**
     * create a process database.
     * @param iocdb the iocdb associated with the record processing.
     * @return the SupportCreation.
     */
    static public SupportCreation createSupportCreation(IOCDB iocdb,Requestor requestor) {
        SupportCreationInstance processDB = new SupportCreationInstance(iocdb,requestor);
        return processDB;
    }
    
    static private class SupportCreationInstance implements SupportCreation{
        private IOCDB iocdb;
        private Requestor requestor;
        private Collection<DBRecord> records;
        
        private SupportCreationInstance(IOCDB iocdbin,Requestor requestor) {
            iocdb = iocdbin;
            this.requestor = requestor;
            records = iocdb.getRecordMap().values();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#getIOCDB()
         */
        public IOCDB getIOCDB() {
            return iocdb;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#createSupport()
         */
        public boolean createSupport() {
            boolean result = true;
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                if(!createStructureSupport(record)) result = false;
                if(!createRecordSupport(record)) result = false;
            }
            if(!result) return result;
            iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                if(record.getRecordProcess()!=null) continue;
                RecordProcess recordProcess =
                    RecordProcessFactory.createRecordProcess(record);
                record.setRecordProcess(recordProcess);
            }
            return result;
        }
        
        public boolean initializeSupport() {
            boolean result = true;
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                Support support = record.getSupport();
                RecordProcess process = record.getRecordProcess();
                process.initialize();
                SupportState supportState = support.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    printError(record,
                        " state " + supportState.toString()
                        + " but should be readyForStart");
                    result = false;
                }
            }
            return result;
        }
        
        public boolean startSupport() {
            boolean result = true;
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                Support support = record.getSupport();
                RecordProcess process = record.getRecordProcess();
                process.start();
                SupportState supportState = support.getSupportState();
                if(supportState!=SupportState.ready) {
                    printError(record,
                            " state " + supportState.toString()
                            + " but should be ready");
                    result = false;
                }
            }
            return result;
            
        }
        public void stopSupport() {
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                RecordProcess process = record.getRecordProcess();
                process.stop();
            }
        }
        public void uninitializeSupport() {
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                RecordProcess process = record.getRecordProcess();
                process.uninitialize();
            }
        }
        
        private boolean createRecordSupport(DBRecord dbRecord) {
            if(dbRecord.getSupport()!=null) return true;
            String supportName = dbRecord.getSupportName();
            if(supportName==null) {
                requestor.message(
                    dbRecord.getRecordName() + " no support found",
                    MessageType.fatalError);
                return false;
            }
            boolean result = createSupport(dbRecord);
            if(!result) return result;
            return true;
        }
        
        
        private boolean createStructureSupport(PVStructure pvStructure) {
            boolean result = createSupport((DBData)pvStructure);
            PVData[] pvDatas = pvStructure.getFieldPVDatas();
            for(PVData pvData : pvDatas) {
                Type type = pvData.getField().getType();
                if(type==Type.pvStructure) {
                    if(!createStructureSupport((PVStructure)pvData)) result = false;
                } else if(type==Type.pvArray) {
                    if(!createArraySupport((PVArray)pvData)) result = false;
                } else {
                    if(!createSupport(pvData)) result = false;
                }
            }
            return result;
        }
        
        private boolean createArraySupport(PVArray pvArray) {
            boolean result = true;
            if(!createSupport((DBData)pvArray)) result = false;
            Array array = (Array)pvArray.getField();
            Type elementType = array.getElementType();
            if(elementType==Type.pvStructure) {
                PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
                int len = pvStructureArray.getLength();
                StructureArrayData data = new StructureArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = pvStructureArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    PVStructure[] pvStructures = data.data;
                    for(int i=0; i<n; i++) {
                        if(pvStructures[i]==null) continue;
                        if(!createStructureSupport(pvStructures[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==Type.pvLink) {
                PVLinkArray pvLinkArray = (PVLinkArray)pvArray;
                int len = pvLinkArray.getLength();
                LinkArrayData data = new LinkArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = pvLinkArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    PVLink[] pvLink = data.data;
                    for(int i=0; i<n; i++) {
                        if(pvLink[i]==null) continue;
                        if(!createSupport((DBData)pvLink[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==Type.pvArray) {
                PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
                int len = pvArrayArray.getLength();
                ArrayArrayData data = new ArrayArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = pvArrayArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    PVArray[] db = data.data;
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
            name = dbData.getRecord().getRecordName() + name;
            requestor.message(
                    name + " " + message,
                    MessageType.error);
        }
        
        private boolean createSupport(PVData pvData) {
            DBData dbData = (DBData)pvData;
            if(dbData.getSupport()!=null) return true;
            String supportName = dbData.getSupportName();
            if(supportName==null) return true;
            DBD dbd = DBDFactory.getMasterDBD();
            DBDSupport dbdSupport = dbd.getSupport(supportName);
            if(dbdSupport==null) {
                dbdSupport = dbd.getLinkSupport(supportName);
            }
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
            Type type = dbData.getField().getType();
            if(type==Type.pvArray) {
                data = "PVArray";
            } else if (type==Type.pvLink) {
                data = "PVLink";
            } else if (type==Type.pvStructure) {
                data = "PVStructure";
            } else {
                data = "PVData";
            }
            data = "org.epics.ioc.pv." + data;
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
