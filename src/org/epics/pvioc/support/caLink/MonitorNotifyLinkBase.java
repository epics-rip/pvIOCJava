/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;

/**
 * Implementation for a channel access monitor link.
 * @author mrk
 *
 */
public class MonitorNotifyLinkBase extends AbstractIOLink
implements MonitorRequester,Runnable,RecordProcessRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvRecordField The field being supported.
     */
    public MonitorNotifyLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    private static Executor executor = ExecutorFactory.create("caNotifyLinkMonitor", ThreadPriority.low);
    private ExecutorNode executorNode = executor.createNode(this);
    private ProcessToken processToken = null;
    
    private Monitor monitor = null;

    /* (non-Javadoc)
     * @see org.epics.pvioc.process.Support#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(super.getSupportState()!=SupportState.ready) return;
        String request = "record[queueSize=2]field(timeStamp[algorithm=onChange,causeMonitor=true])";
        pvRequest = CreateRequestFactory.createRequest(request,this);
        processToken = recordProcess.requestProcessToken(this);
    	if(processToken==null) {
    		pvStructure.message("can not process",MessageType.warning);
    		super.stop();
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
     * @see org.epics.pvioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    @Override
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            monitor = channel.createMonitor(this, pvRequest);
        } else {
            if(monitor!=null) monitor.destroy();
            monitor = null;
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
    	recordProcess.queueProcessRequest(processToken);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.monitor.MonitorRequester#unlisten(org.epics.pvdata.monitor.Monitor)
     */
    @Override
	public void unlisten(Monitor monitor) {
        recordProcess.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.RecordProcessRequester#recordProcessComplete(org.epics.pvioc.process.RequestResult)
     */
    @Override
    public void recordProcessComplete() {}
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.AlarmSeverity, java.lang.String, org.epics.pvioc.util.TimeStamp)
     */
    @Override
    public void recordProcessResult(RequestResult requestResult) {}
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
		super.message("can not process " + reason, MessageType.warning);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
        processToken = null;
        super.stop();
	}
}
