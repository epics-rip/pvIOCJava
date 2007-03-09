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
    
    static public boolean createSupport(DBField dbField) {
        return createSupportPvt(dbField.getPVField(),dbField);
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
        
        
        private boolean createStructureSupport(DBField dbField) {
            boolean result = SupportCreationFactory.createSupportPvt(requestor,dbField);
            DBStructure dbStructure = (DBStructure)dbField;
            DBField[] dbFields = dbStructure.getFieldDBFields();
            for(DBField field : dbFields) {
                Type type = field.getPVField().getField().getType();
                if(type==Type.pvStructure) {
                    if(!createStructureSupport(field)) result = false;
                } else if(type==Type.pvArray) {
                    if(!createArraySupport(field)) result = false;
                } else {
                    if(!SupportCreationFactory.createSupportPvt(requestor,field)) result = false;
                }
            }
            return result;
        }
        
        private boolean createArraySupport(DBField dbField) {
            boolean result = true;
            PVArray pvArray = (PVArray)dbField.getPVField();
            if(!SupportCreationFactory.createSupportPvt(requestor,dbField)) result = false;
            Array array = (Array)pvArray.getField();
            Type elementType = array.getElementType();
            if(elementType.isScalar()) return result;
            DBNonScalarArray dbNonScalayArray = (DBNonScalarArray)dbField;
            DBField[] dbFields = dbNonScalayArray.getElementDBFields();
            if(elementType==Type.pvStructure) {
                PVStructureArray pvStructureArray = (PVStructureArray)dbField.getPVField();
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
                        if(!createStructureSupport(dbFields[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==Type.pvLink) {
                PVLinkArray pvLinkArray = (PVLinkArray)dbField.getPVField();
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
                        if(!SupportCreationFactory.createSupportPvt(requestor,dbFields[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } else if(elementType==Type.pvArray) {
                PVArrayArray pvArrayArray = (PVArrayArray)dbField.getPVField();
                int len = pvArrayArray.getLength();
                ArrayArrayData data = new ArrayArrayData();
                int nsofar = 0;
                int offset = 0;
                while(nsofar<len) {
                    int n = pvArrayArray.get(offset,len-nsofar,data);
                    if(n<=0) break;
                    for(int i=0; i<n; i++) {
                        if(dbFields[i]==null) continue;
                        if(!createArraySupport(dbFields[i])) result = false;
                    }
                    nsofar += n; offset += n;
                }
            } 
            return result;
        }
    }
    
    private static void printError(Requestor requestor,PVField pvField,String message) {
        requestor.message(
                pvField.getFullName() + " " + message,
                MessageType.error);
    }
    
    private static boolean createSupportPvt(Requestor requestor,DBField dbField) {
        if(dbField.getSupport()!=null) return true;
        String supportName = dbField.getSupportName();
        if(supportName==null) return true;
        String factoryName = null;
        PVField pvField = dbField.getPVField();
        DBD dbd = DBDFactory.getMasterDBD();
        if(pvField.getField().getType()==Type.pvLink) {
            DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(supportName);
            if(dbdLinkSupport==null) {
                printError(requestor,pvField,"linkSupport " + supportName + " does not exist");
                return false;
            }
            factoryName = dbdLinkSupport.getFactoryName();
            if(factoryName==null) {
                printError(requestor,pvField,"linkSupport " + supportName + " does not define a factory name");
                return false;
            }
        } else {
            DBDSupport dbdSupport = dbd.getSupport(supportName);
            if(dbdSupport==null) {
                printError(requestor,pvField,"support " + supportName + " does not exist");
                return false;
            }
            factoryName = dbdSupport.getFactoryName();
            if(factoryName==null) {
                printError(requestor,pvField,"support " + supportName + " does not define a factory name");
                return false;
            }
        }
        Class supportClass;
        Support support = null;
        Method method = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
            printError(requestor,pvField,
                    "support " + supportName 
                    + " factory " + e.getLocalizedMessage()
                    + " class not found");
            return false;
        }
        String data = null;
        Type type = pvField.getField().getType();
        if (type==Type.pvLink) {
            data = "DBLink";
        } else if (type==Type.pvStructure) {
            data = "DBStructure";
        } else {
            data = "DBField";
        }
        data = "org.epics.ioc.db." + data;
        try {
            method = supportClass.getDeclaredMethod("create",
                    Class.forName(data));    
        } catch (NoSuchMethodException e) {
            printError(requestor,pvField,
                    "support "
                    + supportName
                    + " no factory method "
                    + e.getLocalizedMessage());
            return false;
        } catch (ClassNotFoundException e) {
            printError(requestor,pvField,
                    "support "
                    + factoryName
                    + " arg class "
                    + e.getLocalizedMessage());
            return false;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            printError(requestor,pvField,
                    "support "
                    + factoryName
                    + " create is not a static method ");
            return false;
        }
        try {
            support = (Support)method.invoke(null,dbField);
        } catch(IllegalAccessException e) {
            printError(requestor,pvField,
                    "support "
                    + supportName
                    + " create IllegalAccessException "
                    + e.getLocalizedMessage());
            return false;
        } catch(IllegalArgumentException e) {
            printError(requestor,pvField,
                    "support "
                    + supportName
                    + " create IllegalArgumentException "
                    + e.getLocalizedMessage());
            return false;
        } catch(InvocationTargetException e) {
            printError(requestor,pvField,
                    "support "
                    + supportName
                    + " create InvocationTargetException "
                    + e.getLocalizedMessage());
            return false;
        }
        dbField.setSupport(support);
        return true;
    }
    
}
