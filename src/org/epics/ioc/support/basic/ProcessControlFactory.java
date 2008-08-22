/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportCreationFactory;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanPriority;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class ProcessControlFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param dbField The array which must be an array of links.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(DBStructure dbField) {
        return new ProcessControlImpl(dbField);
    }
    
    private static String supportName = "processControl";
    
    
    private static class ProcessControlImpl extends AbstractSupport
    implements Runnable,ProcessContinueRequester
    {
        private static final DBD dbd = DBDFactory.getMasterDBD();
        private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
        private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
        private IOCExecutor iocExecutor = IOCExecutorFactory.create(supportName, ScanPriority.lowest);
        private IOCDB masterIOCDB = IOCDBFactory.getMaster();
        private RecordProcess recordProcess = null;
        
        private DBField dbMessage = null;
        private PVString pvMessage = null;
        
        private PVString recordNamePVString = null;
        
        private PVBoolean traceValuePVBoolean = null;
        private DBField traceValueDBField = null;
        private PVBoolean traceDesiredValuePVBoolean = null;
        private PVBoolean traceSetValuePVBoolean = null;
        private DBField traceSetValueDBField = null;
        
        private PVBoolean enableValuePVBoolean = null;
        private DBField enableValueDBField = null;
        private PVBoolean enableDesiredValuePVBoolean = null;
        private PVBoolean enableSetValuePVBoolean = null;
        private DBField enableSetValueDBField = null;
        
        private Enumerated supportStateRecordEnumerated = null;
        private PVInt supportStateRecordPVInt = null;
        private DBField supportStateRecordDBField = null;
        private Enumerated supportStateCommandRecordEnumerated = null;
        private PVInt supportStateCommandRecordPVInt = null;
        private DBField supportStateCommandRecordDBField = null;
        
        private PVString fieldNamePVString = null;
        private DBField fieldNameDBField = null;
        private PVString fieldNameDesiredPVString = null;
        private PVBoolean fieldNameSetPVBoolean = null;
        private DBField fieldNameSetDBField = null;
        
        private Enumerated supportStateEnumerated = null;
        private PVInt supportStatePVInt = null;
        private DBField supportStateDBField = null;
        private Enumerated supportStateCommandEnumerated = null;
        private PVInt supportStateCommandPVInt = null;
        private DBField supportStateCommandDBField = null;
        
        private PVString supportNamePVString = null;
        private DBField supportNameDBField = null;
        private PVString supportNameDesiredPVString = null;
        private PVBoolean supportNameSetPVBoolean = null;
        private DBField supportNameSetDBField = null;
       
        private PVString structureNamePVString = null;
        private DBField structureNameDBField = null;
        private PVString structureNameDesiredPVString = null;
        private PVBoolean structureNameSetPVBoolean = null;
        private DBField structureNameSetDBField = null;
       
        private DBRecord recordDBRecord = null;
        private RecordProcess recordRecordProcess = null;
        private DBField fieldDBField = null;
        private Support fieldSupport = null;
        
        private RequestResult requestResult = null;
        private String errorMessage = null;
        private SupportProcessRequester supportProcessRequester = null;
        
        private String recordNameDesired = null;
        private String recordNamePrevious = null;
        private SupportStateCommand supportStateRecordCommand = null; 
        private SupportState supportStateRecord = null;
        private boolean traceDesiredValue = false;
        private boolean traceSetValue = false;
        private boolean traceNewValue = false;
        private boolean enableDesiredValue = false;
        private boolean enableSetValue = false;
        private boolean enableNewValue = false;
        
        private String fieldNameDesired = null;
        private boolean fieldNameSet = false;
        private String fieldNameNew = null;

        private SupportStateCommand supportStateCommand = null; 
        private SupportState supportState = null;
        
        private String supportNameDesired = null;
        private boolean supportNameSet = false;
        private String supportNameNew = null;
        
        private String structureNameDesired = null;
        private boolean structureNameSet = false;
        private String structureNameNew = null;
        
        private ProcessControlImpl(DBStructure dbField) {
            super(supportName,dbField);
            
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            DBField dbField = super.getDBField();
            DBRecord dbRecord = dbField.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            
            PVStructure pvRecord = dbRecord.getPVRecord();
            
            pvMessage = pvRecord.getStringField("message");
            if(pvMessage==null) return;
            dbMessage = dbRecord.findDBField(pvMessage);
            
            
            recordNamePVString = pvRecord.getStringField("recordName");
            if(recordNamePVString==null) return;
            
            PVStructure tracePVStructure = pvRecord.getStructureField("trace", "booleanState");
            if(tracePVStructure==null) return;
            traceValuePVBoolean = tracePVStructure.getBooleanField("value");
            if(traceValuePVBoolean==null) return;
            traceValueDBField = dbRecord.findDBField(traceValuePVBoolean);
            traceDesiredValuePVBoolean = tracePVStructure.getBooleanField("desiredValue");
            if(traceDesiredValuePVBoolean==null) return;
            traceSetValuePVBoolean = tracePVStructure.getBooleanField("setValue");
            if(traceSetValuePVBoolean==null) return;
            traceSetValueDBField = dbRecord.findDBField(traceSetValuePVBoolean);
            
            PVStructure enablePVStructure = pvRecord.getStructureField("enable", "booleanState");
            if(enablePVStructure==null) return;
            enableValuePVBoolean = enablePVStructure.getBooleanField("value");
            if(enableValuePVBoolean==null) return;
            enableValueDBField = dbRecord.findDBField(enableValuePVBoolean);
            enableDesiredValuePVBoolean = enablePVStructure.getBooleanField("desiredValue");
            if(enableDesiredValuePVBoolean==null) return;
            enableSetValuePVBoolean = enablePVStructure.getBooleanField("setValue");
            if(enableSetValuePVBoolean==null) return;
            enableSetValueDBField = dbRecord.findDBField(enableSetValuePVBoolean);
            
            PVStructure pvStructure = pvRecord.getStructureField("supportStateRecord", "supportState");
            if(pvStructure==null) return;
            dbField = dbRecord.findDBField(pvStructure);
            supportStateRecordEnumerated = SupportState.getSupportState(dbField);
            if(supportStateRecordEnumerated==null) return;
            supportStateRecordPVInt = supportStateRecordEnumerated.getIndexField();
            supportStateRecordDBField = dbRecord.findDBField(supportStateRecordPVInt);
            pvStructure = pvRecord.getStructureField("supportStateCommandRecord", "supportStateCommand");
            if(pvStructure==null) return;
            dbField = dbRecord.findDBField(pvStructure);
            supportStateCommandRecordEnumerated = SupportStateCommand.getSupportStateCommand(dbField);
            if(supportStateCommandRecordEnumerated==null) return;
            supportStateCommandRecordPVInt = supportStateCommandRecordEnumerated.getIndexField();
            supportStateCommandRecordDBField = dbRecord.findDBField(supportStateCommandRecordPVInt);
            
            PVStructure fieldNamePVStructure = pvRecord.getStructureField("fieldName", "stringState");
            if(fieldNamePVStructure==null) return;
            fieldNamePVString = fieldNamePVStructure.getStringField("value");
            if(fieldNamePVString==null) return;
            fieldNameDBField = dbRecord.findDBField(fieldNamePVString);
            fieldNameDesiredPVString = fieldNamePVStructure.getStringField("desiredValue");
            if(fieldNameDesiredPVString==null) return;
            fieldNameSetPVBoolean = fieldNamePVStructure.getBooleanField("setValue");
            if(fieldNameSetPVBoolean==null) return;
            fieldNameSetDBField = dbRecord.findDBField(fieldNameSetPVBoolean);
            
            pvStructure = pvRecord.getStructureField("supportState", "supportState");
            if(pvStructure==null) return;
            dbField = dbRecord.findDBField(pvStructure);
            supportStateEnumerated = SupportState.getSupportState(dbField);
            if(supportStateEnumerated==null) return;
            supportStatePVInt = supportStateEnumerated.getIndexField();
            supportStateDBField = dbRecord.findDBField(supportStatePVInt);
            pvStructure = pvRecord.getStructureField("supportStateCommand", "supportStateCommand");
            if(pvStructure==null) return;
            dbField = dbRecord.findDBField(pvStructure);
            supportStateCommandEnumerated = SupportStateCommand.getSupportStateCommand(dbField);
            if(supportStateCommandEnumerated==null) return;
            supportStateCommandPVInt = supportStateCommandEnumerated.getIndexField();
            supportStateCommandDBField = dbRecord.findDBField(supportStateCommandPVInt);
            
            PVStructure supportNamePVStructure = pvRecord.getStructureField("supportName", "stringState");
            if(supportNamePVStructure==null) return;
            supportNamePVString = supportNamePVStructure.getStringField("value");
            if(supportNamePVString==null) return;
            supportNameDBField = dbRecord.findDBField(supportNamePVString);
            supportNameDesiredPVString = supportNamePVStructure.getStringField("desiredValue");
            if(supportNameDesiredPVString==null) return;
            supportNameSetPVBoolean = supportNamePVStructure.getBooleanField("setValue");
            if(supportNameSetPVBoolean==null) return;
            supportNameSetDBField = dbRecord.findDBField(supportNameSetPVBoolean);
            
            PVStructure structureNamePVStructure = pvRecord.getStructureField("structureName", "stringState");
            if(structureNamePVStructure==null) return;
            structureNamePVString = structureNamePVStructure.getStringField("value");
            if(structureNamePVString==null) return;
            structureNameDBField = dbRecord.findDBField(structureNamePVString);
            structureNameDesiredPVString = structureNamePVStructure.getStringField("desiredValue");
            if(structureNameDesiredPVString==null) return;
            structureNameSetPVBoolean = structureNamePVStructure.getBooleanField("setValue");
            if(structureNameSetPVBoolean==null) return;
            structureNameSetDBField = dbRecord.findDBField(structureNameSetPVBoolean);
            
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            recordNameDesired = recordNamePVString.get();
            if(recordNameDesired==null || recordNameDesired.equals("")) {
                pvMessage.put("recordName is null");
                dbMessage.postPut();
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            requestResult = RequestResult.success;
            errorMessage = null;
            this.supportProcessRequester = supportProcessRequester;
            supportStateRecord = null;
            supportStateRecordCommand = SupportStateCommand.getSupportStateCommand(
                    supportStateCommandRecordPVInt.get());
            traceDesiredValue = traceDesiredValuePVBoolean.get();
            traceSetValue = traceSetValuePVBoolean.get();
            enableDesiredValue = enableDesiredValuePVBoolean.get();
            enableSetValue = enableSetValuePVBoolean.get();
            
            fieldNameDesired = fieldNameDesiredPVString.get();
            fieldNameSet = fieldNameSetPVBoolean.get();
            fieldNameNew = null;
            
            supportState = null;
            supportStateCommand = SupportStateCommand.getSupportStateCommand(
                    supportStateCommandPVInt.get());
            supportNameDesired = supportNameDesiredPVString.get();
            supportNameSet = supportNameSetPVBoolean.get();
            supportNameNew = null;
            structureNameDesired = structureNameDesiredPVString.get();
            structureNameSet = structureNameSetPVBoolean.get();
            structureNameNew = null;
            iocExecutor.execute(this);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if(!recordNameDesired.equals(recordNamePrevious)) {
                recordDBRecord = masterIOCDB.findRecord(recordNameDesired);
                if(recordDBRecord==null) {
                    requestResult = RequestResult.failure;
                    errorMessage = "recordName " + recordNameDesired + " not found";
                    recordProcess.processContinue(this);
                    return;
                }
                recordNamePrevious = recordNameDesired;
                recordRecordProcess = recordDBRecord.getRecordProcess();
                if(recordRecordProcess==null) {
                    requestResult = RequestResult.failure;
                    errorMessage = "recordProcess for "
                        + recordDBRecord.getPVRecord().getFullName()
                        + " not found";
                    recordDBRecord = null;
                    recordProcess.processContinue(this);
                    return;
                }

            }
            if(recordDBRecord==null) {
                requestResult = RequestResult.failure;
                errorMessage = "recordDBRecord is null";
                recordProcess.processContinue(this);
                return;
            }
            recordDBRecord.lock();
            try {
                if(traceSetValue) recordRecordProcess.setTrace(traceDesiredValue);
                traceNewValue = recordRecordProcess.isTrace();
                if(enableSetValue) recordRecordProcess.setEnabled(enableDesiredValue);
                enableNewValue = recordRecordProcess.isEnabled();
                supportStateRecord = recordRecordProcess.getSupportState();
                switch(supportStateRecordCommand) {
                case idle:
                    break;
                case initialize:
                    if(supportStateRecord!=SupportState.readyForInitialize) {
                        recordRecordProcess.uninitialize();
                        waitForState(SupportState.readyForInitialize);
                    }
                    recordRecordProcess.initialize(); break;
                case start:
                    if(supportStateRecord!=SupportState.readyForStart) {
                        if(supportStateRecord==SupportState.ready) {
                            recordRecordProcess.stop();
                            waitForState(SupportState.readyForStart);
                        } else if(supportStateRecord==SupportState.readyForInitialize) {
                            recordRecordProcess.initialize();
                        }
                        supportStateRecord = recordRecordProcess.getSupportState();
                        if(supportStateRecord!=SupportState.readyForStart) {
                            requestResult = RequestResult.failure;
                            errorMessage = "record support is not readyForStart";
                            recordProcess.processContinue(this);
                            return;
                        }
                    }
                    recordRecordProcess.start(); break;
                case stop:
                    if(supportStateRecord!=SupportState.ready) {
                        requestResult = RequestResult.failure;
                        errorMessage = "support is not ready";
                        recordProcess.processContinue(this);
                        return;
                    }
                    recordRecordProcess.stop();
                    waitForState(SupportState.readyForStart);
                    break;
                case uninitialize:
                    recordRecordProcess.uninitialize();
                    waitForState(SupportState.readyForInitialize);
                    break;
                }
                supportStateRecord = recordRecordProcess.getSupportState();
                if(fieldNameSet) {
                    if(fieldNameDesired==null || fieldNameDesired.equals("")) {
                        fieldDBField = recordDBRecord.getDBStructure();
                    } else {
                        PVRecord pvRecord = recordDBRecord.getPVRecord();
                        PVField pvField = pvProperty.findProperty(pvRecord,fieldNameDesired);
                        if(pvField==null) {
                            pvField = pvRecord.getSubField(fieldNameDesired);
                        }
                        if(pvField==null) {
                            fieldDBField = null;
                        } else {
                            fieldDBField = recordDBRecord.findDBField(pvField);
                        }
                    }
                    if(fieldDBField==null) {
                        requestResult = RequestResult.failure;
                        errorMessage = "fieldName " + fieldNameDesired + " not found";
                        recordProcess.processContinue(this);
                        return;
                    }
                    fieldNameNew = fieldNameDesired;
                }
                if(fieldDBField!=null)fieldSupport = fieldDBField.getSupport();
                if(supportStateCommand!=SupportStateCommand.idle) {
                    if(fieldSupport==null) {
                        requestResult = RequestResult.failure;
                        errorMessage = "field support is null";
                        recordProcess.processContinue(this);
                        return;
                    }
                    supportState = fieldSupport.getSupportState();
                    switch(supportStateCommand) {
                    case idle:
                        break;
                    case initialize:
                        if(supportState!=SupportState.readyForInitialize) {
                            fieldSupport.uninitialize();
                        }
                        fieldSupport.initialize(); break;
                    case start:
                        if(supportState!=SupportState.readyForStart) {
                            if(supportState==SupportState.ready) {
                                fieldSupport.stop();
                            } else if(supportState==SupportState.readyForInitialize) {
                                fieldSupport.initialize();
                            }
                            supportState = fieldSupport.getSupportState();
                            if(supportState!=SupportState.readyForStart) {
                                requestResult = RequestResult.failure;
                                errorMessage = "support is not readyForStart";
                                recordProcess.processContinue(this);
                                return;
                            }
                        }
                        fieldSupport.start(); break;
                    case stop:
                        if(supportState!=SupportState.ready) {
                            requestResult = RequestResult.failure;
                            errorMessage = "support is not ready";
                            recordProcess.processContinue(this);
                            return;
                        }
                        fieldSupport.stop(); break;
                    case uninitialize:
                        fieldSupport.uninitialize(); break;
                    }
                }
                if(fieldSupport!=null) supportState = fieldSupport.getSupportState();
                if(supportNameSet||structureNameSet) {
                    if(fieldDBField==null) {
                        requestResult = RequestResult.failure;
                        errorMessage = "request to change suipportName or structureName but fieldDBField is null";
                        recordProcess.processContinue(this);
                        return;
                    }
                    if(supportStateRecord!=SupportState.readyForInitialize) {
                        requestResult = RequestResult.failure;
                        errorMessage = "recordState must be readyForInitilize before supportName or structureName can be changed";
                        recordProcess.processContinue(this);
                        return;
                    }
                    if(structureNameSet) {
                        PVField pvField = fieldDBField.getPVField();
                        if(pvField.getField().getType()!=Type.pvStructure) {
                            requestResult = RequestResult.failure;
                            errorMessage = "structureName change but field is not a structure";
                            recordProcess.processContinue(this);
                            return;
                        }
                        PVField pvParent = pvField.getParent();
                        if(pvParent==null) {
                            requestResult = RequestResult.failure;
                            errorMessage = "structureName change for recordType is not supported";
                            recordProcess.processContinue(this);
                            return;
                        }
                        DBStructure fieldDBStructure = (DBStructure)fieldDBField;
                        DBDStructure dbdStructure = dbd.getStructure(structureNameDesired);
                        if(dbdStructure==null) {
                            requestResult = RequestResult.failure;
                            errorMessage = "structureName change but structure " + structureNameDesired + " not in DBD database";
                            recordProcess.processContinue(this);
                            return;
                        }
                        Structure structure = fieldCreate.createStructure(
                                pvField.getField().getFieldName(),
                                dbdStructure.getStructureName(),
                                dbdStructure.getFields());
                        PVStructure newPVField = (PVStructure)pvDataCreate.createPVField(pvParent, structure);
                        fieldDBStructure.replacePVField(newPVField);
                        fieldDBField = recordDBRecord.findDBField(newPVField);
                    }
                    if(supportNameSet) {
                        fieldDBField.setSupportName(supportNameDesired);
                        SupportCreationFactory.createSupport(fieldDBField);
                    }
                }
                if(fieldDBField!=null) {
                    if(fieldDBField.getPVField().getField().getType()==Type.pvStructure) {
                        PVStructure pvStructure = (PVStructure)fieldDBField.getPVField();
                        structureNameNew = pvStructure.getStructure().getStructureName();
                    }
                    supportNameNew = fieldDBField.getSupportName();
                }
                recordProcess.processContinue(this);
            } finally {
                recordDBRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            if(requestResult!=RequestResult.success) {
                pvMessage.put(errorMessage);
                dbMessage.postPut();
            }
            if(traceSetValue) {
                traceSetValuePVBoolean.put(false);
                traceSetValueDBField.postPut();
            }
            boolean prevValue = traceValuePVBoolean.get();
            if(prevValue!=traceNewValue) {
                traceValuePVBoolean.put(traceNewValue);
                traceValueDBField.postPut();
            }
            if(enableSetValue) {
                enableSetValuePVBoolean.put(false);
                enableSetValueDBField.postPut();
            }
            prevValue = enableValuePVBoolean.get();
            if(prevValue!=enableNewValue) {
                enableValuePVBoolean.put(enableNewValue);
                enableValueDBField.postPut();
            }
            if(supportStateRecordCommand!=SupportStateCommand.idle) {
                supportStateCommandRecordPVInt.put(0);
                supportStateCommandRecordDBField.postPut();
            }
            if(supportStateRecord!=null) {
                int currentIndex = supportStateRecordPVInt.get();
                int newIndex = supportStateRecord.ordinal();
                if(currentIndex!=newIndex) {
                    supportStateRecordPVInt.put(newIndex);
                    supportStateRecordDBField.postPut();
                }
            }
            if(fieldNameSet) {
                fieldNameSetPVBoolean.put(false);
                fieldNameSetDBField.postPut();
            }
            if(fieldNameNew!=null) {
                String now = fieldNamePVString.get();
                if(now==null || !now.equals(fieldNameNew)) {
                    fieldNamePVString.put(fieldNameNew);
                    fieldNameDBField.postPut();
                }
            }
            if(supportStateCommand!=SupportStateCommand.idle) {
                supportStateCommandPVInt.put(0);
                supportStateCommandDBField.postPut();
            }
            if(supportState!=null) {
                int currentIndex = supportStatePVInt.get();
                int newIndex = supportState.ordinal();
                if(currentIndex!=newIndex) {
                    supportStatePVInt.put(newIndex);
                    supportStateDBField.postPut();
                }
            }
            if(supportNameSet) {
                supportNameSetPVBoolean.put(false);
                supportNameSetDBField.postPut();
            }
            if(supportNameNew!=null) {
                String now = supportNamePVString.get();
                if(now==null || !now.equals(supportNameNew)) {
                    supportNamePVString.put(supportNameNew);
                    supportNameDBField.postPut();
                }
            }
            if(structureNameSet) {
                structureNameSetPVBoolean.put(false);
                structureNameSetDBField.postPut();
            }
            if(structureNameNew!=null) {
                String now = structureNamePVString.get();
                if(now==null || !now.equals(structureNameNew)) {
                    structureNamePVString.put(structureNameNew);
                    structureNameDBField.postPut();
                }
            }
            
            supportProcessRequester.supportProcessDone(requestResult);
        }
        
        private void waitForState(SupportState supportState) {
            while(supportState!=recordRecordProcess.getSupportState()) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        }

        private enum SupportStateCommand {
            idle, initialize, start, stop, uninitialize;
            
            public static SupportStateCommand getSupportStateCommand(int value) {
                switch(value) {
                case 0: return SupportStateCommand.idle;
                case 1: return SupportStateCommand.initialize;
                case 2: return SupportStateCommand.start;
                case 3: return SupportStateCommand.stop;
                case 4: return SupportStateCommand.uninitialize;
                }
                throw new IllegalArgumentException("SupportStateCommand getSupportStateCommand("
                    + ((Integer)value).toString() + ") is not a valid SupportStateCommand");
            }
            
            private static final String[] supportStateCommandChoices = {
                "idle", "initialize", "start", "stop", "uninitialize"
            };
            /**
             * Convenience method for code that accesses a supportStateCommand structure.
             * @param dbField A field which is potentially a supportStateCommand structure.
             * @return The Enumerated interface only if dbField has an Enumerated interface and defines
             * the supportStateCommand choices.
             */
            public static Enumerated getSupportStateCommand(DBField dbField) {
                PVField pvField = dbField.getPVField();
                if(pvField.getField().getType()!=Type.pvStructure) {
                    pvField.message("field is not a structure", MessageType.error);
                    return null;
                }
                DBStructure dbStructure = (DBStructure)dbField;
                Create create = dbStructure.getCreate();
                if(create==null || !(create instanceof Enumerated)) {
                    pvField.message("interface Enumerated not found", MessageType.error);
                    return null;
                }
                Enumerated enumerated = (Enumerated)create;
                PVStringArray pvChoices = enumerated.getChoicesField();
                int len = pvChoices.getLength();
                if(len!=supportStateCommandChoices.length) {
                    pvField.message("not an supportStateCommand structure", MessageType.error);
                    return null;
                }
                StringArrayData data = new StringArrayData();
                pvChoices.get(0, len, data);
                String[] choices = data.data;
                for (int i=0; i<len; i++) {
                    if(!choices[i].equals(supportStateCommandChoices[i])) {
                        pvField.message("not an supportStateCommand structure", MessageType.error);
                        return null;
                    }
                }
                pvChoices.setMutable(false);
                return enumerated;
            }
        }

    }
}
