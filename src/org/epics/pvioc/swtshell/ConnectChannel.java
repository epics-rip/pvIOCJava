/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;


/**
 * A set of controls for connecting to a channel.
 * @author mrk
 *
 */
public interface ConnectChannel {
    /**
     * Connect to the channel.
     * The ChannelRequester is called with the result of the request.
     */
    void connect();
    /**
     * Called by client when the client has received the first channelStateChange.
     */
    void cancelTimeout();
}
