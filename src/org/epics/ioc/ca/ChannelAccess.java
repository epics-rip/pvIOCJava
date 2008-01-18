/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;


/**
 * Interface returned by ChannelAccessFactory.
 * @author mrk
 *
 */
public interface ChannelAccess {
    /**
     * Create a channel.
     * @param pvName The process variable name.
     * @param propertys The array of desired properties.
     * @param providerName The name of the channel provider.
     * @param listener The listener for channel state changes.
     * @return The channel or null if it could not be created.
     */
    Channel createChannel(String pvName,String[] propertys,String providerName, ChannelListener listener);
    /**
     * Register a channelProvider.
     * @param channelProvider The channelProvider.
     */
    void registerChannelProvider(ChannelProvider channelProvider);
    /**
     * Get the ChannelProvider.
     * @param providerName The providerName.
     * @return The ChannelProvider interface or null if the providerName is not registered.
     */
    ChannelProvider getChannelProvider(String providerName);
    /**
     * Get the array of all the channelProviders.
     * @return The array.
     */
    ChannelProvider[] getChannelProviders();
    /**
     * Does providerName provide a channel for channelName?
     * @param channelName The channelName.
     * @param providerName The provixderName.
     * @return (false,true) if (does not, does) provide a channel for channelName.
     */
    boolean isChannelProvider(String channelName,String providerName);
}
