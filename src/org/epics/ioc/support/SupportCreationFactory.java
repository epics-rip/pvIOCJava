/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.ioc.db.DBArray;
import org.epics.ioc.db.DBArrayArray;
import org.epics.ioc.db.DBD;
import org.epics.ioc.db.DBDFactory;
import org.epics.ioc.db.DBDSupport;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.DBStructureArray;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

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
    static public SupportCreation createSupportCreation(IOCDB iocdb,Requester requester) {
        return new SupportCreationImpl(iocdb,requester);
    }
    
    static public boolean createSupport(DBField dbField) {
        return createSupportPvt(dbField.getPVField(),dbField);
    }
    
    static private class SupportCreationImpl implements SupportCreation{
        private IOCDB iocdb;
        private Requester requester;
        private DBRecord[] records;
        
        private SupportCreationImpl(IOCDB iocdbin,Requester requester) {
            iocdb = iocdbin;
            this.requester = requester;
            records = iocdb.getDBRecords();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#createSupport()
         */
        public boolean createSupport() {
            boolean result = true;
            for(DBRecord record : records) {
                if(!createRecordSupport(record)) {
                    PVRecord pvRecord = record.getPVRecord();
                    printError(requester,pvRecord,
                            "no record support for record " + pvRecord.getRecordName());
                    result = false;
                } else {
                    if(record.getRecordProcess()!=null) continue;
                    RecordProcess recordProcess =
                        RecordProcessFactory.createRecordProcess(record);
                    record.setRecordProcess(recordProcess);
                }
            }
            for(DBRecord record : records) {
                DBStructure dbStructure = record.getDBStructure();
                if(!createStructureSupport(dbStructure)) result = false;
            }
            return result;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#initializeSupport()
         */
        public boolean initializeSupport() {
            boolean result = true;
            for(DBRecord record : records) {
                PVRecord pvRecord = record.getPVRecord();
                Support support = record.getDBStructure().getSupport();
                RecordProcess process = record.getRecordProcess();
                process.initialize();
                SupportState supportState = support.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    printError(requester,pvRecord,
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
            for(DBRecord record : records) {
                PVRecord pvRecord = record.getPVRecord();
                Support support = record.getDBStructure().getSupport();
                RecordProcess process = record.getRecordProcess();
                process.start();
                SupportState supportState = support.getSupportState();
                if(supportState!=SupportState.ready) {
                    printError(requester,pvRecord,
                            " state " + supportState.toString()
                            + " but should be ready");
                    result = false;
                }
            }
            if(result) {
                for(DBRecord record : records) {
                    RecordProcess process = record.getRecordProcess();
                    process.allSupportStarted();
                }
            }
            return result;
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#stopSupport()
         */
        public void stopSupport() {
            for(DBRecord record : records) {
                RecordProcess process = record.getRecordProcess();
                process.stop();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#uninitializeSupport()
         */
        public void uninitializeSupport() {
            for(DBRecord record : records) {
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
                requester.message(
                    pvRecord.getRecordName() + " no support found",
                    MessageType.fatalError);
                return false;
            }
            boolean result = SupportCreationFactory.createSupportPvt(requester,dbStructure);
            if(!result) return result;
            return true;
        }
        
        
        private boolean createStructureSupport(DBField dbField) {
            boolean result = SupportCreationFactory.createSupportPvt(requester,dbField);
            DBStructure dbStructure = (DBStructure)dbField;
            DBField[] dbFields = dbStructure.getDBFields();
            for(DBField field : dbFields) {
                Type type = field.getPVField().getField().getType();
                if(type==Type.pvStructure) {
                    if(!createStructureSupport(field)) result = false;
                } else if(type==Type.pvArray) {
                    if(!createArraySupport(field)) result = false;
                } else {
                    if(!SupportCreationFactory.createSupportPvt(requester,field)) result = false;
                }
            }
            return result;
        }
        
        private boolean createArraySupport(DBField dbField) {
            boolean result = true;
            PVArray pvArray = (PVArray)dbField.getPVField();
            if(!SupportCreationFactory.createSupportPvt(requester,dbField)) result = false;
            Array array = (Array)pvArray.getField();
            Type elementType = array.getElementType();
            if(elementType.isScalar()) return result;
            if(elementType==Type.pvArray) {
                DBArrayArray dbArrayArray = (DBArrayArray)dbField;
                DBArray[] dbFields = dbArrayArray.getElementDBArrays();
                for(DBField dbf : dbFields) {
                    if(dbf==null) continue;
                    if(!createArraySupport(dbf)) result = false;
                }
            } else if(elementType==Type.pvStructure) {
                DBStructureArray dbStructureArray = (DBStructureArray)dbField;
                DBStructure[] dbFields = dbStructureArray.getElementDBStructures();
                for(DBField dbf : dbFields) {
                    if(dbf==null) continue;
                    if(!createStructureSupport(dbf)) result = false;
                }
            } 
            return result;
        }
    }
    
    private static void printError(Requester requester,PVField pvField,String message) {
        requester.message(
                pvField.getFullName() + " " + message,
                MessageType.error);
    }
    
    private static boolean createSupportPvt(Requester requester,DBField dbField) {
        if(dbField.getSupport()!=null) return true;
        String supportName = dbField.getSupportName();
        if(supportName==null) return true;
        if(supportName.length()<=0) return true;
        if(supportName.equals("null")) return true;
        String factoryName = null;
        PVField pvField = dbField.getPVField();
        DBD dbd = DBDFactory.getMasterDBD();       
        DBDSupport dbdSupport = dbd.getSupport(supportName);
        if(dbdSupport==null) {
            printError(requester,pvField,"support " + supportName + " does not exist");
            return false;
        }
        factoryName = dbdSupport.getFactoryName();
        if(factoryName==null) {
            printError(requester,pvField,"support " + supportName + " does not define a factory name");
            return false;
        }
        Class supportClass;
        Support support = null;
        Method method = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
            printError(requester,pvField,
                    "support " + supportName 
                    + " factory " + e.getLocalizedMessage()
                    + " class not found");
            return false;
        }
        String data = null;
        Type type = pvField.getField().getType();
        if (type==Type.pvStructure) {
            data = "DBStructure";
        } else {
            data = "DBField";
        }
        data = "org.epics.ioc.db." + data;
        try {
            method = supportClass.getDeclaredMethod("create",
                    Class.forName(data));    
        } catch (NoSuchMethodException e) {
            printError(requester,pvField,
                    "support "
                    + supportName
                    + " no factory method "
                    + e.getLocalizedMessage());
            return false;
        } catch (ClassNotFoundException e) {
            printError(requester,pvField,
                    "support "
                    + factoryName
                    + " arg class "
                    + e.getLocalizedMessage());
            return false;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            printError(requester,pvField,
                    "support "
                    + factoryName
                    + " create is not a static method ");
            return false;
        }
        try {
            support = (Support)method.invoke(null,dbField);
        } catch(IllegalAccessException e) {
            printError(requester,pvField,
                    "support "
                    + supportName
                    + " create IllegalAccessException "
                    + e.getLocalizedMessage());
            return false;
        } catch(IllegalArgumentException e) {
            printError(requester,pvField,
                    "support "
                    + supportName
                    + " create IllegalArgumentException "
                    + e.getLocalizedMessage());
            return false;
        } catch(InvocationTargetException e) {
            printError(requester,pvField,
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
