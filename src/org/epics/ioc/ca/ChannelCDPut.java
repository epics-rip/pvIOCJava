/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * Channel put using a CD (Channel Data) to hold the data..
 * @author mrk
 *
 */
public interface ChannelCDPut {
    /**
     * Get the interface for the CD created for this ChannelCDPut. 
     * @return The CD interface.
     */
    CD getCD();
    /**
     * Get the latest value of the target data. The record will NOT be processed before the data is read.
     * If the request fails then ChannelCDPutRequester.getDone is called before get returns.
     */
    void get();
    /**
     * Get the latest value of the target data.
     * If the request fails then ChannelCDPutRequester.putDone is called before put returns.
     */
    void put();
}
