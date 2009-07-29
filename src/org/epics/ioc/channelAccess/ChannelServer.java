/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelProvider;

/**
 * Interface implemented by code that can provide access to the record
 * to which a channel connects.
 * @author mrk
 *
 */
public interface ChannelServer extends ChannelProvider{
    /**
     * Register an interface for creating monitors.
     * @param monitorCreate The interface.
     */
    void registerMonitor(MonitorCreate monitorCreate);
    /**
     * Destroy a monitor.
     * @param channel The channel to which the monitor is attached.
     * @param channelMonitor The channelMonitor.
     */
    void destroyMonitor(Channel channel,ChannelMonitor channelMonitor);
    /**
     * Register an interface for a channelProcessProvider.
     * @param channelProcessProvider The provider.
     * @return (false,true) if the provider was registered.
     */
    boolean registerChannelProcessProvider(ChannelProcessorProvider channelProcessProvider);
}
