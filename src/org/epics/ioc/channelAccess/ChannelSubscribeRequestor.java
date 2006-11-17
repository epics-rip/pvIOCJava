/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;

/**
 * Listener for a subscription request that wants notification but not the data.
 * @author mrk
 *
 */
public interface ChannelSubscribeRequestor extends Requestor {
    /**
     * A message for requester.
     * @param channel The channel.
     * @param message The message.
     * @param messageType The message type.
     */
    void message(Channel channel,String message,MessageType messageType);
    /**
     * New data value.
     * @param channel The channel.
     */
    void dataModified(Channel channel);
}
