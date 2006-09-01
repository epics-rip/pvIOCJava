/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.PVData;


/**
 * Listener for a subscription request that wants data as well as notification.
 * @author mrk
 *
 */
public interface ChannelNotifyGetListener {
    /**
     * Beginning of a set of synchronous data.
     * @param channel the channel.
     */
    void beginSynchronous(Channel channel);
    /**
     * End of a set of synchronous data.
     * @param channel The channel.
     */
    void endSynchronous(Channel channel);
    /**
     * The reason why the data is being sent.
     * @param channel The channel.
     * @param reason The reason.
     */
    void reason(Channel channel,Event reason);
    /**
     * New data value.
     * @param channel The channel.
     * @param field The field.
     * @param data The data.
     */
    void newData(Channel channel,ChannelField field,PVData data);
    /**
     * Failure.
     * @param channel The channel.
     * @param reason The reason.
     */
    void failure(Channel channel,String reason);
}
