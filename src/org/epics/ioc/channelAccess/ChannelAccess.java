/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * interface for creating and destroying a channel.
 * @author mrk
 *
 */
public interface ChannelAccess {
    /**
     * create a channel.
     * 
     * @param name the channel name.
     * @return a Channel or null if the channel can not be created.
     */
    Channel createChannel(String name);
}
