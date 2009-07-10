/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;

import org.epics.pvData.channelAccess.Channel;
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
 * A Base class for implementing a ChannelMonitor.
 * @author mrk
 *
 */
abstract public class BaseMonitor implements ChannelMonitor,PVCopyMonitorRequester{
    /**
     * Constructor for BaseMonitor
     * @param channel The channel;
     * @param channelMonitorRequester The requester.
     * @param pvCopy The PVCopy for creating data and bit sets.
     * @param queueSize The queueSize.
     * @param executor The executor for calling requester.
     */
    protected BaseMonitor(
            Channel channel,
            ChannelMonitorRequester channelMonitorRequester,
            PVCopy pvCopy,
            byte queueSize,
            Executor executor)
    {
        this.channel = channel;
        this.channelMonitorRequester = channelMonitorRequester;
        this.pvCopy = pvCopy;
        this.executor = executor;
        this.queueSize = queueSize;
        pvRecord = pvCopy.getPVRecord();
        callRequester = new CallRequester();
        pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
        if(queueSize<2)  {
            pvStructure = pvCopy.createPVStructure();
            changeBitSets = new BitSet[2];
            overrunBitSets = new BitSet[2];
            for(int i=0; i<2; i++) {
                changeBitSets[i] = new BitSet(pvStructure.getNextFieldOffset());
                overrunBitSets[i] = new BitSet(pvStructure.getNextFieldOffset());
            }
        } else {
            monitorQueue = MonitorQueueFactory.create(pvCopy, queueSize);
        }
    }
    private static final ChannelProvider channelProvider = ChannelProviderFactory.getChannelProvider();
    protected static final Convert convert = ConvertFactory.getConvert();
    protected static final BitSetUtil bitSetUtil = BitSetUtilFactory.getCompressBitSet();
    protected Channel channel;
    protected ChannelMonitorRequester channelMonitorRequester;
    protected PVCopy pvCopy;
    protected Executor executor;
    protected PVRecord pvRecord;
    private CallRequester callRequester;
    private PVCopyMonitor pvCopyMonitor;
    private boolean isMonitoring = false;
    private byte queueSize;
    // following only used if queueSize <=1
    private PVStructure pvStructure = null;
    private int indexBitSet = 0;
    private BitSet[] changeBitSets = null;
    private BitSet[] overrunBitSets = null;
    // following only used if queueSize>=2
    private MonitorQueue monitorQueue = null;
    private MonitorQueue.MonitorQueueElement monitorQueueElement = null;
    
    
    /**
     * A method that must be implemented by a derived class.
     * When this class gets notified that data has changed it calls this method to see
     * if it should notify the ChannelMonitorRequester that a monitor has occurred.
     * @param changeBitSet The change bit set.
     * @return (false,true) if the ChannelMonitorRequester should be notified of a new monitor.
     */
    abstract protected boolean generateMonitor(BitSet changeBitSet);
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitor#destroy()
     */
    @Override
    public void destroy() {
        if(isMonitoring) {
            pvCopyMonitor.stopMonitoring();
            isMonitoring = false;
        }
        channelProvider.destroyMonitor(channel, this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.ChannelMonitor#start()
     */
    @Override
    public void start() {
        BitSet changeBitSet = null;
        BitSet overrunBitSet = null;
        pvRecord.lock();
        try {
            if(queueSize<2) {
                indexBitSet = 0;
                changeBitSet = changeBitSets[indexBitSet];
                overrunBitSet = overrunBitSets[indexBitSet];
                
            } else {
                monitorQueue.clear();
                monitorQueueElement = monitorQueue.getFree();
                changeBitSet = monitorQueueElement.getChangedBitSet();
                overrunBitSet = monitorQueueElement.getOverrunBitSet();
            }
        } finally {
            pvRecord.unlock();
        }
        pvCopyMonitor.startMonitoring(changeBitSet,overrunBitSet);
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
        if(queueSize<2) {
            if(!generateMonitor(changeBitSets[indexBitSet])) return;
        } else {
            PVStructure pvStructure = monitorQueueElement.getPVStructure();
            BitSet bitSet = monitorQueueElement.getChangedBitSet();
            pvCopy.updateCopyFromBitSet(pvStructure, bitSet, false);
            if(!generateMonitor(monitorQueueElement.getChangedBitSet())) return;
            MonitorQueue.MonitorQueueElement newElement = monitorQueue.getFree();
            if(newElement==null) return;
            bitSetUtil.compress(bitSet, pvStructure);
            bitSet = monitorQueueElement.getOverrunBitSet();
            bitSetUtil.compress(bitSet, pvStructure);
            pvStructure = monitorQueueElement.getPVStructure();
            PVStructure pvNext = newElement.getPVStructure();
            convert.copy(pvStructure, pvNext);
            BitSet changeBitSet = newElement.getChangedBitSet();
            BitSet overrunBitSet = newElement.getOverrunBitSet();
            changeBitSet.clear();
            overrunBitSet.clear();
            pvCopyMonitor.switchBitSets(changeBitSet,overrunBitSet , false);
            monitorQueue.setUsed(monitorQueueElement);
            monitorQueueElement = newElement;
        }
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
            if(queueSize<2) {
                BitSet changeBitSet = changeBitSets[indexBitSet];
                BitSet overrunBitSet = changeBitSets[indexBitSet];
                int nextIndex = (indexBitSet + 1) % 2;
                BitSet nextChangeBitSet = changeBitSets[nextIndex];
                BitSet nextOverrunBitSet = changeBitSets[nextIndex];
                nextChangeBitSet.clear();
                nextOverrunBitSet.clear();
                PVRecord pvRecord = pvCopy.getPVRecord();
                pvRecord.lock();
                try {
                    pvCopy.updateCopyFromBitSet(pvStructure, changeBitSet, false);
                    pvCopyMonitor.switchBitSets(nextChangeBitSet, nextOverrunBitSet, false);
                    indexBitSet = nextIndex;
                } finally {
                    pvRecord.unlock();
                }
                bitSetUtil.compress(changeBitSet, pvStructure);
                bitSetUtil.compress(overrunBitSet, pvStructure);
                channelMonitorRequester.monitorEvent(pvStructure,changeBitSet,overrunBitSet);
            } else { // using queue
                while(true) {
                    MonitorQueue.MonitorQueueElement monitorQueueElement = monitorQueue.getUsed();
                    if(monitorQueueElement==null) {
                        return;
                    }
                    PVStructure pvStructure = monitorQueueElement.getPVStructure();
                    BitSet changeBitSet = monitorQueueElement.getChangedBitSet();
                    BitSet overrunBitSet = monitorQueueElement.getOverrunBitSet();
                    channelMonitorRequester.monitorEvent(pvStructure,changeBitSet,overrunBitSet);
                    monitorQueue.releaseUsed(monitorQueueElement);
                }
            }
        }
        private void call() {
            executor.execute(executorNode);
        }

    }
}

