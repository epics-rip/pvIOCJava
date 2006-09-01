/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.util.*;

/**
 * Listener for a process request.
 * @author mrk
 *
 */
public interface ChannelProcessListener {
    /**
     * The process request is done.
     * @param channel The channel.
     * @param result The result.
     * @param alarmSeverity The alarm Severity after processing.
     * @param status The status after processing.
     */
    void processDone(Channel channel,ProcessResult result,AlarmSeverity alarmSeverity,String status);
    /**
     * The process request failed.
     * @param channel The channel.
     * @param reason The reason.
     */
    void failure(Channel channel,String reason);
}
