/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.rpc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.database.PVDatabase;
import org.epics.ioc.database.PVDatabaseFactory;
import org.epics.ioc.database.PVRecord;
import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.misc.TimeFunction;
import org.epics.pvData.misc.TimeFunctionFactory;
import org.epics.pvData.misc.TimeFunctionRequester;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;

/**
 * Support for remotely get a list of records.
 * @author mrk
 *
 */
public class RecordShowFactory {
    /**
     * Create support for showing records.
     * @param pvRecordStructure The field supported.
     * @return An interface to the support or null if the supportName was not "linkArray".
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        return new RecordShowImpl(pvRecordStructure);
    }
    
    private static final String supportName = "org.epics.ioc.rpc.recordShow";
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final String newLine = String.format("%n");
    private static final Executor executor = ExecutorFactory.create("recordShowFactory",ThreadPriority.low);
    
    private static class RecordShowImpl extends AbstractSupport implements Runnable,ProcessContinueRequester
    {
        private final ExecutorNode executorNode = executor.createNode(this);
        private final PVRecordStructure pvRecordStructure;
        private RecordProcess thisRecordProcess = null;
        private SupportProcessRequester supportProcessRequester = null;
        private PVString pvRecordName = null;
        private PVRecord pvRecord = null;
        private RecordProcess recordProcess = null;
        private Enumerated command = null;
        private PVString pvResult = null;
        private StringBuilder stringBuilder = new StringBuilder();
        
        private RecordShowImpl(PVRecordStructure pvRecordStructure) {
            super(RecordShowFactory.supportName,pvRecordStructure);
            this.pvRecordStructure = pvRecordStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            thisRecordProcess = pvRecordStructure.getPVRecord().getRecordProcess();
            PVStructure pvStructure = pvRecordStructure.getPVStructure();
            pvRecordName = pvStructure.getStringField("arguments.recordName");
            if(pvRecordName==null) return;
            PVStructure pvTemp = pvStructure.getStructureField("arguments.command");
            if(pvTemp==null) return;
            command = EnumeratedFactory.getEnumerated(pvTemp);
            if(command==null) {
                super.message("arguments.command is not enumerated", MessageType.error);
                return;
            }
            pvResult = pvStructure.getStringField("result.value");
            if(pvResult==null) return;
            super.initialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            this.supportProcessRequester = supportProcessRequester;
            executor.execute(executorNode);
           
        }
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            pvRecord = masterPVDatabase.findRecord(pvRecordName.get());
            if(pvRecord==null) {
                pvResult.put("record not found");
            } else {
                recordProcess = pvRecord.getRecordProcess();
                if(recordProcess==null) {
                    pvResult.put("recordProcess not found");
                } else {
                    String cmd = command.getChoice();
                    if(cmd.equals("showState")) {
                        showState();
                    } else if(cmd.equals("setTraceOn")) {
                        recordProcess.setTrace(true);
                        pvResult.put("traceOn");
                    } else if(cmd.equals("setTraceOff")) {
                        recordProcess.setTrace(false);
                        pvResult.put("traceOff");
                    } else if(cmd.equals("timeProcess")) {
                        timeProcess();
                    } else if(cmd.equals("setEnableOn")) {
                        recordProcess.setEnabled(true);
                        pvResult.put("enabled");
                    } else if(cmd.equals("setEnableOff")) {
                        recordProcess.setEnabled(false);
                        pvResult.put("disabled");
                    } else if(cmd.equals("releaseProcessor")) {
                        recordProcess.forceInactive();
                        pvResult.put("releaseRecordProcessRequester");
                    }
                }
            }
            thisRecordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
         */
        @Override
        public void processContinue() {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        private void showState() {
        	PVBoolean pvBoolean= pvRecord.getPVRecordStructure().getPVStructure().getBooleanField("scan.singleProcessRequester");
            boolean singleProcessRequester = ((pvBoolean==null) ? false : pvBoolean.get());
            String processRequesterName = recordProcess.getRecordProcessRequesterName();
            SupportState supportState = recordProcess.getSupportState();
            boolean isActive = recordProcess.isActive();
            boolean isEnabled = recordProcess.isEnabled();
            boolean isTrace = recordProcess.isTrace();
            String alarmSeverity = null;
            PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("alarm.severity.choice");
            if(pvField!=null) alarmSeverity = pvField.toString();
            String alarmMessage = null;
            pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("alarm.message");
            if(pvField!=null) alarmMessage = pvField.toString();
            stringBuilder.setLength(0);
            stringBuilder.append(pvRecord.getRecordName());
            stringBuilder.append(newLine);
            stringBuilder.append("  singleProcessRequester ");
            stringBuilder.append(Boolean.toString(singleProcessRequester));
            stringBuilder.append(" processRequester ");
            stringBuilder.append(processRequesterName);
            stringBuilder.append(" supportState ");
            stringBuilder.append(supportState.name());
            stringBuilder.append(newLine);
            stringBuilder.append("  isActive ");
            stringBuilder.append(Boolean.toString(isActive));
            stringBuilder.append(" isEnabled ");
            stringBuilder.append(Boolean.toString(isEnabled));
            stringBuilder.append(" isTrace ");
            stringBuilder.append(Boolean.toString(isTrace));
            stringBuilder.append(newLine);
            stringBuilder.append("  alarmSeverity ");
            stringBuilder.append(alarmSeverity);
            stringBuilder.append(" alarmMessage ");
            stringBuilder.append(alarmMessage);
            stringBuilder.append(newLine);
            pvResult.put(stringBuilder.toString());
        }
        
        private void timeProcess() {
            stringBuilder.setLength(0);
            TimeProcess timeProcess = new TimeProcess();
            timeProcess.doIt();
            pvResult.put(stringBuilder.toString());
        }
        
        private class TimeProcess 
        {   
            private RecordProcess recordProcess = null;
            private ProcessToken processToken = null;
            private ProcessIt processIt = null;


            private TimeProcess() {
                processIt = new ProcessIt();
            }

            private void doIt() {
                recordProcess = pvRecord.getRecordProcess();
                processToken = recordProcess.requestProcessToken(processIt);
                if(processToken==null) {
                    stringBuilder.append("could not process the record");
                    return;
                }
                processIt.setToken(processToken);
                TimeFunction timeFunction = TimeFunctionFactory.create(processIt);
                double perCall = timeFunction.timeCall();
                stringBuilder.append(" records/second=");
                stringBuilder.append(Double.toString(1.0/perCall));
                recordProcess.releaseProcessToken(processToken);
            }
            
            private class ProcessIt implements TimeFunctionRequester, RecordProcessRequester {
                private TimeStamp timeStamp = TimeStampFactory.create(0,0);
                private ReentrantLock lock = new ReentrantLock();
                private Condition waitProcessDone = lock.newCondition();
                private boolean processDone = false;
                private ProcessToken processToken = null;

                private ProcessIt() {
                    long start = System.currentTimeMillis();
                    timeStamp.put(start);
                }
                
                private void setToken(ProcessToken processToken) {
                	this.processToken = processToken;
                }

                /* (non-Javadoc)
                 * @see org.epics.pvData.misc.TimeFunctionRequester#function()
                 */
                public void function() {
                    processDone = false;
                    recordProcess.queueProcessRequest(processToken);
                    lock.lock();
                    try {
                        while(!processDone) {
                            try {
                                waitProcessDone.await();
                            } catch(InterruptedException e) {}
                        }
                    }finally {
                        lock.unlock();
                    }
                }
                @Override
				public void becomeProcessor() {
                	recordProcess.process(processToken,false, timeStamp);
				}

				@Override
				public void canNotProcess(String reason) {
					message("can not process " + reason,MessageType.error);
					lock.lock();
                    try {
                        processDone = true;
                        waitProcessDone.signal();
                    } finally {
                        lock.unlock();
                    }
				}

				@Override
				public void lostRightToProcess() {
					throw new IllegalStateException(" lost right to process");
				}

				/* (non-Javadoc)
                 * @see org.epics.pvData.pv.Requester#getRequesterName()
                 */
                public String getRequesterName() {
                    return thisRecordProcess.getRecordProcessRequesterName();
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
                 */
                public void message(final String message, final MessageType messageType) {
                    stringBuilder.append(message);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
                 */
                public void recordProcessComplete() {
                    lock.lock();
                    try {
                        processDone = true;
                        waitProcessDone.signal();
                    } finally {
                        lock.unlock();
                    }
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
                 */
                public void recordProcessResult(RequestResult requestResult) {
                    // nothing to do
                }
            }
        }
    }
}
