/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * Factory for creating channels. 
 * @author mrk
 *
 */
public class ChannelFactory {
    
    private static ChannelAccess localAccess = null;
    private static ChannelAccess remoteAccess = null;
    
    /**
     * Create a channel.
     * @param name The channel name.
     * @param listener The listener for channel state changes.
     * @return The channel or null if it could not be created.
     */
    public static Channel createChannel(String name,ChannelStateListener listener) {
        Channel channel = null;
        if(localAccess!=null) {
            channel = localAccess.createChannel(name,listener);
            if(channel!=null) return channel;
        }
        if(remoteAccess!=null) {
            channel = remoteAccess.createChannel(name,listener);
        }
        return channel;
    }
    
    /**
     * Register the channel access for local channels. 
     * @param channelAccess The interface for the implementation.
     */
    public static void registerLocalChannelAccess(ChannelAccess channelAccess) {
        localAccess = channelAccess;
    }
    
    /**
     * Register the channel access for remote channels. 
     * @param channelAccess The interface for the implementation.
     */
    public static void registerRemoteChannelAccess(ChannelAccess channelAccess) {
        remoteAccess = channelAccess;
    }
}
