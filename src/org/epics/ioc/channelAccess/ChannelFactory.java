/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author mrk
 *
 */
public class ChannelFactory {
    
    private static AtomicReference<ChannelAccess> localAccess = new AtomicReference<ChannelAccess>();
    private static AtomicReference<ChannelAccess> remoteAccess = new AtomicReference<ChannelAccess>();
    
    public static Channel createChannel(String name) {
        Channel channel = null;
        ChannelAccess access = localAccess.get();
        if(access!=null) {
            channel = access.createChannel(name);
            if(channel!=null) return channel;
        }
        access = remoteAccess.get();
        if(access!=null) {
            channel = access.createChannel(name);
        }
        return channel;
    }
    
    public static boolean registerLocalChannelAccess(ChannelAccess channelAccess) {
        return localAccess.compareAndSet(null,channelAccess);
    }
    
    public static boolean registerRemoteChannelAccess(ChannelAccess channelAccess) {
        return remoteAccess.compareAndSet(null,channelAccess);
    }
}
