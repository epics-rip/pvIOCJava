/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.PVData;
import org.epics.ioc.util.*;


/**
 * Listener for a subscription request that wants data as well as notification.
 * @author mrk
 *
 */
public interface ChannelNotifyGetRequestor extends Requestor{
    /**
     * A message for requester.
     * @param channel The channel.
     * @param message The message.
     * @param messageType The type of message.
     */
    void message(Channel channel,String message,MessageType messageType);
    /**
     * A new set of data is available.
     * The requestor calls ChannelSubscribe.readyForData when ready to receive the new data.
     */
    void startNotifyGetData();
    /**
     * A number of sets of data have been discarded. 
     * @param numberSets The number of sets of data that have been discarded.
     */
    void dataOverrun(int numberSets);
    /**
     * New notify data value.
     * @param channel The channel.
     * @param field The field.
     * @param data The data.
     * @param last Is this the last data item for this notify request?
     */
    void nextNotifyGetData(Channel channel,ChannelField field,PVData data,boolean last);
}
