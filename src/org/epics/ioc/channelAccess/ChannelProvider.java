/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.channelAccess.ChannelRequester;

/**
 * @author mrk
 *
 */
public interface ChannelProvider {
    String getProviderName();
    void findChannel(String channelName,ChannelRequester channelRequester,double timeOut);
}
