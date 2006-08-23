/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * @author mrk
 *
 */
public class ChannelFactory {
    
    private static ChannelAccess localAccess = null;
    private static ChannelAccess remoteAccess = null;
    
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
    
    public static void registerLocalChannelAccess(ChannelAccess channelAccess) {
        localAccess = channelAccess;
    }
    
    public static void registerRemoteChannelAccess(ChannelAccess channelAccess) {
        remoteAccess = channelAccess;
    }
}
