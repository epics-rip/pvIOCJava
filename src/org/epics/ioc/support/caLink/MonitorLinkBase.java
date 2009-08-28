/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.ProcessSelf;
import org.epics.ioc.support.ProcessSelfRequester;
import org.epics.ioc.support.RecordProcess;
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
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Structure;

/**
 * Implementation for a channel access monitor link.
 * @author mrk
 *
 */
public class MonitorLinkBase extends AbstractIOLink
implements MonitorRequester,Runnable,ProcessSelfRequester
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
        getData();
        supportProcessRequester.supportProcessDone(RequestResult.success);
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    @Override
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(monitor==null) {
                monitor = channel.createMonitor(this, pvRequest, "monitor", pvOption);
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
            if(monitor!=null) monitor.destroy();
            monitor = null;
        }
    } 
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.MonitorRequester#monitorConnect(org.epics.pvData.monitor.Monitor, org.epics.pvData.pv.Structure)
     */
    @Override
    public void monitorConnect(Monitor monitor, Structure structure) {
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
            if(monitorElement==null) return;
            if(process) {
                if(isRecordProcessRequester) {
                    boolean canProcess = recordProcess.process(this, false, null);
                    if(canProcess) return;
                    overrun = true;
                } else {
                    processSelf.request(this);
                    return;
                }
            }
            pvRecord.lock();
            try {
                getData();
                return;
            } finally {
                pvRecord.unlock();
            }
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
    public void recordProcessComplete() {
        if(processSelf!=null) processSelf.endRequest(this);
        run();
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
        if(canProcess) return;
        pvRecord.lock();
        try {
            overrun = true;
            getData();
        } finally {
            pvRecord.unlock();
        }
        run();
    }
    
    private void getData() {
        PVStructure monitorStructure = monitorElement.getPVStructure();
        BitSet changeBitSet = monitorElement.getChangedBitSet();
        BitSet overrunBitSet = monitorElement.getOverrunBitSet();
        boolean allSet = changeBitSet.get(0);
        copyChanged(monitorStructure.getSubField("value"),valuePVField,changeBitSet,allSet);
        if(alarmIsProperty) {
            PVString pvMessage = monitorStructure.getStringField("alarm.message");
            PVInt pvSeverityIndex = monitorStructure.getIntField("alarm.severity.index");
            alarmSupport.setAlarm(pvMessage.get(), AlarmSeverity.getSeverity(pvSeverityIndex.get()));
        }
        int length = 0;
        if(propertyPVFields!=null) length = propertyPVFields.length;
        if(length>=1) for(int i=0; i <length; i++) {
            PVField pvLink = monitorStructure.getSubField(propertyPVFields[i].getField().getFieldName());
            copyChanged(pvLink,propertyPVFields[i],changeBitSet,allSet);
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
