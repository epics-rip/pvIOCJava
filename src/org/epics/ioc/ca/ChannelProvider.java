/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * Interface for creating and destroying a channel.
 * @author mrk
 *
 */
public interface ChannelProvider {
    /**
     * Create a channel.
     * 
     * @param pvName The channel name.
     * @param listener A state listener.
     * @return A Channel or null if the channel can not be created.
     */
    Channel createChannel(String pvName,ChannelStateListener listener);
    /**
     * Get the provider name.
     * @return The name.
     */
    String getProviderName();
    /**
     * Do you provide this channelName.
     * @param channelName The channelName.
     * @return (false,true) if the channelName is known to this provider.
     * This call may block while looking for the name.
     */
    boolean isProvider(String channelName);
}
