/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.channelAccess.ChannelAccess;

/**
 * @author mrk
 *
 */
public class ChannelAccessFactory {
    private static ChannelAccess channelAccess = new ChannelAccessImpl();
    static {
        ChannelProviderLocalFactory.register();
    }
    
    /**
     * Get the ChannelAccess interface.
     * @return The interface.
     */
    public static ChannelAccess getChannelAccess() {
        return channelAccess;
    }
    
    public static void registerChannelProvider(ChannelProvider channelProvider) {
        
    }
    
    private static class ChannelAccessImpl implements ChannelAccess,ChannelProvider{

    }
}
