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
public interface ChannelPutRequestor extends ChannelRequestor {
    /**
     * Provide the next set of data to put to the channel.
     * The requestor is expected to call the put method.
     * @param channel The channel.
     * @param field The field.
     * @param data The interface for putting data.
     * @return (false,true) if the requestor (has,has not) obtained all the data.
     * A value of true means that the requestor wants to be called again for this data.
     * This normally means that an array is being transfered and the requestor is
     * not able to handle the array as a single chunk of data.
     */
    boolean nextPutData(Channel channel,ChannelField field,PVData data);
}
