/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;

import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.PVCopy;

/**
 * @author mrk
 *
 */
public class MonitorQueueFactory {
    public static MonitorQueue create(PVCopy pvCopy, byte queueSize) {
        MonitorQueue.MonitorQueueElement[] monitorQueueElements = new MonitorQueue.MonitorQueueElement[queueSize];
        for(int i=0; i<monitorQueueElements.length; i++) {
            PVStructure pvStructure = pvCopy.createPVStructure();
            monitorQueueElements[i] = new MonitorQueueElementImlp(pvStructure);
            
        }
        return new MonitorQueueImpl(monitorQueueElements);
    }
    
    
    private static class MonitorQueueElementImlp implements MonitorQueue.MonitorQueueElement {
        MonitorQueueElementImlp(PVStructure pvStructure) {
            this.pvStructure = pvStructure;
            changedBitSet = new BitSet(pvStructure.getNumberFields());
            overrunBitSet = new BitSet(pvStructure.getNumberFields());
        }
        
        private PVStructure pvStructure;
        private BitSet changedBitSet;
        private BitSet overrunBitSet;
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement#getChangedBitSet()
         */
        @Override
        public BitSet getChangedBitSet() {
            return changedBitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement#getOverrunBitSet()
         */
        @Override
        public BitSet getOverrunBitSet() {
            return overrunBitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement#getPVStructure()
         */
        @Override
        public PVStructure getPVStructure() {
            return pvStructure;
        }
    }
    private static class MonitorQueueImpl implements MonitorQueue {
        MonitorQueueElement[] monitorQueueElements;
        int number = 0;
        int numberUsed = 0;
        int nextFree = 0;
        int nextUsed = 0;
        
        MonitorQueueImpl(MonitorQueueElement[] monitorQueueElements) {
            this.monitorQueueElements = monitorQueueElements;
            number = monitorQueueElements.length;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue#clear()
         */
        @Override
        public void clear() {
            numberUsed = 0;
            nextFree = 0;
            nextUsed = 0;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue#capacity()
         */
        @Override
        public int capacity() {
            return number;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue#getNumberFree()
         */
        @Override
        public int getNumberFree() {
            synchronized(this) {
                return number - numberUsed;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue#getFree()
         */
        @Override
        public MonitorQueueElement getFree() {
            synchronized(this) {
                if(numberUsed>=number) return null;
                MonitorQueueElement monitorQueueElement = monitorQueueElements[nextFree++];
                if(nextFree>=number) nextFree = 0;
                numberUsed++;
                return monitorQueueElement;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue#getUsed()
         */
        @Override
        public MonitorQueueElement getUsed() {
            synchronized(this) {
                if(numberUsed==0) return null;
                return monitorQueueElements[nextUsed];
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorQueue#release()
         */
        @Override
        public void release() {
            synchronized(this) {
                nextUsed++;
                numberUsed--;
            }
        }
    }
}
