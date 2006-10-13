/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;

/**
 * Base interface for channel requests.
 * @author mrk
 *
 */
public interface ChannelRequestListener {
    /**
     * The result of the process request.
     * @param channel The channel.
     * @param alarmSeverity The alarm Severity after processing.
     * @param status The status after processing.
     * @param timeStamp The time stamp after processing.
     */
    void requestResult(Channel channel,
            AlarmSeverity alarmSeverity,String status,TimeStamp timeStamp);
    /**
     * A message for requester.
     * @param channel The channel.
     * @param message The message.
     */
    void message(Channel channel,String message);
    /**
     * The request is done. This is always called with no locks held.
     * This is only called if the request is asynchronous,
     * i.e. if the original request returned ChannelRequestReturn.active.
     * @param channel The channel.
     */
    void requestDone(Channel channel);
}
