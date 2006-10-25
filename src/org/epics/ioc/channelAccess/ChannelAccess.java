/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbAccess.DBLink;

/**
 * Interface for creating and destroying a channel.
 * @author mrk
 *
 */
public interface ChannelAccess {
    /**
     * Create a channel.
     * 
     * @param name The channel name.
     * @param listener A state listener.
     * @return A Channel or null if the channel can not be created.
     */
    Channel createChannel(String name,ChannelStateListener listener);
    /**
     * Create a channel for an IOC database link.
     * @param dbLink The database link.
     * @param name The channel name.
     * @param listener The channel state listener.
     * @return The created channel or null if it could not be created.
     */
    Channel createLinkChannel(DBLink dbLink,String name,ChannelStateListener listener);
}
