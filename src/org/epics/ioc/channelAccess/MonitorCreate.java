/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.PVCopy;

/**
 * An interface implemented by code that implements a monitor algorithm.
 * @author mrk
 *
 */
public interface MonitorCreate {
    /**
     * Get the name of the algorithm.
     * @return The name.
     */
    String getName();
    /**
     * Create a ChannelMonitor.
     * @param channel The channel;
     * @param channelMonitorRequester The requester.
     * @param pvOption Options for the algorithm.
     * @param pvCopy The PVCopy that maps to a subset of the fields in a PVBRecord.
     * @param queueSize The queuesize.
     * @param executor An Executor for calling the requester.
     * @return The ChannelMonitor interface.
     */
    ChannelMonitor create(
            Channel channel,
            ChannelMonitorRequester channelMonitorRequester,
            PVStructure pvOption,
            PVCopy pvCopy,
            byte queueSize,
            Executor executor);
}
