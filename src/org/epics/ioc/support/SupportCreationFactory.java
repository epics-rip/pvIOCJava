/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;


/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class SupportCreationFactory {
    /**
     * create a process database.
     * @param supportDatabase The supportDatabase.
     * @param requester The requester.
     * @return the SupportCreation.
     */
    static public SupportCreation createSupportCreation(SupportDatabase supportDatabase,Requester requester) {
        return new SupportCreationImpl(supportDatabase,requester);
    }
    
    static public boolean createSupport(Requester requester,RecordSupport recordSupport,PVField pvField) {
        return createSupportPvt(requester,recordSupport,pvField);
    }
    
    private static final PVDatabase masterDatabase = PVDatabaseFactory.getMaster();
    private static final String supportFactory = "supportFactory";
    
    static private class SupportCreationImpl implements SupportCreation{
        
        private SupportDatabase supportDatabase;
        private Requester requester;
        private PVRecord[] pvRecords;
        // Must keep private copy of recordSupport so that merge can be implement
        private RecordSupport[] recordSupports;
        
        private SupportCreationImpl(SupportDatabase supportDatabase,Requester requester) {
            this.supportDatabase = supportDatabase;
            this.requester = requester;
            pvRecords = supportDatabase.getPVDatabase().getRecords();
            recordSupports = new RecordSupport[pvRecords.length];
            for(int i=0; i<pvRecords.length; i++) {
                recordSupports[i] = supportDatabase.getRecordSupport(pvRecords[i]);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#createSupport()
         */
        public boolean createSupport() {
            boolean result = true;
            for(PVRecord pvRecord : pvRecords) {
                RecordSupport recordSupport = supportDatabase.getRecordSupport(pvRecord);
                if(recordSupport.getRecordProcess()!=null) continue;
                result = SupportCreationFactory.createSupportPvt(requester,recordSupport,pvRecord);
                if(!result) return result;
                    if(recordSupport.getRecordProcess()!=null) continue;
                    RecordProcess recordProcess =
                        RecordProcessFactory.createRecordProcess(recordSupport,pvRecord);
                    recordSupport.setRecordProcess(recordProcess);
            }
            for(PVRecord pvRecord : pvRecords) {
                RecordSupport recordSupport = supportDatabase.getRecordSupport(pvRecord);
                if(!createStructureSupport(recordSupport,pvRecord)) result = false;
            }
            return result;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#initializeSupport()
         */
        public boolean initializeSupport() {
            boolean result = true;
            for(PVRecord pvRecord : pvRecords) {
                RecordSupport recordSupport = supportDatabase.getRecordSupport(pvRecord);
                Support support = recordSupport.getSupport(pvRecord);
                RecordProcess process = recordSupport.getRecordProcess();
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
            for(int i=0; i<pvRecords.length; i++) {
                PVRecord pvRecord = pvRecords[i];
                RecordSupport recordSupport = recordSupports[i];
                Support support = recordSupport.getSupport(pvRecord);
                RecordProcess process = recordSupport.getRecordProcess();
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
                for(RecordSupport recordSupport : recordSupports) {
                    RecordProcess process = recordSupport.getRecordProcess();
                    process.allSupportStarted();
                }
            }
            return result;
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#stopSupport()
         */
        public void stopSupport() {
            for(PVRecord pvRecord : pvRecords) {
                RecordSupport recordSupport = supportDatabase.getRecordSupport(pvRecord);
                RecordProcess process = recordSupport.getRecordProcess();
                process.stop();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportCreation#uninitializeSupport()
         */
        public void uninitializeSupport() {
            for(PVRecord pvRecord : pvRecords) {
                RecordSupport recordSupport = supportDatabase.getRecordSupport(pvRecord);
                RecordProcess process = recordSupport.getRecordProcess();
                process.uninitialize();
            }
        }
  
            
        
        
        private boolean createStructureSupport(RecordSupport recordSupport,PVStructure pvStructure) {
            boolean result = SupportCreationFactory.createSupportPvt(requester,recordSupport,pvStructure);
            PVField[] pvFields = pvStructure.getPVFields();
            for(PVField pvField : pvFields) {
                Type type = pvField.getField().getType();
                if(type==Type.structure) {
                    if(!createStructureSupport(recordSupport,(PVStructure)pvField)) result = false;
                } else {
                    if(!SupportCreationFactory.createSupportPvt(requester,recordSupport,pvField)) result = false;
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
    
    private static boolean createSupportPvt(Requester requester,RecordSupport recordSupport,PVField pvField) {
        if(recordSupport.getSupport(pvField)!=null) return true;
        PVAuxInfo pvAuxInfo = pvField.getPVAuxInfo();
        PVScalar pvAuxField = pvAuxInfo.getInfo(supportFactory);
        if(pvAuxField==null) {
            if(pvField!=pvField.getPVRecord()) return true;
            pvAuxField = pvAuxInfo.createInfo(supportFactory, ScalarType.pvString);
            PVString pvString = (PVString)pvAuxField;
            pvString.put("genericFactory");
        }
        if(pvAuxField.getScalar().getScalarType()!=ScalarType.pvString) {
            printError(requester,pvField,"pvAuxInfo for support is not a string");
            return false;
        }
        PVString pvString = (PVString)pvAuxField;
        String supportName = pvString.get();
        if(supportName==null) return true;
        if(supportName.length()<=0) return true;
        if(supportName.equals("null")) return true;
        String factoryName = null;
        PVStructure pvStructure = masterDatabase.findStructure(supportName);
        if(pvStructure==null) {
            printError(requester,pvField,"support " + supportName + " does not exist");
            return false;
        }
        PVString factoryNamePV = pvStructure.getStringField("supportFactory");
        if(factoryNamePV==null) return false;
        factoryName = factoryNamePV.get();
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
        if (type==Type.structure) {
            data = "PVStructure";
        } else {
            data = "PVField";
        }
        data = "org.epics.pvData.pv." + data;
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
            support = (Support)method.invoke(null,pvField);
        } catch(IllegalAccessException e) {
            printError(requester,pvField,
                    "support "
                    + supportName
                    + " invoke IllegalAccessException "
                    + e.getLocalizedMessage());
            return false;
        } catch(IllegalArgumentException e) {
            printError(requester,pvField,
                    "support "
                    + supportName
                    + " invoke IllegalArgumentException "
                    + e.getLocalizedMessage());
            return false;
        } catch(InvocationTargetException e) {
            printError(requester,pvField,
                    "support "
                    + supportName
                    + " invoke InvocationTargetException "
                    + e.getLocalizedMessage());
            return false;
        }
        recordSupport.setSupport(pvField, support);
        return true;
    }
    
}
