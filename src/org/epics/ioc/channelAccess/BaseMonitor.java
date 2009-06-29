/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;

import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.BitSetUtil;
import org.epics.pvData.pvCopy.BitSetUtilFactory;
import org.epics.pvData.pvCopy.PVCopy;
import org.epics.pvData.pvCopy.PVCopyMonitor;
import org.epics.pvData.pvCopy.PVCopyMonitorRequester;
/**
 * @author mrk
 *
 */
abstract public class BaseMonitor implements ChannelMonitor,PVCopyMonitorRequester{
    
    protected BaseMonitor(
            ChannelMonitorRequester channelMonitorRequester,
            PVCopy pvCopy,
            byte queueSize,
            Executor executor)
    {
        this.channelMonitorRequester = channelMonitorRequester;
        this.pvCopy = pvCopy;
        this.executor = executor;
        pvRecord = pvCopy.getPVRecord();
        callRequester = new CallRequester();
        pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
        monitorQueue = MonitorQueueFactory.create(pvCopy, queueSize);
    }
    
    private static final Convert convert = ConvertFactory.getConvert();
    private static final BitSetUtil bitSetUtil = BitSetUtilFactory.getCompressBitSet();
    private ChannelMonitorRequester channelMonitorRequester;
    private PVCopy pvCopy;
    private Executor executor;
    private PVRecord pvRecord;
    private CallRequester callRequester;
    private PVCopyMonitor pvCopyMonitor;
    private MonitorQueue monitorQueue;
    private MonitorQueue.MonitorQueueElement monitorQueueElement = null;
    private boolean isMonitoring = false;
    
    abstract protected boolean generateMonitor(MonitorQueue.MonitorQueueElement monitorQueueElement);
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitor#destroy()
     */
    @Override
    public void destroy() {
        if(isMonitoring) {
            pvCopyMonitor.stopMonitoring();
            isMonitoring = false;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitor#start()
     */
    @Override
    public void start() {
        pvRecord.lock();
        try {
            monitorQueue.clear();
            monitorQueueElement = monitorQueue.getFree();
            PVStructure pvStructure = monitorQueueElement.getPVStructure();
            pvCopy.initCopy(pvStructure, monitorQueueElement.getChangedBitSet(), false);
            monitorQueueElement = monitorQueue.getFree();
            if(monitorQueueElement!=null) {
                PVStructure pvNext = monitorQueueElement.getPVStructure();
                convert.copy(pvStructure, pvNext);
                monitorQueueElement.getChangedBitSet().clear();
                monitorQueueElement.getOverrunBitSet().clear();
                callRequester.call();
            }
        } finally {
            pvRecord.unlock();
        }
        pvCopyMonitor.startMonitoring(monitorQueueElement.getChangedBitSet(),monitorQueueElement.getOverrunBitSet());
        isMonitoring = true;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitor#stop()
     */
    @Override
    public void stop() {
        pvCopyMonitor.stopMonitoring();
        isMonitoring = false;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pvCopy.PVCopyMonitorRequester#dataChanged()
     */
    @Override
    public void dataChanged() {
        PVStructure pvStructure = monitorQueueElement.getPVStructure();
        BitSet bitSet = monitorQueueElement.getChangedBitSet();
        bitSetUtil.compress(bitSet, pvStructure);
        bitSet = monitorQueueElement.getOverrunBitSet();
        bitSetUtil.compress(bitSet, pvStructure);
        if(!generateMonitor(monitorQueueElement)) return;
        MonitorQueue.MonitorQueueElement newElement = monitorQueue.getFree();
        if(newElement==null) {
            if(monitorQueue.capacity()<2) {
                callRequester.call();
            }
            return;
        }
        pvStructure = monitorQueueElement.getPVStructure();
        PVStructure pvNext = newElement.getPVStructure();
        convert.copy(pvStructure, pvNext);
        monitorQueueElement = newElement;
        monitorQueueElement.getChangedBitSet().clear();
        monitorQueueElement.getOverrunBitSet().clear();
        callRequester.call();
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.pvCopy.PVCopyMonitorRequester#unlisten()
     */
    @Override
    public void unlisten() {
        channelMonitorRequester.unlisten();
    }
    
    private class CallRequester implements Runnable {
        private ExecutorNode executorNode = null;
        private CallRequester(){
            executorNode = executor.createNode(this);
        }         

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        public void run() {
            while(true) {
                MonitorQueue.MonitorQueueElement monitorQueueElement = monitorQueue.getUsed();
                if(monitorQueueElement==null) {
                    return;
                }
                channelMonitorRequester.monitorEvent(
                        monitorQueueElement.getPVStructure(),
                        monitorQueueElement.getChangedBitSet(),
                        monitorQueueElement.getOverrunBitSet());
                monitorQueue.release();
            }
        }
        private void call() {
            executor.execute(executorNode);
        }

    }
}

