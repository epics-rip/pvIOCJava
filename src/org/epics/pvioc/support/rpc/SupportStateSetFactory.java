/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.rpc;

import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.property.PVEnumerated;
import org.epics.pvdata.property.PVEnumeratedFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.ProcessContinueRequester;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;

/**
 * Support for an array of calcArg structures.
 * @author mrk
 *
 */
public class SupportStateSetFactory {
    /**
     * Create support for showing the support state of a record.
     * @param pvRecordStructure The supported field.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new SupportStateSetImpl(pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.rpc.supportStateSet";
    private static final String emptyString = "";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    
    private static class SupportStateSetImpl extends AbstractSupport
    implements Runnable,ProcessContinueRequester
    {
        private final Executor executor = ExecutorFactory.create(SupportStateSetFactory.supportName, ThreadPriority.lowest);
        private final PVRecordStructure pvRecordStructure;
        private final ExecutorNode executorNode;
        private RecordProcess recordProcess = null;
        
        private PVString pvMessage = null;
        private String message = emptyString;
        
        private PVString pvRecordName = null;
        private String recordName = null;
        private String recordNamePrevious = emptyString;
         
        private PVEnumerated supportStateEnumerated = null;
        private PVEnumerated supportStateCommandEnumerated = PVEnumeratedFactory.create();
        
        private PVRecord targetPVRecord = null;
        private RecordProcess targetRecordProcess = null;
        
        private RequestResult requestResult = null;
        private SupportProcessRequester supportProcessRequester = null;

        private SupportStateCommand supportStateCommand = null; 
        private SupportState supportState = null;
        
        
        private SupportStateSetImpl(PVRecordStructure pvRecordStructure) {
            super(SupportStateSetFactory.supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
            executorNode = executor.createNode(this); 
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            PVStructure pvSupportStateSet = pvRecordStructure.getPVStructure();
            recordProcess = pvRecordStructure.getPVRecord().getRecordProcess();
            pvRecordName = pvSupportStateSet.getStringField("arguments.recordName");
            if(pvRecordName==null) return;
            PVStructure pvStructure = pvSupportStateSet.getStructureField("arguments.supportStateCommand");
            if(pvStructure==null) return;
            if(!SupportStateCommand.checkSupportStateCommand(supportStateCommandEnumerated,pvStructure)) return;
            pvMessage = pvSupportStateSet.getStringField("result.message");
            if(pvMessage==null) return;
            pvStructure = pvSupportStateSet.getStructureField("result.supportState");
            if(pvStructure==null) return;
            supportStateEnumerated = SupportState.getSupportState(pvStructure);
            if(supportStateEnumerated==null) return;
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            message = emptyString;
            recordName = pvRecordName.get();
            if(recordName==null || recordName.equals("")) {
                pvMessage.put("recordName is null");
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            requestResult = RequestResult.success;
            this.supportProcessRequester = supportProcessRequester;
            supportState = null;
            supportStateCommand = SupportStateCommand.getSupportStateCommand(supportStateEnumerated.getIndex());
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
                targetRecordProcess = targetPVRecord.getRecordProcess();
                if(targetRecordProcess==null) {
                    requestResult = RequestResult.failure;
                    message = "recordProcess for " + "recordName " + recordName + " not found";
                    targetPVRecord = null;
                    recordProcess.processContinue(this);
                    return;
                }
                recordNamePrevious = recordName;
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
         * @see org.epics.pvioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            pvMessage.put(message);
            if(supportState!=null) {
                int index = supportState.ordinal();
                if(index!=supportStateEnumerated.getIndex()) {
                    supportStateEnumerated.setIndex(index);
                }
            }
            if(supportStateCommandEnumerated.getIndex()!=0) {
                supportStateCommandEnumerated.setIndex(0);
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
            public static boolean checkSupportStateCommand(PVEnumerated enumerated,PVField pvField) {
                if(!enumerated.attach(pvField)) {
                    pvField.message("not an enumerated structure", MessageType.error);
                    return false;
                }
                String[] choices = enumerated.getChoices();
                int len = choices.length;
                if(len!=supportStateCommandChoices.length) {
                    pvField.message("not an supportStateCommand structure", MessageType.error);
                    return false;
                }
                
                for (int i=0; i<len; i++) {
                    if(!choices[i].equals(supportStateCommandChoices[i])) {
                        pvField.message("not an supportStateCommand structure", MessageType.error);
                        return false;
                    }
                }
                PVStructure pvStruct = (PVStructure)pvField;
                PVArray pvArray = pvStruct.getScalarArrayField("choices", ScalarType.pvString);
                pvArray.setImmutable();
                return true;
            }
        }

    }
}
