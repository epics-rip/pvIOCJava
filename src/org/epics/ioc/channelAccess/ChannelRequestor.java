/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;

/**
 * Base interface for channel requests.
 * @author mrk
 *
 */
public interface ChannelRequestor extends Requestor{
    /**
     * A message for requester.
     * @param channel The channel.
     * @param message The message.
     * @param messageType The type of message.
     */
    void message(Channel channel,String message,MessageType messageType);
    /**
     * The request is done. This is always called with no locks held.
     * This is only called if the request is asynchronous,
     * i.e. if the original request returned ChannelRequestReturn.active
     * or was a subscription request.
     * @param channel The channel.
     * @param requestResult The result of the request.
     * This is always success or failure.
     */
    void requestDone(Channel channel, RequestResult requestResult);
}
