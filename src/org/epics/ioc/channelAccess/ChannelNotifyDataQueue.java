/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * A queue of ChannelNotifyData.
 * @author mrk
 *
 */
public interface ChannelNotifyDataQueue {
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
     * Get the next free queue element.
     * @return
     */
    ChannelNotifyData getFree();
    /**
     * Get the oldest queue element.
     * @return The oldest element.
     */
    ChannelNotifyData getNext();
    /**
     * Get the number of missed sets of data.
     * @return The number of missed sets of data.
     */
    int getNumberMissed();
    /**
     * Release the queue element. This must be the element returned by getNext.
     * @param channelNotifyData The queue element to release.
     */
    void releaseNext(ChannelNotifyData channelNotifyData);
}
