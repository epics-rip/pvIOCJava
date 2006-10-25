/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * Listener for a subscription request that wants notification but not the data.
 * @author mrk
 *
 */
public interface ChannelNotifyListener extends ChannelRequestor {
    /**
     * Beginning of a set of synchronous data.
     * @param channel The channel.
     */
    void beginSynchronous(Channel channel);
    /**
     * End of a set of synchronous data.
     * @param channel The channel.
     */
    void endSynchronous(Channel channel);
    /**
     * The field that changed.
     * @param channel The channel.
     * @param field The field.
     */
    void newData(Channel channel,ChannelField field);
}
