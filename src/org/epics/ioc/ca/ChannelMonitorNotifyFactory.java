/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;


/**
 * @author mrk
 *
 */
public class ChannelMonitorNotifyFactory {

    public static ChannelMonitorNotify create(Channel channel,ChannelMonitorNotifyRequester monitorNotifyRequestor) {
        return new MonitorNotifyImpl(channel,monitorNotifyRequestor);
    }


    private static class MonitorNotifyImpl  implements ChannelMonitorNotify, PVListener  {
        private Channel channel;
        private ChannelFieldGroup channelFieldGroup = null;
        private ChannelField[] channelFields = null;
        private ChannelMonitorNotifyRequester monitorNotifyRequestor;
        private boolean groupPutActive = false;
        private boolean monitorOccured = false;
        
        private MonitorNotifyImpl(Channel channel,ChannelMonitorNotifyRequester monitorNotifyRequestor){
            this.channel = channel;
            this.monitorNotifyRequestor = monitorNotifyRequestor;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#destroy()
         */
        public void destroy() {
            monitorNotifyRequestor.unlisten();
            stop();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#setFieldGroup(org.epics.ioc.ca.ChannelFieldGroup)
         */
        @Override
        public void setFieldGroup(ChannelFieldGroup channelFieldGroup) {
            this.channelFieldGroup = channelFieldGroup;
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#start()
         */
        @Override
        public void start() {
            channelFields = channelFieldGroup.getArray();
            PVRecord pvRecord = channel.getPVRecord();
            pvRecord.registerListener(this);
            for(ChannelField channelField : channelFields) {
                PVField pvField = channelField.getPVField();
                pvField.addListener(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitor#stop()
         */
        public void stop() {
            PVRecord pvRecord = channel.getPVRecord();
            pvRecord.unregisterListener(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#beginGroupPut(org.epics.pvData.pv.PVRecord)
         */
        public void beginGroupPut(PVRecord pvRecord) {
            groupPutActive = true;
            monitorOccured = false;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVField)
         */
        public void dataPut(PVField pvField) {
            if(groupPutActive) {
                monitorOccured = true;
                return;
            }
            monitorNotifyRequestor.monitorEvent();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVField)
         */
        public void dataPut(PVStructure requested, PVField pvField) {
            if(groupPutActive) {
                monitorOccured = true;
                return;
            }
            monitorNotifyRequestor.monitorEvent();
            
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
         */
        public void endGroupPut(PVRecord pvRecord) {
            if(monitorOccured) monitorNotifyRequestor.monitorEvent();
            groupPutActive = false;
            monitorOccured = false;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
         */
        public void unlisten(PVRecord pvRecord) {
            monitorNotifyRequestor.unlisten();
        }
    }
}
