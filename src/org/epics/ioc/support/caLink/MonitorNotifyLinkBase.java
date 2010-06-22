/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pvCopy.PVCopyFactory;

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
     * @param pvField The field being supported.
     */
    public MonitorNotifyLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    private static Executor executor = ExecutorFactory.create("caNotifyLinkMonitor", ThreadPriority.low);
    private ExecutorNode executorNode = executor.createNode(this);
    private ProcessToken processToken = null;
    
    private Monitor monitor = null;

    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(super.getSupportState()!=SupportState.ready) return;
        String request = "record[queueSize=2]field(timeStamp[algorithm=onChange,causeMonitor=true])";
        pvRequest = PVCopyFactory.createRequest(request,this);
        processToken = recordProcess.requestProcessToken(this);
    	if(processToken==null) {
    		pvStructure.message("can not process",MessageType.warning);
    		super.stop();
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
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
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
     * @see org.epics.pvData.monitor.MonitorRequester#monitorConnect(Status,org.epics.pvData.monitor.Monitor, org.epics.pvData.pv.Structure)
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
    	recordProcess.queueProcessRequest(processToken);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.MonitorRequester#unlisten(org.epics.pvData.monitor.Monitor)
     */
    @Override
	public void unlisten(Monitor monitor) {
        recordProcess.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
     */
    @Override
    public void recordProcessComplete() {}
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
     */
    @Override
    public void recordProcessResult(RequestResult requestResult) {}
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
		super.message("can not process " + reason, MessageType.warning);
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
        processToken = null;
        super.stop();
	}
}
