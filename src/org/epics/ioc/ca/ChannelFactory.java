/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.db.IOCDBFactory;


/**
 * Factory for creating channels. 
 * @author mrk
 *
 */
public class ChannelFactory {
    
    private static ChannelAccess localAccess = null;
    private static ChannelAccess remoteAccess = null;
    
    /**
     * Is the name a local process variable.
     * @param pvName The process variable name.
     * This is of the form recordName.name.name...
     * @return (false,true) id the recordName (is not,is)
     * the name of a record in the local IOC.
     */
    public static boolean isLocalChannel(String pvName) {
        int index = pvName.indexOf('.');
        String recordName = pvName;
        if(index>=0) recordName = pvName.substring(0,index);
        return IOCDBFactory.getMaster().getRecordMap().containsKey(recordName) ;
    }
    /**
     * Create a channel.
     * @param pvName The process variable name.
     * @param listener The listener for channel state changes.
     * @param mustBeLocal If true the request succeeds only if the channel is local.
     * @return The channel or null if it could not be created.
     */
    public static Channel createChannel(String pvName,ChannelStateListener listener, boolean mustBeLocal) {
        if(isLocalChannel(pvName)) {
            if(localAccess==null) ChannelAccessLocalFactory.register();
            if(localAccess!=null) {
                return localAccess.createChannel(pvName,listener);
            }
            return null;
        }
        if(mustBeLocal) return null;
        if(remoteAccess!=null) {
            return remoteAccess.createChannel(pvName,listener);
        }
        return null;
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
