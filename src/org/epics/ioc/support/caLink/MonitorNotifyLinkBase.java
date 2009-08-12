/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ca.channelAccess.client.ChannelMonitor;
import org.epics.ca.channelAccess.client.ChannelMonitorRequester;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.ProcessSelf;
import org.epics.ioc.support.ProcessSelfRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;

/**
 * Implementation for a channel access monitor link.
 * @author mrk
 *
 */
public class MonitorNotifyLinkBase extends AbstractIOLink
implements ChannelMonitorRequester,ProcessSelfRequester
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
    private boolean isRecordProcessRequester = false;
    private ProcessSelf processSelf = null;
    private PVStructure pvOption = null;
    private PVString pvAlgorithm = null;
    
    private ChannelMonitor channelMonitor = null;

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
            channel.createChannelMonitor(this, pvRequest, "monitorNotify", pvOption, executor);
        } else {
            if(channelMonitor!=null) channelMonitor.destroy();
            channelMonitor = null;
        }
    } 
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.ChannelMonitorRequester#channelMonitorConnect(org.epics.ca.channelAccess.client.ChannelMonitor)
     */
    @Override
    public void channelMonitorConnect(ChannelMonitor channelMonitor) {
        this.channelMonitor = channelMonitor;
        channelMonitor.start();
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.ChannelMonitorRequester#monitorEvent(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, org.epics.pvData.misc.BitSet)
     */
    @Override
    public void monitorEvent(PVStructure pvStructure, BitSet changeBitSet,BitSet overrunBitSet) {
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
    public void recordProcessResult(RequestResult requestResult) {
        // nothing to do
    }
    
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
