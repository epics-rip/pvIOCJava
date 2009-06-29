/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.PVCopy;

/**
 * @author mrk
 *
 */
public class MonitorOnChangeFactory {
    private static final String name = "onChange";
    private static final MonitorOnChange monitorOnChange = new MonitorOnChange();
    private static final PVDataCreate pvDataCreate= PVDataFactory.getPVDataCreate();
    private static final Convert convert = ConvertFactory.getConvert();

    public static void start() {
        ChannelProviderLocalFactory.registerMonitor(monitorOnChange);
    }

    private static class MonitorOnChange implements MonitorCreate {
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorCreate#create(org.epics.pvData.channelAccess.ChannelMonitorRequester, org.epics.pvData.pv.PVStructure, org.epics.pvData.pvCopy.PVCopy, byte, org.epics.pvData.misc.Executor)
         */
        public ChannelMonitor create(
                ChannelMonitorRequester channelMonitorRequester,
                PVStructure pvOption,
                PVCopy pvCopy,
                byte queueSize,
                Executor executor)
        {
            PVStructure pvStructure = pvCopy.createPVStructure();
            PVField pvField = pvStructure.getSubField("value");
            if(pvField==null) {
                channelMonitorRequester.message("value field not defined", MessageType.error);
                return null;
            }
            pvField = pvCopy.getRecordPVField(pvField.getFieldOffset());
            return new Monitor(channelMonitorRequester,pvCopy,queueSize,executor,pvField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorCreate#getName()
         */
        @Override
        public String getName() {
            return name;
        }
    }
    
    
    private static class Monitor extends BaseMonitor {
        private Monitor(
                ChannelMonitorRequester channelMonitorRequester,
                PVCopy pvCopy,
                byte queueSize,
                Executor executor,
                PVField valuePVField)
        {
            super(channelMonitorRequester,pvCopy,queueSize,executor);
            this.valuePVField = valuePVField;
            pvPrev = pvDataCreate.createPVField(null, null, valuePVField);
        }
        
        private PVField valuePVField;
        private PVField pvPrev;
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.BaseMonitor#generateMonitor(org.epics.ioc.channelAccess.MonitorQueue.MonitorQueueElement)
         */
        @Override
        protected boolean generateMonitor(MonitorQueueElement monitorQueueElement) {
            if(valuePVField.equals(pvPrev)) return false;
            convert.copy(valuePVField, pvPrev);   
            return true;
        }
        
    }
}
