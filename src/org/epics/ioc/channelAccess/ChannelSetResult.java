/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * @author mrk
 *
 */
public enum ChannelSetResult {
    /**
     * the requested field is located via another channel.
     * calls to getRemoteChannel and getRemoteField can be used to connect to the channel and field.
     */
    otherChannel,
    /**
     * the requested field is in this channel.
     * getField can be called to retrieve the ChannelData interface.
     */
    thisChannel,
    /**
     * the field could not be found.
     */
    notFound
}
