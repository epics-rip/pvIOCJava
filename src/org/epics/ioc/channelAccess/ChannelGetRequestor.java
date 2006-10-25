/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;

/**
 * Requestor for a ChannelGet.get request.
 * @author mrk
 *
 */
public interface ChannelGetRequestor extends ChannelProcessRequestor {
    /**
     * Data.
     * @param channel The channel.
     * @param field The field.
     * @param data The data.
     */
    void newData(Channel channel,ChannelField field,PVData data);
}
