/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;

/**
 * Listener for a get request.
 * @author mrk
 *
 */
public interface ChannelGetListener extends ChannelRequestListener {
    /**
     * A set of synchronous data is coming.
     * @param channel The channel which has tye data.
     */
    void beginSynchronous(Channel channel);
    /**
     * End of a set of synchronous data. This is also the end of the request. 
     * @param channel The channel.
     */
    void endSynchronous(Channel channel);
    /**
     * Data.
     * @param channel The channel.
     * @param field The field.
     * @param data The data.
     */
    void newData(Channel channel,ChannelField field,PVData data);
}
