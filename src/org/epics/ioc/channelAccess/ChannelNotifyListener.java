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
public interface ChannelNotifyListener {
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
     * The reason why the notification is being sent.
     * @param channel The channel.
     * @param reason The reason.
     */
    void reason(Channel channel,Event reason);
    /**
     * The field that changed.
     * @param channel The channel.
     * @param field The field.
     */
    void newData(Channel channel,ChannelField field);
    /**
     * Failure.
     * @param channel The channel.
     * @param reason The reason.
     */
    void failure(Channel channel,String reason);
}
