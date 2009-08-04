/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.ProcessSelf;
import org.epics.ioc.support.ProcessSelfRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDouble;
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
public class MonitorLinkBase extends AbstractIOLink
implements ChannelMonitorRequester,ProcessSelfRequester
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
    private PVString monitorTypeAccess = null;
    private PVDouble deadbandAccess = null;
    private PVInt queueSizeAccess = null;
    private PVBoolean reportOverrunAccess = null;
    private PVBoolean processAccess = null;
    
    
    private double deadband = 0.0;
    private int queueSize = 0;
    private boolean isRecordProcessRequester = false;
    private ProcessSelf processSelf = null;
    private boolean process = false;
    private PVStructure pvOption = null;
    private PVString pvAlgorithm = null;
   
          
    
    private PVStructure monitorStructure = null;
    private BitSet changeBitSet = null;
    private BitSet overrunBitSet = null;
    
    private boolean isReady = false;
    private ChannelMonitor channelMonitor = null;
    private ReentrantLock lock = new ReentrantLock();
    private Condition waitForCopied = lock.newCondition();
    private volatile boolean cdCopied = false;

   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    @Override
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(super.getSupportState()!=SupportState.readyForStart) return;
        monitorTypeAccess = pvStructure.getStringField("type");
        if(monitorTypeAccess==null) {
            uninitialize(); return;
        }
        deadbandAccess = pvStructure.getDoubleField("deadband");
        if(deadbandAccess==null)  {
            uninitialize(); return;
        }
        queueSizeAccess = pvStructure.getIntField("queueSize");
        if(queueSizeAccess==null)  {
            uninitialize(); return;
        }
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
        String monitorType = monitorTypeAccess.get();
        deadband = deadbandAccess.get();
        queueSize = queueSizeAccess.get();
        if(queueSize<=1) {
            pvStructure.message("queueSize being put to 2", MessageType.warning);
            queueSize = 2;
        }
        pvOption = pvDataCreate.createPVStructure(null, "pvOption", new Field[0]);
        pvAlgorithm = (PVString)pvDataCreate.createPVScalar(pvOption, "algorithm", ScalarType.pvString);
        pvAlgorithm.put(monitorType);
        pvOption.appendPVField(pvAlgorithm);
        PVInt pvQueueSize = (PVInt)pvDataCreate.createPVScalar(pvOption, "queueSize", ScalarType.pvInt);
        pvQueueSize.put(queueSize);
        pvOption.appendPVField(pvQueueSize);
        PVDouble pvDeadband = (PVDouble)pvDataCreate.createPVScalar(pvOption, "deadband", ScalarType.pvDouble);
        pvDeadband.put(deadband);
        pvOption.appendPVField(pvDeadband);
        process = processAccess.get();
        if(process) {
            isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
            if(!isRecordProcessRequester) {
                processSelf = recordProcess.canProcessSelf();
                if(processSelf==null) {
                    pvStructure.message("process may fail",
                            MessageType.warning);
                    this.process = false;
                }
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
     * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.SupportListener)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            alarmSupport.setAlarm("Support not connected",AlarmSeverity.invalid);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        getCD();
        supportProcessRequester.supportProcessDone(RequestResult.success);
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    @Override
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            channel.createChannelMonitor(channel, this, pvRequest, "monitor", pvOption, executor);
            pvRecord.lock();
            try {
                isReady = true;
            } finally {
                pvRecord.unlock();
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
            if(channelMonitor!=null) channelMonitor.destroy();
            channelMonitor = null;
        }
    } 
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitorRequester#channelMonitorConnect(org.epics.pvData.channelAccess.ChannelMonitor)
     */
    @Override
    public void channelMonitorConnect(ChannelMonitor channelMonitor) {
        this.channelMonitor = channelMonitor;
        channelMonitor.start();
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitorRequester#monitorEvent(org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet, org.epics.pvData.misc.BitSet)
     */
    @Override
    public void monitorEvent(PVStructure pvStructure, BitSet changeBitSet,BitSet overrunBitSet) {
        monitorStructure = pvStructure;
        this.changeBitSet = changeBitSet;
        this.overrunBitSet = overrunBitSet;
        if(process) {
            cdCopied = false;
            if(isRecordProcessRequester) {
                becomeProcessor(recordProcess);
            } else {
                processSelf.request(this);
            }
            lock.lock();
            try {
                if(!cdCopied) waitForCopied.await(10, TimeUnit.SECONDS);
            } catch(InterruptedException e) {
                System.err.println(
                        e.getMessage()
                        + " thread did not call ready");
            } finally {
                lock.unlock();
            }
            return;
        }
        pvRecord.lock();
        try {
            getCD();
            return;
        } finally {
            pvRecord.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitorRequester#unlisten()
     */
    @Override
    public void unlisten() {
        recordProcess.stop();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
     */
    public void recordProcessComplete() {
        lock.lock();
        try {
            cdCopied = true;
            waitForCopied.signal();
            if(processSelf!=null) processSelf.endRequest(this);
        } finally {
            lock.unlock();
        }
    }
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
    
    
    private void getCD() {
        boolean allSet = changeBitSet.get(0);
        copyChanged(monitorStructure.getSubField("value"),valuePVField,allSet);
        if(alarmIsProperty) {
            PVString pvMessage = monitorStructure.getStringField("alarm.message");
            PVInt pvSeverityIndex = monitorStructure.getIntField("alarm.severity.index");
            alarmSupport.setAlarm(pvMessage.get(), AlarmSeverity.getSeverity(pvSeverityIndex.get()));
        }
        int length = 0;
        if(propertyPVFields!=null) length = propertyPVFields.length;
        if(length>=1) for(int i=0; i <length; i++) {
            PVField pvLink = monitorStructure.getSubField(propertyPVFields[i].getField().getFieldName());
            copyChanged(pvLink,propertyPVFields[i],allSet);
        }
        if(overrunBitSet.nextSetBit(0)>=0) {
            alarmSupport.setAlarm(
                    "overrun",
                    AlarmSeverity.none);
        }
    }
    
    private void copyChanged(PVField pvFrom,PVField pvTo,boolean allSet) {
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
