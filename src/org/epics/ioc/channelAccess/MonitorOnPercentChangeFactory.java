/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;

import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.Type;
import org.epics.pvData.pvCopy.PVCopy;

/**
 * @author mrk
 *
 */
public class MonitorOnPercentChangeFactory {
    private static final String name = "onPercentChange";
    private static final MonitorOnPercent monitorOnPercent = new MonitorOnPercent();
    private static final ChannelProvider channelProvider = ChannelProviderFactory.getChannelProvider();

    public static void start() {
        channelProvider.registerMonitor(monitorOnPercent);
    }

    private static class MonitorOnPercent implements MonitorCreate {
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.MonitorCreate#create(org.epics.pvData.channelAccess.ChannelMonitorRequester, org.epics.pvData.pv.PVStructure, org.epics.pvData.pvCopy.PVCopy, byte, org.epics.pvData.misc.Executor)
         */
        public ChannelMonitor create(
                Channel channel,
                ChannelMonitorRequester channelMonitorRequester,
                PVStructure pvOption,
                PVCopy pvCopy,
                byte queueSize,
                Executor executor)
        {
            PVStructure pvStructure = pvCopy.createPVStructure();
            PVDouble pvDeadband = pvOption.getDoubleField("deadband");
            if(pvDeadband==null) {
                channelMonitorRequester.message("deadband field not defined", MessageType.error);
                return null;
            }
            PVField pvField = pvStructure.getSubField("value");
            if(pvField==null) {
                channelMonitorRequester.message("value field not defined", MessageType.error);
                return null;
            }
            if(pvField.getField().getType()!=Type.scalar) {
                channelMonitorRequester.message("value is not a scalar", MessageType.error);
                return null;
            }
            Scalar scalar = (Scalar)pvField.getField();
            if(!scalar.getScalarType().isNumeric()) {
                channelMonitorRequester.message("value is not a numeric scalar", MessageType.error);
                return null;
            }
            pvField = pvCopy.getRecordPVField(pvField.getFieldOffset());
            return new Monitor(channel,channelMonitorRequester,pvCopy,queueSize,executor,pvDeadband.get(),(PVScalar)pvField);
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
                Channel channel,
                ChannelMonitorRequester channelMonitorRequester,
                PVCopy pvCopy,
                byte queueSize,
                Executor executor,
                double deadband,
                PVScalar valuePVField)
        {
            super(channel,channelMonitorRequester,pvCopy,queueSize,executor);
            this.deadband = deadband;
            this.valuePVField = valuePVField;
            prevValue = convert.toDouble(valuePVField);
        }
        
        private double deadband = 0.0;
        private PVScalar valuePVField;
        private double prevValue = 0.0;
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.BaseMonitor#generateMonitor(java.util.BitSet)
         */
        @Override
        protected boolean generateMonitor(BitSet changeBitSet) {
            double value = convert.toDouble(valuePVField);
            double diff = value - prevValue;
            if(value!=0.0) {
                if((100.0*Math.abs(diff)/Math.abs(value)) < deadband) return false;
            }
            prevValue = value;
            return true;
        }
    }
}
