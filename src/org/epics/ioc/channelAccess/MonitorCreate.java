/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.PVCopy;

/**
 * @author mrk
 *
 */
public interface MonitorCreate {
    String getName();
    ChannelMonitor create(
            ChannelMonitorRequester channelMonitorRequester,
            PVStructure pvOption,
            PVCopy pvCopy,
            byte queueSize,
            Executor executor);
}
