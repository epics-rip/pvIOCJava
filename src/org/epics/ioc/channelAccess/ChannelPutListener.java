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
 * @author mrk
 *
 */
public interface ChannelPutListener {
    void nextData(Channel channel,ChannelField field,PVData data);
    void processDone(Channel channel,ProcessResult result,AlarmSeverity alarmSeverity,String status);
    void failure(Channel channel,String reason);
}
