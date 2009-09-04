/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.ProcessSelf;
import org.epics.ioc.support.ProcessSelfRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.Structure;

/**
 * Implementation for a channel access monitor link.
 * @author mrk
 *
 */
public class MonitorNotifyLinkBase extends AbstractIOLink
implements MonitorRequester,Runnable,ProcessSelfRequester
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
    private boolean isRecordProcessRequester = false;
    private ProcessSelf processSelf = null;
    private PVStructure pvOption = null;
    private PVString pvAlgorithm = null;
    
    private Monitor monitor = null;

    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(super.getSupportState()!=SupportState.ready) return;
        pvOption = pvDataCreate.createPVStructure(null, "pvOption", new Field[0]);
        pvAlgorithm = (PVString)pvDataCreate.createPVScalar(pvOption, "algorithm", ScalarType.pvString);
        pvAlgorithm.put("onPut");
        pvOption.appendPVField(pvAlgorithm);
        PVInt pvQueueSize = (PVInt)pvDataCreate.createPVScalar(pvOption, "queueSize", ScalarType.pvInt);
        pvQueueSize.put(0);
        pvOption.appendPVField(pvQueueSize);
        isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
        if(!isRecordProcessRequester) {
            processSelf = recordProcess.canProcessSelf();
            if(processSelf==null) {
                pvStructure.message("process not possible",
                        MessageType.error);
                super.stop();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
        isRecordProcessRequester = false;
        super.stop();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    @Override
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            monitor = channel.createMonitor(this, pvRequest, "monitorNotify", pvOption);
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
    	// TODO check status
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
        if(isRecordProcessRequester) {
            becomeProcessor(recordProcess);
        } else {
            processSelf.request(this);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.ChannelMonitorRequester#unlisten()
     */
    @Override
    public void unlisten() {
        recordProcess.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
     */
    public void recordProcessComplete() {}
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
     */
    public void recordProcessResult(RequestResult requestResult) {}
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
     */
    public void becomeProcessor(RecordProcess recordProcess) {
        boolean canProcess = recordProcess.process(this, false, null);
        if(!canProcess) {
            recordProcessComplete();
            return;
        }
    } 
}
