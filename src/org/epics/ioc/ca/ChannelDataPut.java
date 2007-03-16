/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * Puts using a ChanneData.
 * @author mrk
 *
 */
public interface ChannelDataPut {
    /**
     * Get the interface for the ChannelData created for this ChannelDataPut. 
     * @return The ChannelData interface.
     */
    ChannelData getChannelData();
    /**
     * Get the latest value of the target data. The record will NOT be processed before the data is read.
     * @return (false,true) if the request (was not,was) started.
     * If true is returned then ChanneldataPutRequestor.getDone is called when the request is completed.
     */
    boolean get();
    /**
     * Get the latest value of the target data.
     * @return (false,true) if the request (was not,was) started.
     * If true is returned then ChanneldataPutRequestor.putDone is called when the request is completed.
     */
    boolean put();
}
