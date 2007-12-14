/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.Map;
import java.util.TreeMap;

import org.epics.ioc.util.MessageType;


/**
 * Factory for creating channels. 
 * @author mrk
 *
 */
public class ChannelFactory {   
    /**
     * Create a channel.
     * @param pvName The process variable name.
     * @param providerName The name of the channel provider.
     * @param listener The listener for channel state changes.
     * @return The channel or null if it could not be created.
     */
    public static Channel createChannel(String pvName,String providerName, ChannelStateListener listener) {
        ChannelProvider channelProvider = channelProviders.get(providerName);
        if(channelProvider==null) {
            listener.message(
                "providerName " + providerName + " not a registered provider",
                MessageType.error);
        }
        return channelProvider.createChannel(pvName, listener);
    }
    /**
     * Register a channelProvider.
     * @param channelProvider The channelProvider.
     */
    public static void registerChannelProvider(ChannelProvider channelProvider) {
        channelProviders.put(channelProvider.getProviderName(), channelProvider);
    }
    /**
     * Get the ChannelProvider.
     * @param providerName The providerName.
     * @return The ChannelProvider interface or null if the providerName is not registered.
     */
    public static ChannelProvider getChannelProvider(String providerName) {
        return channelProviders.get(providerName);
    }
    /**
     * Does providerName provide a channel for channelName?
     * @param channelName The channelName.
     * @param providerName The provixderName.
     * @return (false,true) if (does not, does) provide a channel for channelName.
     */
    public static boolean isChannelProvider(String channelName,String providerName) {
        ChannelProvider channelProvider = channelProviders.get(providerName);
        if(channelProvider==null) return false;
        return channelProvider.isProvider(channelName);
        
    }
    
    private static Map<String,ChannelProvider> channelProviders = new TreeMap<String,ChannelProvider>();
    static {
        ChannelProviderLocalFactory.register();
    }
}
