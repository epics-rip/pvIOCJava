/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * Request to get data from a channel.
 * @author mrk
 *
 */
public interface ChannelGet {
    /**
     * Allow no further requests.
     */
    void destroy();
    /**
     * Get data from the channel.
     * @param fieldGroup The description of the data to get.
     * @param channelGetListener Listener to call when the operation is complete.
     * @param process (false,true) if server (should not, should) process before getting data.
     * @return The result of request.
     */
    ChannelRequestReturn get(ChannelFieldGroup fieldGroup,ChannelGetListener channelGetListener,boolean process);
    /**
     * Cancel the get request.
     */
    void cancelGet();
}
