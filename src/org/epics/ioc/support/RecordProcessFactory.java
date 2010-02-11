/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.AfterStartFactory;
import org.epics.ioc.install.AfterStartNode;
import org.epics.ioc.install.AfterStartRequester;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanField;
import org.epics.ioc.util.ScanFieldFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Structure;



/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class RecordProcessFactory {
    
    /**
     * Create RecordProcess for a record instance.
     * @param pvRecord The record instance.
     * @return The interface for the newly created RecordProcess.
     */
    static public RecordProcess createRecordProcess(LocateSupport locateSupport,PVRecord pvRecord) {
        return new RecordProcessImpl(locateSupport,pvRecord);
    }
    
    static private class Token implements ProcessToken 
    {
    	private RecordProcessRequester recordProcessRequester;
    	
    	Token(RecordProcessRequester recordProcessRequester) {
    		this.recordProcessRequester = recordProcessRequester;
    	}
    }
    
    static private class RecordProcessImpl
    implements RecordProcess,SupportProcessRequester,PVListener
    {
		private boolean trace = false;
        private PVRecord pvRecord;
        private LocateSupport locateSupport;
        private String recordProcessSupportName = null;
        private boolean enabled = true;
        private Support fieldSupport = null;
        private ScanField  scanField = null;
        private PVBoolean pvProcessAfterStart = null;
        private Support scanSupport = null;
        private PVBoolean pvSingleProcessRequester = null;
        private boolean singleProcessRequester = false;
        
        private ArrayList<Token> tokenList = new ArrayList<Token>();
        private ArrayList<Token> queueRequestList = new ArrayList<Token>();
        private boolean leaveActive = false;
        private Token activeToken = null;
        private boolean recordProcessActive = false;
        private List<ProcessCallbackRequester> processCallbackRequesterList =
            new ArrayList<ProcessCallbackRequester>();
        private boolean callStopAfterActive = false;
        private boolean callUninitializeAfterActive = false;
        private boolean processIsComplete = false;
        private boolean processCompleteDone = false;
        private boolean callRecordProcessComplete = false;
        private RequestResult requestResult = null;
        
        
        private TimeStamp timeStamp = null;
        
        private RecordProcessImpl(LocateSupport locateSupport,PVRecord pvRecord) {
            this.locateSupport = locateSupport;
            this.pvRecord = pvRecord;
            locateSupport.setRecordProcess(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isEnabled()
         */
        @Override
        public boolean isEnabled() {
            return enabled;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#setEnabled(boolean)
         */
        @Override
        public boolean setEnabled(boolean value) {
            pvRecord.lock();
            try {
                boolean oldValue = enabled;
                enabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isActive()
         */
        @Override
        public boolean isActive() {
            return (activeToken==null) ? false : true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#getRecord()
         */
        @Override
        public PVRecord getRecord() {
            return pvRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#isTrace()
         */
        @Override
        public boolean isTrace() {
            return trace;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#setTrace(boolean)
         */
        @Override
        public boolean setTrace(boolean value) {
            pvRecord.lock();
            try {
                boolean oldValue = trace;
                trace = value;
                if(value!=oldValue) return true;
                return false;
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#getSupportState()
         */
        @Override
        public SupportState getSupportState() {
            pvRecord.lock();
            try {
                return fieldSupport.getSupportState();
            } finally {
                pvRecord.unlock();
            }
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#initialize()
         */
        @Override
        public void initialize() {
            pvRecord.lock();
            try {
                if(trace) traceMessage(" initialize");
                PVStructure pvStructure = pvRecord.getPVStructure();
                recordProcessSupportName = "recordProcess(" + pvRecord.getRecordName() + ")";
                fieldSupport = locateSupport.getSupport(pvRecord.getPVStructure());
                if(fieldSupport==null) {
                    throw new IllegalStateException(
                        pvRecord.getRecordName() + " has no support");
                }
                PVField[] pvFields = pvStructure.getPVFields();
                Structure structure = (Structure)pvRecord.getPVStructure().getField();
                int index;
                index = structure.getFieldIndex("timeStamp");
                if(index>=0) {
                    timeStamp = TimeStampFactory.getTimeStamp((PVStructure)pvFields[index]);
                }
                index = structure.getFieldIndex("scan");
                if(index>=0) {
                    scanSupport = locateSupport.getSupport(pvFields[index]);
                    scanField = ScanFieldFactory.create(pvRecord);
                    if(scanField!=null) {
                    	pvSingleProcessRequester = scanField.getSingleProcessRequesterPV();
                        pvProcessAfterStart = scanField.getProcessAfterStartPV();
                    }
                }
                
                fieldSupport.initialize(locateSupport);
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#start()
         */
        @Override
        public void start(AfterStart afterStart) {
        	pvRecord.lock();
        	try {
        		if(trace) traceMessage(" start");
        		fieldSupport.start(afterStart);
        		if(scanSupport!=null) scanSupport.start(afterStart);
        		if(pvSingleProcessRequester!=null) {
                	singleProcessRequester = pvSingleProcessRequester.get();
                	pvRecord.registerListener(this);
                	pvSingleProcessRequester.getPVRecordField().addListener(this);
                }
        		if(!singleProcessRequester && pvProcessAfterStart!=null) {
        			if(pvProcessAfterStart.get()) {
        				new ProcessAfterStart(this,afterStart);
        			}
        		}
        	} finally {
        		pvRecord.unlock();
        	}
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#stop()
         */
        @Override
        public void stop() {
            pvRecord.lock();
            try {
                if(activeToken!=null) {
                    callStopAfterActive = true;
                    if(trace) traceMessage("stop delayed because active");
                    return;
                }
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                fieldSupport.stop();
                pvRecord.removeEveryListener();
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcess#uninitialize()
         */
        @Override
        public void uninitialize() {
            pvRecord.lock();
            try {
                if(activeToken!=null) {
                    callUninitializeAfterActive = true;
                    if(trace) traceMessage("uninitialize delayed because active");
                    return;
                }
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                fieldSupport.uninitialize();
            } finally {
                pvRecord.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#requestProcessToken(org.epics.ioc.support.RecordProcessRequester)
         */
        @Override
		public ProcessToken requestProcessToken(RecordProcessRequester recordProcessRequester)
        {
        	if(recordProcessRequester==null) {
                throw new IllegalArgumentException("must implement recordProcessRequester");
            }
        	pvRecord.lock();
        	try {
        		if(singleProcessRequester && tokenList.size()!=0) return null;
        		for(int i=0; i<tokenList.size(); i++) {
        			Token token = tokenList.get(i);
        			if(token.recordProcessRequester==recordProcessRequester) {
        				throw new IllegalStateException("already have token");
        			}
        		}
        		Token token = new Token(recordProcessRequester);
        		tokenList.add(token);
        		return token;
        	} finally {
				pvRecord.unlock();
			}
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcess#releaseProcessToken(org.epics.ioc.support.ProcessToken)
		 */
		@Override
		public void releaseProcessToken(ProcessToken processToken) {
			pvRecord.lock();
			try {
				Token token = (Token)processToken;
				int index = tokenList.indexOf(token);
				if(index<0) return;
				tokenList.remove(index);
				return;
			} finally {
				pvRecord.unlock();
			}
		}
		/* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#queueProcessRequest(org.epics.ioc.support.ProcessToken)
         */
        @Override
        public void queueProcessRequest(ProcessToken processToken) {
        	Token token = (Token) processToken;
            RecordProcessRequester recordProcessRequester = token.recordProcessRequester;
        	pvRecord.lock();
        	try {
        		SupportState supportState = fieldSupport.getSupportState();
        		if (supportState != SupportState.ready) {
        			recordProcessRequester.canNotProcess("record support is not ready");
        			return;
        		}
        		if (!isEnabled()) {
        			recordProcessRequester.canNotProcess("record is disabled");
        			return;
        		}
        		if(token==activeToken) {
        			recordProcessRequester.canNotProcess("record already active");
        			return;
        		}
        		if(activeToken!=null) {
        			queueRequestList.add(token);
        			return;
        		}
        		activeToken = token;
        		processIsComplete = false;
        		processCompleteDone = false;
        		pvRecord.beginGroupPut();
        	} finally {
        		pvRecord.unlock();
        	}
        	recordProcessRequester.becomeProcessor();
        }        
        /*
		 * (non-Javadoc)
		 * 
		 * @see org.epics.ioc.support.RecordProcess#getRecordProcessRequesterName()
		 */
        @Override
        public String getRecordProcessRequesterName() {
            pvRecord.lock();
            try {
                if(activeToken==null) return null;
                return activeToken.recordProcessRequester.getRequesterName();
            } finally {
                pvRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#forceInactive()
         */
        @Override
        public void forceInactive() {
            pvRecord.lock();
            try {
            	if(activeToken==null) return;
                message(
                   " forceInactive recordProcessRequester " 
                   + activeToken.recordProcessRequester.getRequesterName(),
                   MessageType.error);
                tokenList.remove(activeToken);
                activeToken = null;
            } finally {
                pvRecord.unlock();
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#process(org.epics.ioc.support.ProcessToken, boolean, org.epics.pvData.property.TimeStamp)
         */
        @Override
		public void process(ProcessToken processToken,boolean leaveActive, TimeStamp timeStamp) {
            if(processToken==null || activeToken!=processToken) {
            	throw new IllegalStateException("not the active process requester");
            }
            RecordProcessRequester recordProcessRequester;
            pvRecord.lock();
            try {
            	recordProcessRequester = activeToken.recordProcessRequester;
                if(this.timeStamp!=null) {
                    if(timeStamp==null) {
                        this.timeStamp.put(System.currentTimeMillis());
                        if(trace) traceMessage(
                                "process with system timeStamp "
                                + recordProcessRequester.getRequesterName()); 
                    } else {
                        this.timeStamp.put(timeStamp.getSecondsPastEpoch(),timeStamp.getNanoSeconds());
                        if(trace) traceMessage(
                                "process with callers timeStamp "
                                + recordProcessRequester.getRequesterName()); 
                    }
                } else {
                    if(trace) {
                        if(timeStamp==null) {
                            traceMessage("process no TimeStamp no Caller TimeStamp "
                                 + recordProcessRequester.getRequesterName()); 
                        } else {
                            traceMessage("process no TimeStamp Caller supplied TimeStamp "
                                    + recordProcessRequester.getRequesterName()); 
                        }
                    }
                }
                this.leaveActive = leaveActive;
                recordProcessActive = true;
                // NOTE: processContinue may be called before the following returns
                fieldSupport.process(this);
                recordProcessActive = false;
                if(processIsComplete && !processCompleteDone) {
                    completeProcessing();
                }
            } finally {
                pvRecord.unlock();
            }
            if(callRecordProcessComplete) {
                callRecordProcessComplete = false;
                recordProcessRequester.recordProcessComplete();
                if(!leaveActive && activeToken!=null) {
                	activeToken.recordProcessRequester.becomeProcessor();
                }
                return;
            }
            while(true) {
                ProcessCallbackRequester processCallbackRequester = null;
                synchronized(processCallbackRequesterList) {
                	if(processCallbackRequesterList.size()<1) break;
                	processCallbackRequester = processCallbackRequesterList.remove(0);
                }
                processCallbackRequester.processCallback();
            }
            return;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcess#setInactive()
         */
        @Override
		public void setInactive(ProcessToken processToken) {
        	if(processToken==null || activeToken!=processToken) {
            	throw new IllegalStateException("not the active process requester");
            }
            pvRecord.lock();
            try {
            	RecordProcessRequester recordProcessRequester = activeToken.recordProcessRequester;
                if(trace) traceMessage("setInactive " + recordProcessRequester.getRequesterName());
                if(!processIsComplete) {
                    throw new IllegalStateException("processing is not finished");
                }
                if(!processCompleteDone) {
                    throw new IllegalStateException("process complete is not done");
                }
                activeToken = null;
                if(queueRequestList.size()>0) {
                	activeToken = queueRequestList.remove(0);
                }
            } finally {
                pvRecord.unlock();
            }
            if(activeToken!=null) activeToken.recordProcessRequester.becomeProcessor();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#processContinue()
         */
        public void processContinue(ProcessContinueRequester processContinueRequester) {
        	RecordProcessRequester recordProcessRequester;
            pvRecord.lock();
            try {
                if(activeToken==null) {
                    throw new IllegalStateException(
                        "processContinue called but record "
                         + pvRecord.getRecordName()
                         + " is not active");
                }
                recordProcessRequester = activeToken.recordProcessRequester;
                if(trace) {
                    traceMessage("processContinue ");
                }
                recordProcessActive = true;
                processContinueRequester.processContinue();
                recordProcessActive = false;
                if(processIsComplete && !processCompleteDone) {
                    completeProcessing();
                }
            } finally {
                pvRecord.unlock();
            }
            if(callRecordProcessComplete) {
                callRecordProcessComplete = false;
                recordProcessRequester.recordProcessComplete();
                if(!leaveActive && activeToken!=null) {
                	activeToken.recordProcessRequester.becomeProcessor();
                }
                return;
            }
            while(true) {
                ProcessCallbackRequester processCallbackRequester = null;
                synchronized(processCallbackRequesterList) {
                	if(processCallbackRequesterList.size()<1) break;
                	processCallbackRequester = processCallbackRequesterList.remove(0);
                }
                processCallbackRequester.processCallback();
            }
            return;

        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#requestProcessCallback(org.epics.ioc.process.ProcessCallbackRequester)
         */
        public void requestProcessCallback(ProcessCallbackRequester processCallbackRequester) {
        	if(!recordProcessActive) {
        		throw new IllegalStateException("must be called from process or processContinue");
        	}
            if(activeToken==null) {
                throw new IllegalStateException("requestProcessCallback called but record is not active");
            }
            if(processIsComplete) {
                throw new IllegalStateException("requestProcessCallback called but processIsComplete");
            }
            if(trace) {
                traceMessage("requestProcessCallback " + processCallbackRequester.getRequesterName());
            }
            synchronized(processCallbackRequesterList) {
            	if(processCallbackRequesterList.contains(processCallbackRequester)) {
            		throw new IllegalStateException("requestProcessCallback called but already on list");
            	}
            	processCallbackRequesterList.add(processCallbackRequester);
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#setTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void setTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            if(trace) traceMessage("setTimeStamp");
            if(this.timeStamp!=null) this.timeStamp.put(timeStamp.getSecondsPastEpoch(),timeStamp.getNanoSeconds());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessSupport#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            if(this.timeStamp==null) return;
            timeStamp.put(this.timeStamp.getSecondsPastEpoch(), this.timeStamp.getNanoSeconds());
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#getSupportProcessRequesterName()
         */
        public String getRequesterName() {
            return recordProcessSupportName;
        }
      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(!recordProcessActive) {
                throw new IllegalStateException("must be called from process or processContinue");
            }
            processIsComplete = true;
            this.requestResult = requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#beginGroupPut(org.epics.pvData.pv.PVRecord)
         */
        @Override
		public void beginGroupPut(PVRecord pvRecord) {}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVField)
		 */
		@Override
		public void dataPut(PVField pvField) {
			if(pvField!=pvSingleProcessRequester) {
				throw new IllegalStateException("logic error");
			}
			pvRecord.lock();
			try {
				boolean oldValue = singleProcessRequester;
				boolean newValue = pvSingleProcessRequester.get();
				if(oldValue==newValue) return;
				singleProcessRequester = pvSingleProcessRequester.get();
				if(!singleProcessRequester) return;
				if(tokenList.size()<2) return;
				// remove all requesters.
				while(true) {
					int index = tokenList.size();
					if(index==0) break;
					Token token = tokenList.remove(index-1);
					token.recordProcessRequester.lostRightToProcess();
				}
				while(true) {
					int index = queueRequestList.size();
					if(index==0) break;
					queueRequestList.remove(index-1);
				}
			} finally {
				pvRecord.unlock();
			}
		}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVField)
		 */
		@Override
		public void dataPut(PVStructure requested, PVField pvField) {}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
		 */
		@Override
		public void endGroupPut(PVRecord pvRecord) {}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
		 */
		@Override
		public void unlisten(PVRecord pvRecord) {
			// don't think I have to do anything
		}
                
        private void traceMessage(String message) {
            String time = "";
            if(timeStamp!=null) {
                long milliPastEpoch = System.currentTimeMillis();
                Date date = new Date(milliPastEpoch);
                time = String.format("%tF %tT.%tL ", date,date,date);
            }
            message(
                    time + " " + message + " thread " + Thread.currentThread().getName(),
                    MessageType.info);
        }
        // called by process and processContinue with record locked.
        private void completeProcessing() {
            processCompleteDone = true;
            callRecordProcessComplete = true;
            if(callStopAfterActive) {
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                fieldSupport.stop();
                pvRecord.removeEveryListener();
                callStopAfterActive = false;
            }
            if(callUninitializeAfterActive) {
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                fieldSupport.uninitialize();
                callUninitializeAfterActive = false;
            }
            if(!processCallbackRequesterList.isEmpty()){
                pvRecord.message(
                    "completing processing but ProcessCallbackRequesters are still present",
                    MessageType.fatalError);
            }
            pvRecord.endGroupPut();
            activeToken.recordProcessRequester.recordProcessResult(requestResult);
            if(!leaveActive) {
            	activeToken = null;
            	if(queueRequestList.size()>0) {
                	activeToken = queueRequestList.remove(0);
                	processIsComplete = false;
            		processCompleteDone = false;
            		pvRecord.beginGroupPut();
                }
            }
            if(trace) traceMessage("process completion " + fieldSupport.getRequesterName());
        }
        
		private void checkForIllegalRequest() {
            if(activeToken!=null && (recordProcessActive)) return;
            if(activeToken==null) {
                message("illegal request because record is not active",
                     MessageType.info);
                throw new IllegalStateException("record is not active");
            } else {
                message("illegal request because neither process or processContinue is running",
                        MessageType.info);
                throw new IllegalStateException("neither process or processContinue is running");
            }
        }
		
		private void message(String message,MessageType messageType) {
			message = pvRecord.getRecordName() + " " + message;
			pvRecord.message(message, messageType);
		}
    }
    
    private static class ProcessAfterStart implements AfterStartRequester, RecordProcessRequester {
    	private RecordProcess recordProcess;
    	private ProcessToken processToken = null;
    	private AfterStart afterStart = null;
    	private AfterStartNode afterStartNode = null;
    	

    	ProcessAfterStart(RecordProcess recordProcess,AfterStart afterStart) {
    		this.recordProcess = recordProcess;
    		this.afterStart = afterStart;
    		AfterStartNode afterStartNode = AfterStartFactory.allocNode(this);
    		afterStart.requestCallback(afterStartNode, true, ThreadPriority.middle);
    	}
		/* (non-Javadoc)
		 * @see org.epics.ioc.install.AfterStartRequester#callback(org.epics.ioc.install.AfterStartNode)
		 */
		@Override
		public void callback(AfterStartNode node) {
			afterStartNode = node;
			processToken = recordProcess.requestProcessToken(this);
			if(processToken==null) {
				recordProcess.getRecord().getPVStructure().message(
			        "processAfterStart but requestProcessToken failed",
			        MessageType.warning);
				return;
			}
			recordProcess.queueProcessRequest(processToken);
		}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Requester#getRequesterName()
		 */
		@Override
		public String getRequesterName() {
			return "ProcesssAfterStart";
		}
		/* (non-Javadoc)
		 * @see org.epics.pvData.pv.Requester#message(java.lang.String, org.epics.pvData.pv.MessageType)
		 */
		@Override
		public void message(String message, MessageType messageType) {
			recordProcess.getRecord().message(message, messageType);
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
		 */
		@Override
		public void becomeProcessor() {
			recordProcess.process(processToken, false, null);
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
		 */
		@Override
		public void canNotProcess(String reason) {
			recordProcess.getRecord().message(
					"ProcessAfterStart canNotProcess "
					+ reason,
					MessageType.warning);
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
		 */
		@Override
		public void lostRightToProcess() {}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
		 */
		@Override
		public void recordProcessComplete() {
			recordProcess.releaseProcessToken(processToken);
			afterStart.done(afterStartNode);
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
		 */
		@Override
		public void recordProcessResult(RequestResult requestResult) {}
    }
}
