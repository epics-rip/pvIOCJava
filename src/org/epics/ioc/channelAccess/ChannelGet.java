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
     * @param listener listener to call when the operation is complete.
     * @param process Should the channel be processed before retrieving data?
     * @param wait Wait until process is complete beforec retrieving data?
     */
    void get(ChannelFieldGroup fieldGroup,ChannelGetListener listener,boolean process, boolean wait);
    /**
     * Cancel the get request.
     */
    void cancelGet();
}
