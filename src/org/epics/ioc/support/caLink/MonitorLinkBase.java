/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorElement;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.Structure;

/**
 * Implementation for a channel access monitor link.
 * @author mrk
 *
 */
public class MonitorLinkBase extends AbstractIOLink
implements MonitorRequester,Runnable,RecordProcessRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvField The field being supported.
     */
    public MonitorLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    
    private static Executor executor = ExecutorFactory.create("caLinkMonitor", ThreadPriority.low);
    private ExecutorNode executorNode = executor.createNode(this);
    private PVBoolean reportOverrunAccess = null;
    private PVBoolean processAccess = null;
    private ProcessToken processToken = null;
    private boolean process = false;
    private boolean overrun = false;

    private MonitorElement monitorElement = null;
    
    private boolean isReady = false;
    private Monitor monitor = null;   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    @Override
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(super.getSupportState()!=SupportState.readyForStart) return;
        reportOverrunAccess = pvStructure.getBooleanField("reportOverrun");
        if(reportOverrunAccess==null)  {
            uninitialize(); return;
        }
        processAccess = pvStructure.getBooleanField("process");
        if(processAccess==null)  {
            uninitialize(); return;
        }
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(super.getSupportState()!=SupportState.ready) return;
        process = processAccess.get();
        if(process) {
        	processToken = recordProcess.requestProcessToken(this);
        	if(processToken==null) {
        		pvStructure.message("can not process",MessageType.warning);
                this.process = false;
        	}
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(processToken!=null) recordProcess.releaseProcessToken(processToken);
        processToken = null;
        super.stop();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.SupportListener)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            alarmSupport.setAlarm("Support not connected",AlarmSeverity.invalid);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        if(process) {
        	if(monitorElement==null) {
                alarmSupport.setAlarm("process request not by MonitorLink support",AlarmSeverity.minor);
        	} else {
        		getData();
        	}
        }
        supportProcessRequester.supportProcessDone(RequestResult.success);
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    @Override
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(monitor==null) {
                monitor = channel.createMonitor(this, pvRequest);
            } else {
                pvRecord.lock();
                try {
                    isReady = true;
                } finally {
                    pvRecord.unlock();
                }
                executor.execute(executorNode);
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
            executor.execute(executorNode);
        }
    } 
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.MonitorRequester#monitorConnect(Status,org.epics.pvData.monitor.Monitor, org.epics.pvData.pv.Structure)
     */
    @Override
    public void monitorConnect(Status status, Monitor monitor, Structure structure) {
        if(!status.isSuccess()) {
            message("createMonitor failed " + status.getMessage(),MessageType.error);
            return;
        }
        this.monitor = monitor;
        pvRecord.lock();
        try {
            isReady = true;
        } finally {
            pvRecord.unlock();
        }
        monitor.start();
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.MonitorRequester#monitorEvent(org.epics.pvData.monitor.Monitor)
     */
    @Override
    public void monitorEvent(Monitor monitor) {
        this.monitor = monitor;
        executor.execute(executorNode);
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
    	while(true) {
    		monitorElement = monitor.poll();
    		if(monitorElement==null && isReady) return;
    		if(processToken!=null) {
    			recordProcess.queueProcessRequest(processToken);
    			return;
    		}
    		pvRecord.lock();
    		try {
    			getData();
    		} finally {
    			pvRecord.unlock();
    		}
    		if(!isReady) return;
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.MonitorRequester#unlisten(org.epics.pvData.monitor.Monitor)
     */
    @Override
	public void unlisten(Monitor monitor) {
        recordProcess.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
	public void becomeProcessor() {
    	recordProcess.process(processToken,false, null);
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
	 */
	@Override
	public void canNotProcess(String reason) {
		pvRecord.lock();
		try {
			overrun = true;
			getData();
		} finally {
			pvRecord.unlock();
		}
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
        this.process = false;
        processToken = null;
        super.stop();
	}
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        if(isReady) run();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
     */
    public void recordProcessResult(RequestResult requestResult) {}  

    private void getData() {
    	if(!isReady) {
    		alarmSupport.setAlarm("connection lost", AlarmSeverity.invalid);
    		return;
    	}
    	PVStructure monitorStructure = monitorElement.getPVStructure();
    	if(super.linkPVFields==null) {
    		if(!super.setLinkPVStructure(monitorStructure)) {
    			monitor.destroy();
    			return;
    		}
    	} else {
    		super.linkPVStructure = monitorStructure;
    		super.linkPVFields = monitorStructure.getPVFields();
    	}
    	BitSet changeBitSet = monitorElement.getChangedBitSet();
    	BitSet overrunBitSet = monitorElement.getOverrunBitSet();
    	boolean allSet = changeBitSet.get(0);
    	for(int i=0; i< linkPVFields.length; i++) {
    		if(i==indexAlarmLinkField) {
    			super.pvAlarmMessage = monitorStructure.getStringField("alarm.message");
    			super.pvAlarmSeverityIndex = monitorStructure.getIntField("alarm.severity.index");
    			alarmSupport.setAlarm(pvAlarmMessage.get(),
    					AlarmSeverity.getSeverity(pvAlarmSeverityIndex.get()));
    		} else {
    			copyChanged(linkPVFields[i],pvFields[i],changeBitSet,allSet);
    		}
    	}
    	if(overrun || overrunBitSet.nextSetBit(0)>=0) {
    		alarmSupport.setAlarm(
    				"overrun",
    				AlarmSeverity.none);
    	}
    	overrun = false;
    	monitor.release(monitorElement);
    	monitorElement = null;
    }
    
    private void copyChanged(PVField pvFrom,PVField pvTo,BitSet changeBitSet,boolean allSet) {
        if(allSet) {
            convert.copy(pvFrom, pvTo);
            return;
        }
        int startFrom = pvFrom.getFieldOffset();
        int startTo = pvTo.getFieldOffset();
        int nextSet = changeBitSet.nextSetBit(startFrom);
        if(nextSet<0) return;
        if(nextSet==startFrom) {
            convert.copy(pvFrom, pvTo);
            return;
        }
        if(pvFrom.getNumberFields()==1) return;
        while(nextSet<pvFrom.getNextFieldOffset()) {
            PVField from = ((PVStructure)pvFrom).getSubField(nextSet);
            int nextTo = nextSet - startFrom + startTo;
            PVField to = ((PVStructure)pvTo).getSubField(nextTo);
            convert.copy(from, to);
            changeBitSet.clear(nextSet);
            nextSet = changeBitSet.nextSetBit(nextSet);
            if(nextSet<0) return;
        }
    }
    
    
}
