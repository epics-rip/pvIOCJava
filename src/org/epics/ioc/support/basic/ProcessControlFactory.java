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
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
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
    
    private static final String supportName = "processControl";
    
    
    private static class ProcessControlImpl extends AbstractSupport
    implements Runnable,ProcessContinueRequester
    {
        private static final String emptyString = "";
        private IOCExecutor iocExecutor = IOCExecutorFactory.create(ProcessControlFactory.supportName, ScanPriority.lowest);
        private IOCDB masterIOCDB = IOCDBFactory.getMaster();
        private RecordProcess recordProcess = null;
        
        private PVString pvMessage = null;
        private DBField dbMessage = null;
        private String message = emptyString;
        
        private PVString pvRecordName = null;
        private String recordName = null;
        private String recordNamePrevious = emptyString;
        
        private PVBoolean pvTrace = null;
        private boolean trace = false;
        private DBField dbTrace = null;
        
        private PVBoolean pvEnable = null;
        private boolean enable = false;
        private DBField dbEnable = null;
        
        private Enumerated supportStateEnumerated = null;
        private PVInt supportStatePVInt = null;
        private DBField supportStateDBField = null;
        private Enumerated supportStateCommandEnumerated = null;
        private PVInt supportStateCommandPVInt = null;
        private DBField supportStateCommandDBField = null;
        
        private DBRecord targetDBRecord = null;
        private RecordProcess targetRecordProcess = null;
        
        private RequestResult requestResult = null;
        private SupportProcessRequester supportProcessRequester = null;

        private SupportStateCommand supportStateCommand = null; 
        private SupportState supportState = null;
        
        
        private ProcessControlImpl(DBStructure dbField) {
            super(ProcessControlFactory.supportName,dbField);
            
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
            pvRecordName = pvRecord.getStringField("recordName");
            if(pvRecordName==null) return;
            pvTrace = pvRecord.getBooleanField("trace");
            if(pvTrace==null) return;
            dbTrace = dbRecord.findDBField(pvTrace);
            pvEnable = pvRecord.getBooleanField("enable");
            if(pvEnable==null) return;
            dbEnable = dbRecord.findDBField(pvEnable);
            PVStructure pvStructure = pvRecord.getStructureField("supportState", "supportState");
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
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            message = emptyString;
            recordName = pvRecordName.get();
            if(recordName==null || recordName.equals("")) {
                pvMessage.put("recordName is null");
                dbMessage.postPut();
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            requestResult = RequestResult.success;
            this.supportProcessRequester = supportProcessRequester;
            trace = pvTrace.get();
            enable = pvEnable.get();
            supportState = null;
            supportStateCommand = SupportStateCommand.getSupportStateCommand(
                    supportStateCommandPVInt.get());
            iocExecutor.execute(this);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if(!recordName.equals(recordNamePrevious)) {
                targetDBRecord = masterIOCDB.findRecord(recordName);
                if(targetDBRecord==null) {
                    requestResult = RequestResult.failure;
                    message = "recordName " + recordName + " not found";
                    recordProcess.processContinue(this);
                    return;
                }
                targetRecordProcess = targetDBRecord.getRecordProcess();
                if(targetRecordProcess==null) {
                    requestResult = RequestResult.failure;
                    message = "recordProcess for " + "recordName " + recordName + " not found";
                    targetDBRecord = null;
                    recordProcess.processContinue(this);
                    return;
                }
                recordNamePrevious = recordName;
                trace = targetRecordProcess.isTrace();
                enable = targetRecordProcess.isEnabled();
                supportState = targetRecordProcess.getSupportState();
                recordProcess.processContinue(this);
                return;
            }
            if(targetDBRecord==null) {
                message = "not connected to a record";
                recordProcess.processContinue(this);
                return;
            }
            targetDBRecord.lock();
            try {
                targetRecordProcess.setTrace(trace);
                targetRecordProcess.setEnabled(enable);
                if(supportStateCommand!=SupportStateCommand.idle) {
                    supportState = targetRecordProcess.getSupportState();
                    SupportState desiredState = null;
                    switch(supportStateCommand) {
                    case initialize:
                        desiredState = SupportState.readyForStart;
                        if(supportState!=SupportState.readyForInitialize) {
                            targetRecordProcess.uninitialize();
                        }
                        targetRecordProcess.initialize();break;
                    case start:
                        desiredState = SupportState.ready;
                        if(supportState!=SupportState.readyForStart) {
                            if(supportState==SupportState.ready) {
                                targetRecordProcess.stop();
                            } else if(supportState==SupportState.readyForInitialize) {
                                targetRecordProcess.initialize();
                            }
                            supportState = targetRecordProcess.getSupportState();
                            if(supportState!=SupportState.readyForStart) {
                                requestResult = RequestResult.failure;
                                message = "support is not readyForStart";
                                recordProcess.processContinue(this);
                                return;
                            }
                        }
                        targetRecordProcess.start(); break;
                    case stop:
                        desiredState = SupportState.readyForStart;
                        if(supportState!=SupportState.ready) {
                            requestResult = RequestResult.failure;
                            message = "support is not ready";
                            recordProcess.processContinue(this);
                            return;
                        }
                        targetRecordProcess.stop(); break;
                    case uninitialize:
                        desiredState = SupportState.readyForInitialize;
                        targetRecordProcess.uninitialize(); break;
                        default:
                            throw new IllegalArgumentException("Logic error");
                    }
                    waitForState(desiredState);
                }
                supportState = targetRecordProcess.getSupportState();
                recordProcess.processContinue(this);
            } finally {
                targetDBRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            pvMessage.put(message);
            dbMessage.postPut();
            if(trace!=pvTrace.get()) {
                pvTrace.put(trace);
                dbTrace.postPut();
            }
            if(enable!=pvEnable.get()) {
                pvEnable.put(enable);
                dbEnable.postPut();
            }
            
            if(supportState!=null) {
                int index = supportState.ordinal();
                if(index!=supportStatePVInt.get()) {
                    supportStatePVInt.put(index);
                    supportStateDBField.postPut();
                }
            }
            if(supportStateCommandPVInt.get()!=0) {
                supportStateCommandPVInt.put(0);
                supportStateCommandDBField.postPut();
            }
            supportProcessRequester.supportProcessDone(requestResult);
        }
        
        private void waitForState(SupportState supportState) {
            int ntimes = 0;
            while(supportState!=targetRecordProcess.getSupportState()) {
                if(ntimes++ > 2000) {
                    message = "Did not reach desired state";
                    return;
                }
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
