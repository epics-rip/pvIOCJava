/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.PVCopy;

/**
 * @author mrk
 *
 */
public class MonitorOnPutFactory{
    private static final String name = "onPut";
    private static final MonitorOnPut monitorOnPut = new MonitorOnPut();

    public static void start() {
        ChannelProviderLocalFactory.registerMonitor(monitorOnPut);
    }

    private static class MonitorOnPut implements MonitorCreate {

        public ChannelMonitor create(
                ChannelMonitorRequester channelMonitorRequester,
                PVStructure pvOption,
                PVCopy pvCopy,
                byte queueSize,
                Executor executor)
        {
            return new MonitorPut(channelMonitorRequester,pvCopy,queueSize,executor);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorCreate#getName()
         */
        @Override
        public String getName() {
            return name;
        }
    }

    private static class MonitorPut extends BaseMonitor {
        private MonitorPut(
                ChannelMonitorRequester channelMonitorRequester,
                PVCopy pvCopy,
                byte queueSize,
                Executor executor)
        {
            super(channelMonitorRequester,pvCopy,queueSize,executor);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.BaseMonitor#generateMonitor(org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement)
         */
        @Override
        protected boolean generateMonitor(MonitorQueueElement monitorQueueElement) {
            return true;
        }

    }
}
