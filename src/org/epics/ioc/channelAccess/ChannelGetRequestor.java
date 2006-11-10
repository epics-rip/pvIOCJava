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
public interface ChannelGetRequestor extends ChannelRequestor {
    /**
     * Next data for a get request..
     * @param channel The channel.
     * @param field The field.
     * @param data The data.
     * @return (false,true) if the requestor (has,has not) obtained all the data.
     * A value of true means that the requestor wants to be called again for this data.
     * This normally means that an array is being transfered and the requestor is
     * not able to handle the array as a single chunk of data. 
     */
    boolean nextGetData(Channel channel,ChannelField field,PVData data);
}
