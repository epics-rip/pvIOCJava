/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * A queue of ChannelData.
 * @author mrk
 *
 */
public interface ChannelDataQueue {
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
     * @param forceFree If true than return a ChannelData even
     * if the client has not retrieved the data it contains.
     * @return A ChannelData.
     */
    ChannelData getFree(boolean forceFree);
    /**
     * Get the oldest queue element.
     * @return The oldest element.
     */
    ChannelData getNext();
    /**
     * Get the number of missed sets of data.
     * The number is reset to 0.
     * @return The number of missed sets of data.
     */
    int getNumberMissed();
    /**
     * Release the queue element. This must be the element returned by getNext.
     * @param channelData The queue element to release.
     */
    void releaseNext(ChannelData channelData);
}
