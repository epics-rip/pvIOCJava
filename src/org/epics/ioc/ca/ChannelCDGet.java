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
public interface ChannelCDGet {
    /**
     * Get the interface for the CD created for this ChannelCDPut. 
     * @return The CD interface.
     */
    CD getCD();
    /**
     * Get the target data at put it into the CD.
     * If the request fails then ChannelCDGetRequestor.getDone is called before get returns..
     */
    void get();
}
