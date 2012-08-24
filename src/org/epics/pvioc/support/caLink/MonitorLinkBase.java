/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;

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
     * @param pvRecordField The field being supported.
     */
    public MonitorLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
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
     * @see org.epics.pvioc.support.caLink.AbstractIOLink#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
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
     * @see org.epics.pvioc.process.Support#start()
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
     * @see org.epics.pvioc.process.Support#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(processToken!=null) recordProcess.releaseProcessToken(processToken);
        processToken = null;
        super.stop();
    }
    
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.Support#process(org.epics.pvioc.process.SupportListener)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            alarmSupport.setAlarm("Support not connected",AlarmSeverity.INVALID,AlarmStatus.DB);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        if(process) {
        	if(monitorElement==null) {
                alarmSupport.setAlarm("process request not by MonitorLink support",AlarmSeverity.MINOR,AlarmStatus.DB);
        	} else {
        		getData();
        	}
        }
        supportProcessRequester.supportProcessDone(RequestResult.success);
    } 
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
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
     * @see org.epics.pvdata.monitor.MonitorRequester#monitorConnect(Status,org.epics.pvdata.monitor.Monitor, org.epics.pvdata.pv.Structure)
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
     * @see org.epics.pvdata.monitor.MonitorRequester#monitorEvent(org.epics.pvdata.monitor.Monitor)
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
     * @see org.epics.pvdata.monitor.MonitorRequester#unlisten(org.epics.pvdata.monitor.Monitor)
     */
    @Override
	public void unlisten(Monitor monitor) {
        recordProcess.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
	public void becomeProcessor() {
    	recordProcess.process(processToken,false);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
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
	 * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
        this.process = false;
        processToken = null;
        super.stop();
	}
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        if(isReady) run();
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.AlarmSeverity, java.lang.String, org.epics.pvioc.util.TimeStamp)
     */
    public void recordProcessResult(RequestResult requestResult) {}  

    private void getData() {
    	if(!isReady) {
    		alarmSupport.setAlarm("connection lost", AlarmSeverity.INVALID,AlarmStatus.DB);
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
    			super.pvAlarmSeverity = monitorStructure.getIntField("alarm.severity");
    			alarmSupport.setAlarm(pvAlarmMessage.get(),
    					AlarmSeverity.getSeverity(pvAlarmSeverity.get()),AlarmStatus.DB);
    		} else {
    			copyChanged(linkPVFields[i],pvFields[i],changeBitSet,allSet);
    		}
    	}
    	if(overrun || overrunBitSet.nextSetBit(0)>=0) {
    		alarmSupport.setAlarm(
    				"overrun",
    				AlarmSeverity.NONE,AlarmStatus.DB);
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
