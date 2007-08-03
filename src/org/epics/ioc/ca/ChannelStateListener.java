/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.*;
/**
 * Listener for connect state changes.
 * @author mrk
 *
 */
public interface ChannelStateListener extends Requester {
    /**
     * The channel has put connection state.
     * @param c The channel.
     * @param isConnected (false,true) if new state (is not, is) connected.
     */
    void channelStateChange(Channel c,boolean isConnected);
    /**
     * Disconnect from the channel. The channel will not honor any further requests.
     * @param c The channel.
     */
    void disconnect(Channel c);
}
