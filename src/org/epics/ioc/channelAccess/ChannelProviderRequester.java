/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.pv.Requester;

/**
 * @author mrk
 *
 */
public interface ChannelProviderRequester extends Requester {
    void foundChannel(String channelName,ChannelProvider channelProvider);
    void timeout(String channelName,ChannelProvider channelProvider);
}
