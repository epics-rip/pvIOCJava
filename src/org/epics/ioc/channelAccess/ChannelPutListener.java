/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.ProcessResult;
import org.epics.ioc.pvAccess.PVData;
import org.epics.ioc.util.AlarmSeverity;

/**
 * Listener for ChannelPut requests.
 * @author mrk
 *
 */
public interface ChannelPutListener {
    /**
     * Provide the next set of data to put to the channel.
     * The listener is expected to call the put method.
     * @param channel The channel.
     * @param field The field.
     * @param data The interface for putting data.
     */
    void nextData(Channel channel,ChannelField field,PVData data);
    /**
     * The request is done.
     * @param channel The channel.
     * @param result The result of processing.
     * @param alarmSeverity The alarm severity after processing completed.
     * @param status The status after processing completed.
     */
    void processDone(Channel channel,ProcessResult result,AlarmSeverity alarmSeverity,String status);
    /**
     * The request failed.
     * @param channel The channel.
     * @param reason The reason.
     */
    void failure(Channel channel,String reason);
}
