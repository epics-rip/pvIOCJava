/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.RequestResult;

/**
 * Base interface for channel requests.
 * @author mrk
 *
 */
public interface ChannelRequestor {
    /**
     * Get the name of the ChannelRequestor;
     * @return The name.
     */
    String getChannelRequestorName();
    /**
     * A message for requester.
     * @param channel The channel.
     * @param message The message.
     */
    void message(Channel channel,String message);
    /**
     * The request is done. This is always called with no locks held.
     * This is only called if the request is asynchronous,
     * i.e. if the original request returned ChannelRequestReturn.active.
     * @param channel The channel.
     * @param requestResult The result of the request.
     * This is always success or failure.
     */
    void requestDone(Channel channel, RequestResult requestResult);
}
