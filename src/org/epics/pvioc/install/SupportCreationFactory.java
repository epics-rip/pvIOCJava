/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.install;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVAuxInfo;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessFactory;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportState;


/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class SupportCreationFactory {
    /**
     * create a process database.
     * @param pvDatabase The PVDatabase.
     * @param requester The requester.
     * @return the SupportCreation.
     */
    static public SupportCreation create(PVDatabase pvDatabase,Requester requester) {
        return new SupportCreationImpl(pvDatabase,requester);
    }
    
    private static final PVDatabase masterDatabase = PVDatabaseFactory.getMaster();
    private static final String supportFactory = "supportFactory";
    
    static private class SupportCreationImpl implements SupportCreation{
        private Requester requester;
        private PVRecord[] pvRecords;
        
        private SupportCreationImpl(PVDatabase pvDatabase,Requester requester) {
            this.requester = requester;
            pvRecords = pvDatabase.getRecords();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.SupportCreation#createSupport()
         */
        public boolean createSupport() {
            boolean result = true;
            for(PVRecord pvRecord : pvRecords) {
                result = SupportCreationFactory.createSupportPvt(requester,pvRecord.getPVRecordStructure());
                if(!result) return result;
                RecordProcessFactory.createRecordProcess(pvRecord);
            }
            for(PVRecord pvRecord : pvRecords) {
                if(!createStructureSupport(pvRecord.getPVRecordStructure())) result = false;
            }
            return result;
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.process.SupportCreation#initializeSupport()
         */
        public boolean initializeSupport() {
            boolean result = true;
            for(int i=0; i<pvRecords.length; i++) {
                PVRecord pvRecord = pvRecords[i];
                RecordProcess process = pvRecord.getRecordProcess();
                process.initialize();
                SupportState supportState = process.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    printError(requester,pvRecord.getPVRecordStructure(),
                        " state " + supportState.toString()
                        + " but should be readyForStart");
                    result = false;
                }
            }
            return result;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.SupportCreation#startSupport()
         */
        public boolean startSupport(AfterStart afterStart) {
            boolean result = true;
            for(int i=0; i<pvRecords.length; i++) {
                PVRecord pvRecord = pvRecords[i];
                RecordProcess process = pvRecord.getRecordProcess();
                process.start(afterStart);
                SupportState supportState = process.getSupportState();
                if(supportState!=SupportState.ready) {
                    printError(requester,pvRecord.getPVRecordStructure(),
                        " state " + supportState.toString()
                        + " but should be ready");
                    result = false;
                }
            }
            return result;
            
        }
       
   
        private boolean createStructureSupport(PVRecordStructure pvRecordStructure) {
        	boolean result = SupportCreationFactory.createSupportPvt(requester,pvRecordStructure);
            PVRecordField[] pvRecordFields = pvRecordStructure.getPVRecordFields();
            for(int i=0; i<pvRecordFields.length; i++) {
            	PVRecordField pvRecordField = pvRecordFields[i];
                Type type = pvRecordField.getPVField().getField().getType();
                if(type==Type.structure) {
                    if(!createStructureSupport((PVRecordStructure)pvRecordField)) result = false;
                } else {
                    if(!SupportCreationFactory.createSupportPvt(requester,pvRecordField)) result = false;
                }
            }
            return result;
        }
    }
    
    private static void printError(Requester requester,PVRecordField pvRecordField,String message) {
        requester.message(
                pvRecordField.getFullName() + " " + message,
                MessageType.error);
    }
    
    private static boolean createSupportPvt(Requester requester,PVRecordField pvRecordField) {
    	PVRecord pvRecord = pvRecordField.getPVRecord();
        if(pvRecordField.getSupport()!=null) return true;
        PVField pvField = pvRecordField.getPVField();
        PVAuxInfo pvAuxInfo = pvField.getPVAuxInfo();
        PVScalar pvAuxField = pvAuxInfo.getInfo(supportFactory);
        if(pvAuxField==null) {
            if(pvRecordField!=pvRecord.getPVRecordStructure()) return true;
            pvAuxField = pvAuxInfo.createInfo(supportFactory, ScalarType.pvString);
            PVString pvString = (PVString)pvAuxField;
            pvString.put("org.epics.pvioc.genericFactory");
        }
        if(pvAuxField.getScalar().getScalarType()!=ScalarType.pvString) {
            printError(requester,pvRecordField,"pvAuxInfo for support is not a string");
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
            printError(requester,pvRecordField,"support " + supportName + " does not exist");
            return false;
        }
        PVString factoryNamePV = pvStructure.getStringField("supportFactory");
        if(factoryNamePV==null) return false;
        factoryName = factoryNamePV.get();
        if(factoryName==null) {
            printError(requester,pvRecordField,"support " + supportName + " does not define a factory name");
            return false;
        }
        Class supportClass;
        Support support = null;
        Method method = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
            printError(requester,pvRecordField,
                    "support " + supportName 
                    + " factory " + e.getLocalizedMessage()
                    + " class not found");
            return false;
        }
        String data = null;
        Type type = pvRecordField.getPVField().getField().getType();
        if(type==Type.structure) {
        	data = "PVRecordStructure";
        } else {
        	data = "PVRecordField";
        }
        data  = "org.epics.pvioc.database." + data;
        try {
            method = supportClass.getDeclaredMethod("create",
                    Class.forName(data));    
        } catch (NoSuchMethodException e) {
            printError(requester,pvRecordField,
                    "support "
                    + supportName
                    + " no factory method "
                    + e.getLocalizedMessage());
            return false;
        } catch (ClassNotFoundException e) {
            printError(requester,pvRecordField,
                    "support "
                    + factoryName
                    + " arg class "
                    + e.getLocalizedMessage());
            return false;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            printError(requester,pvRecordField,
                    "support "
                    + factoryName
                    + " create is not a static method ");
            return false;
        }
        try {
            support = (Support)method.invoke(null,pvRecordField);
        } catch(IllegalAccessException e) {
            printError(requester,pvRecordField,
                    "support "
                    + supportName
                    + " invoke IllegalAccessException "
                    + e.getLocalizedMessage());
            return false;
        } catch(IllegalArgumentException e) {
            printError(requester,pvRecordField,
                    "support "
                    + supportName
                    + " invoke IllegalArgumentException "
                    + e.getLocalizedMessage());
            return false;
        } catch(InvocationTargetException e) {
            printError(requester,pvRecordField,
                    "support "
                    + supportName
                    + " invoke InvocationTargetException "
                    + e.getLocalizedMessage());
            return false;
        }
        if(support==null) {
            printError(requester,pvRecordField,"support "
                    + supportName + " was not created");
        }
        pvRecordField.setSupport(support);
        return true;
    }
}
