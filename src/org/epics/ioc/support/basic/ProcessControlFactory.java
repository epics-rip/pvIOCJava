/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.install.IOCDatabase;
import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.StringArrayData;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class ProcessControlFactory {
    /**
     * Create support for an array of calcArg structures.
     * @param pvStructure The processControlStructure
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVStructure pvStructure) {
        return new ProcessControlImpl(pvStructure);
    }
    
    private static final String supportName = "processControl";
    private static final String emptyString = "";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final IOCDatabase supportDatabase = IOCDatabaseFactory.get(masterPVDatabase);
    
    
    private static class ProcessControlImpl extends AbstractSupport
    implements Runnable,ProcessContinueRequester
    {
        
        private Executor executor = ExecutorFactory.create(ProcessControlFactory.supportName, ThreadPriority.lowest);
        private ExecutorNode executorNode = null;
        private RecordProcess recordProcess = null;
        
        private PVString pvMessage = null;
        private String message = emptyString;
        
        private PVString pvRecordName = null;
        private String recordName = null;
        private String recordNamePrevious = emptyString;
        
        private PVBoolean pvTrace = null;
        private boolean trace = false;
        
        private PVBoolean pvEnable = null;
        private boolean enable = false;
        
        private Enumerated supportStateEnumerated = null;
        private PVInt supportStatePVInt = null;
        private Enumerated supportStateCommandEnumerated = null;
        private PVInt supportStateCommandPVInt = null;
        
        private PVRecord targetPVRecord = null;
        private RecordProcess targetRecordProcess = null;
        
        private RequestResult requestResult = null;
        private SupportProcessRequester supportProcessRequester = null;

        private SupportStateCommand supportStateCommand = null; 
        private SupportState supportState = null;
        
        
        private ProcessControlImpl(PVStructure pvStructure) {
            super(ProcessControlFactory.supportName,pvStructure);
            executorNode = executor.createNode(this); 
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        @Override
        public void initialize(LocateSupport recordSupport) {
            PVStructure pvProcessControl = (PVStructure)super.getPVField();
            recordProcess = recordSupport.getRecordProcess();
            pvMessage = pvProcessControl.getStringField("message");
            if(pvMessage==null) return;
            pvRecordName = pvProcessControl.getStringField("recordName");
            if(pvRecordName==null) return;
            pvTrace = pvProcessControl.getBooleanField("trace");
            if(pvTrace==null) return;
            pvEnable = pvProcessControl.getBooleanField("enable");
            if(pvEnable==null) return;
            PVStructure pvStructure = pvProcessControl.getStructureField("supportState");
            if(pvStructure==null) return;
            supportStateEnumerated = SupportState.getSupportState(pvStructure);
            if(supportStateEnumerated==null) return;
            supportStatePVInt = supportStateEnumerated.getIndex();
            pvStructure = pvProcessControl.getStructureField("supportStateCommand");
            if(pvStructure==null) return;
            supportStateCommandEnumerated = SupportStateCommand.getSupportStateCommand(pvStructure);
            if(supportStateCommandEnumerated==null) return;
            supportStateCommandPVInt = supportStateCommandEnumerated.getIndex();
            super.initialize(recordSupport);
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
                pvMessage.postPut();
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
            executor.execute(executorNode);
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            if(!recordName.equals(recordNamePrevious)) {
                targetPVRecord = masterPVDatabase.findRecord(recordName);
                if(targetPVRecord==null) {
                    requestResult = RequestResult.failure;
                    message = "recordName " + recordName + " not found";
                    recordProcess.processContinue(this);
                    return;
                }
                targetRecordProcess = supportDatabase.getLocateSupport(targetPVRecord).getRecordProcess();
                if(targetRecordProcess==null) {
                    requestResult = RequestResult.failure;
                    message = "recordProcess for " + "recordName " + recordName + " not found";
                    targetPVRecord = null;
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
            if(targetPVRecord==null) {
                message = "not connected to a record";
                recordProcess.processContinue(this);
                return;
            }
            targetPVRecord.lock();
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
                        targetRecordProcess.start(null); break;
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
                targetPVRecord.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            pvMessage.put(message);
            pvMessage.postPut();
            if(trace!=pvTrace.get()) {
                pvTrace.put(trace);
                pvTrace.postPut();
            }
            if(enable!=pvEnable.get()) {
                pvEnable.put(enable);
                pvEnable.postPut();
            }
            
            if(supportState!=null) {
                int index = supportState.ordinal();
                if(index!=supportStatePVInt.get()) {
                    supportStatePVInt.put(index);
                    supportStatePVInt.postPut();
                }
            }
            if(supportStateCommandPVInt.get()!=0) {
                supportStateCommandPVInt.put(0);
                supportStateCommandPVInt.postPut();
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
             * @param pvField A field which is potentially a supportStateCommand structure.
             * @return The Enumerated interface only if pvField has an Enumerated interface and defines
             * the supportStateCommand choices.
             */
            public static Enumerated getSupportStateCommand(PVField pvField) {
                Enumerated enumerated = EnumeratedFactory.getEnumerated(pvField);
                if(enumerated==null) {
                    pvField.message("not an enumerated structure", MessageType.error);
                    return null;
                }
                PVStringArray pvChoices = enumerated.getChoices();
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
