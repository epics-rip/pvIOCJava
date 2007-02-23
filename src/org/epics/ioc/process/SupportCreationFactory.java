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
        return new SupportCreationInstance(iocdb,requestor);
    }
    
    static public boolean createSupport(DBData dbData) {
        return createSupportPvt(dbData.getPVData(),dbData);
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
                DBStructure dbStructure = record.getDBStructure();
                if(!createStructureSupport(dbStructure)) result = false;
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
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#initializeSupport()
         */
        public boolean initializeSupport() {
            boolean result = true;
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                PVRecord pvRecord = record.getPVRecord();
                Support support = record.getDBStructure().getSupport();
                RecordProcess process = record.getRecordProcess();
                process.initialize();
                SupportState supportState = support.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    printError(requestor,pvRecord,
                        " state " + supportState.toString()
                        + " but should be readyForStart");
                    result = false;
                }
            }
            return result;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#startSupport()
         */
        public boolean startSupport() {
            boolean result = true;
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                PVRecord pvRecord = record.getPVRecord();
                Support support = record.getDBStructure().getSupport();
                RecordProcess process = record.getRecordProcess();
                process.start();
                SupportState supportState = support.getSupportState();
                if(supportState!=SupportState.ready) {
                    printError(requestor,pvRecord,
                            " state " + supportState.toString()
                            + " but should be ready");
                    result = false;
                }
            }
            return result;
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#stopSupport()
         */
        public void stopSupport() {
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                RecordProcess process = record.getRecordProcess();
                process.stop();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#uninitializeSupport()
         */
        public void uninitializeSupport() {
            Iterator<DBRecord> iter = records.iterator();
            while(iter.hasNext()) {
                DBRecord record = iter.next();
                RecordProcess process = record.getRecordProcess();
                process.uninitialize();
            }
        }
  
        private boolean createRecordSupport(DBRecord dbRecord) {
            DBStructure dbStructure = dbRecord.getDBStructure();
            PVRecord pvRecord = dbRecord.getPVRecord();
            if(dbStructure.getSupport()!=null) return true;
            String supportName = pvRecord.getSupportName();
            if(supportName==null) {
                requestor.message(
                    pvRecord.getRecordName() + " no support found",
                    MessageType.fatalError);
                return false;
            }
            boolean result = SupportCreationFactory.createSupportPvt(requestor,dbStructure);
            if(!result) return result;
            return true;
        }
        
        
        private boolean createStructureSupport(DBData dbData) {
            boolean result = SupportCreationFactory.createSupportPvt(requestor,dbData);
            DBStructure dbStructure = (DBStructure)dbData;
            DBData[] dbDatas = dbStructure.getFieldDBDatas();
            for(DBData data : dbDatas) {
                Type type = data.getPVData().getField().getType();
                if(type==Type.pvStructure) {
                    if(!createStructureSupport(data)) result = false;
                } else if(type==Type.pvArray) {
                    if(!createArraySupport(data)) result = false;
                } else {
                    if(!SupportCreationFactory.createSupportPvt(requestor,data)) result = false;
                }
            }
            return result;
        }
        
        private boolean createArraySupport(DBData dbData) {
            boolean result = true;
            PVArray pvArray = (PVArray)dbData.getPVData();
            if(!SupportCreationFactory.createSupportPvt(requestor,dbData)) result = false;
            Array array = (Array)pvArray.getField();
            Type elementType = array.getElementType();
            if(elementType.isScalar()) return result;
            DBNonScalarArray dbNonScalayArray = (DBNonScalarArray)dbData;
            DBData[] dbDatas = dbNonScalayArray.getElementDBDatas();
            if(elementType==Type.pvStructure) {
                PVStructureArray pvStructureArray = (PVStructureArray)dbData.getPVData();
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
                        if(!createStructureSupport(dbDatas[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==Type.pvLink) {
                PVLinkArray pvLinkArray = (PVLinkArray)dbData.getPVData();
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
                        if(!SupportCreationFactory.createSupportPvt(requestor,dbDatas[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==Type.pvArray) {
                PVArrayArray pvArrayArray = (PVArrayArray)dbData.getPVData();
                int len = pvArrayArray.getLength();
                ArrayArrayData data = new ArrayArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = pvArrayArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    for(int i=0; i<n; i++) {
                        if(dbDatas[i]==null) continue;
                        if(!createArraySupport(dbDatas[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } 
            return result;
        }
    }
    
    private static void printError(Requestor requestor,PVData pvData,String message) {
        String name = pvData.getFullFieldName();
        name = pvData.getPVRecord().getRecordName() + name;
        requestor.message(
                name + " " + message,
                MessageType.error);
    }
    
    private static boolean createSupportPvt(Requestor requestor,DBData dbData) {
        if(dbData.getSupport()!=null) return true;
        String supportName = dbData.getSupportName();
        if(supportName==null) return true;
        String factoryName = null;
        PVData pvData = dbData.getPVData();
        DBD dbd = DBDFactory.getMasterDBD();
        if(pvData.getField().getType()==Type.pvLink) {
            DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(supportName);
            if(dbdLinkSupport==null) {
                printError(requestor,pvData,"linkSupport " + supportName + " does not exist");
                return false;
            }
            factoryName = dbdLinkSupport.getFactoryName();
            if(factoryName==null) {
                printError(requestor,pvData,"linkSupport " + supportName + " does not define a factory name");
                return false;
            }
        } else {
            DBDSupport dbdSupport = dbd.getSupport(supportName);
            if(dbdSupport==null) {
                printError(requestor,pvData,"support " + supportName + " does not exist");
                return false;
            }
            factoryName = dbdSupport.getFactoryName();
            if(factoryName==null) {
                printError(requestor,pvData,"support " + supportName + " does not define a factory name");
                return false;
            }
        }
        Class supportClass;
        Support support = null;
        Method method = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
            printError(requestor,pvData,
                    "support " + supportName 
                    + " factory " + e.getLocalizedMessage()
                    + " class not found");
            return false;
        }
        String data = null;
        Type type = pvData.getField().getType();
        if (type==Type.pvLink) {
            data = "DBLink";
        } else if (type==Type.pvStructure) {
            data = "DBStructure";
        } else {
            data = "DBData";
        }
        data = "org.epics.ioc.db." + data;
        try {
            method = supportClass.getDeclaredMethod("create",
                    Class.forName(data));    
        } catch (NoSuchMethodException e) {
            printError(requestor,pvData,
                    "support "
                    + supportName
                    + " no factory method "
                    + e.getLocalizedMessage());
            return false;
        } catch (ClassNotFoundException e) {
            printError(requestor,pvData,
                    "support "
                    + factoryName
                    + " arg class "
                    + e.getLocalizedMessage());
            return false;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            printError(requestor,pvData,
                    "support "
                    + factoryName
                    + " create is not a static method ");
            return false;
        }
        try {
            support = (Support)method.invoke(null,dbData);
        } catch(IllegalAccessException e) {
            printError(requestor,pvData,
                    "support "
                    + supportName
                    + " create IllegalAccessException "
                    + e.getLocalizedMessage());
            return false;
        } catch(IllegalArgumentException e) {
            printError(requestor,pvData,
                    "support "
                    + supportName
                    + " create IllegalArgumentException "
                    + e.getLocalizedMessage());
            return false;
        } catch(InvocationTargetException e) {
            printError(requestor,pvData,
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
