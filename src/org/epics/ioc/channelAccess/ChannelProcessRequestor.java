/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.TimeStamp;

/**
 * Requestor for a ChannelProcess.
 * @author mrk
 *
 */
public interface ChannelProcessRequestor extends ChannelRequestor {
    /**
     * The result of the process request.
     * @param channel The channel.
     * @param alarmSeverity The alarm Severity after processing.
     * @param status The status after processing.
     * @param timeStamp The time stamp after processing.
     */
    void processResult(Channel channel,
            AlarmSeverity alarmSeverity,String status,TimeStamp timeStamp);
}
