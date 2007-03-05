/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * Result for a channel.findField request.
 * @author mrk
 *
 */
public enum ChannelFindFieldResult {
    /**
     * The requested field is located via another channel.
     * Calls to getOtherChannel and getOtherField can be used to connect to the channel and field.
     */
    otherChannel,
    /**
     * The requested field is in this channel.
     * getChannelField can be called to retrieve the ChannelField interface.
     */
    thisChannel,
    /**
     * The field could not be found.
     */
    notFound,
    /**
     * Failure. A common reason for failure is that the channel was destroyed.
     */
    failure
}
