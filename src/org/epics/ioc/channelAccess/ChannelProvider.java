/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.channelAccess.*;

/**
 * @author mrk
 *
 */
public interface ChannelProvider {
    void findChannel(String channelName,double timeOut,ChannelProviderRequester channelProviderRequester);
    ChannelAccess getChannelAccess();
}
