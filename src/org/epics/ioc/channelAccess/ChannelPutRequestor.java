/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.PVData;

/**
 * Listener for ChannelPut requests.
 * @author mrk
 *
 */
public interface ChannelPutRequestor extends ChannelProcessRequestor {
    /**
     * Provide the next set of data to put to the channel.
     * The listener is expected to call the put method.
     * @param channel The channel.
     * @param field The field.
     * @param data The interface for putting data.
     */
    void nextData(Channel channel,ChannelField field,PVData data);
}
