/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;
import java.util.BitSet;

import org.epics.pvData.pv.PVStructure;

/**
 * A queue for monitors.
 * @author mrk
 *
 */
public interface MonitorQueue {
    
    /**
     * Each queue element provides a PVStructure, changedBitSet, and overrunBitSet.
     * @author mrk
     *
     */
    public interface MonitorQueueElement {
        /**
         * Get the PVStructure.
         * @return The PVStructure.
         */
        PVStructure getPVStructure();
        /**
         * Get the bitSet showing which fields have changed.
         * @return The bitSet.
         */
        BitSet getChangedBitSet();
        /**
         * Get the bitSet showing which fields have been changed more than once.
         * @return The bitSet.
         */
        BitSet getOverrunBitSet();
    }
    /**
     * Set all elements free.
     */
    void clear();
    /**
     * Get the number of free queue elements.
     * @return The number.
     */
    int getNumberFree();
    /**
     * Get the queue capacity.
     * @return The capacity.
     */
    int capacity();
    /**
     * Get the next free element.
     * @return The next free element or null if no free elements.
     */
    MonitorQueueElement getFree();
    /**
     * Get the oldest used element.
     * @return
     */
    MonitorQueueElement getUsed();
    /**
     * Release the getUsed structure.
     */
    void release();
}
