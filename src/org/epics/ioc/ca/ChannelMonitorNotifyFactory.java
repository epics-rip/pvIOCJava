/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.MessageType;

/**
 * @author mrk
 *
 */
public class ChannelMonitorNotifyFactory {

    public static ChannelMonitorNotify create(Channel channel,ChannelMonitorNotifyRequester monitorNotifyRequestor) {
        return new MonitorNotifyImpl(channel,monitorNotifyRequestor);
    }


    private static class MonitorNotifyImpl  implements ChannelMonitorRequester,ChannelMonitorNotify  {

        private MonitorNotifyImpl(Channel channel,ChannelMonitorNotifyRequester monitorNotifyRequestor){
            this.channel = channel;
            this.monitorNotifyRequestor = monitorNotifyRequestor;
        }
        
        private ChannelFieldGroup channelFieldGroup = null;
        private Channel channel;
        private ChannelMonitorNotifyRequester monitorNotifyRequestor;
        private ChannelMonitor channelMonitor = null;
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#destroy()
         */
        public void destroy() {
            stop();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return monitorNotifyRequestor.getRequesterName();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            monitorNotifyRequestor.message(message, messageType);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#getData(org.epics.ioc.ca.CD)
         */
        public void getData(CD cd) {
            channelMonitor.getData(cd);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#setFieldGroup(org.epics.ioc.ca.ChannelFieldGroup)
         */
        public void setFieldGroup(ChannelFieldGroup channelFieldGroup) {
            this.channelFieldGroup = channelFieldGroup;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#start()
         */
        public void start() {
            channelMonitor = channel.createChannelMonitor(this);
            channelMonitor.setFieldGroup(channelFieldGroup);
            channelMonitor.start();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#stop()
         */
        public void stop() {
            channelMonitor.stop();
            channelMonitor.destroy();
            channelMonitor = null;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#beginPut()
         */
        public void beginPut() {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
         */
        public void dataPut(PVField requestedPVField, PVField pvField) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#dataPut(org.epics.ioc.pv.PVField)
         */
        public void dataPut(PVField pvField) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#endPut()
         */
        public void endPut() {
            monitorNotifyRequestor.monitorEvent();
        }
    }
}
