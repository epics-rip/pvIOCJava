/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

/**
 * Listener for field group changes.
 * @author mrk
 *
 */

public interface ChannelFieldGroupListener {
    /**
     * Access rights have changed.
     * @param channel The channel.
     * @param channelField The channel field.
     */
    void accessRightsChange(Channel channel,ChannelField channelField);
}
