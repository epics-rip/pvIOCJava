/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.ArrayList;
import java.util.List;

import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVFloat;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVLong;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.MessageType;

/**
 * Factory that implements CDMonitor.
 * @author mrk
 *
 */
public class CDMonitorFactory {

    /**
     * Create a ChannelMonitor.
     * @param channel The channel to monitor.
     * @param cdMonitorRequester The CDMonitorRequester.
     * @return The CDMonitor.
     */
    public static CDMonitor create(Channel channel,CDMonitorRequester cdMonitorRequester)
    {
        return new CDMonitorImpl(channel,cdMonitorRequester);
    }

    
    private static Convert convert = ConvertFactory.getConvert();

    private static class CDMonitorImpl
    implements CDMonitor, ChannelMonitorRequester, ChannelFieldGroupListener
    {

        private CDMonitorImpl(Channel channel,CDMonitorRequester cdMonitorRequester)
        {
            this.channel = channel;
            this.cdMonitorRequester = cdMonitorRequester;
        }
        
        private Channel channel;
        private CDMonitorRequester cdMonitorRequester;
        private IOCExecutor iocExecutor = null;
        private CDQueue cdQueue = null;    
        private CallRequester callRequester = null;;    
        private ChannelMonitor channelMonitor = null;
        private ArrayList<ChannelField> channelFieldList = new ArrayList<ChannelField>();
        private ArrayList<MonitorField> monitorFieldList = new ArrayList<MonitorField>();
        private ChannelFieldGroup channelFieldGroup;
        private CD cd = null;
        private boolean monitorOccured = true;
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForAbsoluteChange(org.epics.ioc.ca.ChannelField, double)
         */
        public void lookForAbsoluteChange(ChannelField channelField, double value) {
            Type type = channelField.getField().getType();
            if(!type.isNumeric()) {
                message("field is not a numeric scalar", MessageType.error);
                lookForPut(channelField,true);
                return;
            }
            MonitorField monitorField
                = new MonitorField(MonitorType.absoluteChange,type,value);
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForChange(org.epics.ioc.ca.ChannelField, boolean)
         */
        public void lookForChange(ChannelField channelField, boolean causeMonitor) {
            Type type = channelField.getField().getType();
            if(!type.isPrimitive()) {
                message("field is not primitive", MessageType.error);
                lookForPut(channelField,causeMonitor);
                return;
            }
            MonitorField monitorField = new MonitorField(type,causeMonitor);
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForPercentageChange(org.epics.ioc.ca.ChannelField, double)
         */
        public void lookForPercentageChange(ChannelField channelField, double value) {
            Type type = channelField.getField().getType();
            if(!type.isNumeric()) {
                message("field is not a numeric scalar", MessageType.error);
                lookForPut(channelField,true);
                return;
            }
            MonitorField monitorField
                = new MonitorField(MonitorType.percentageChange,type,value);
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForPut(org.epics.ioc.ca.ChannelField, boolean)
         */
        public void lookForPut(ChannelField channelField, boolean causeMonitor) {
            MonitorField monitorField = new MonitorField(causeMonitor);
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#start(int, org.epics.ioc.util.IOCExecutor)
         */
        public void start(int queueSize, IOCExecutor iocExecutor) {
            
            channelFieldGroup = channel.createFieldGroup(this);
            for(int i=0; i<channelFieldList.size(); i++) {
                channelFieldGroup.addChannelField(channelFieldList.get(i));
            }
            cdQueue = CDFactory.createCDQueue(queueSize, channel, channelFieldGroup);
            this.iocExecutor = iocExecutor;
            callRequester = new CallRequester();
            CD initialData = cdQueue.getFree(true);
            initialData.clearNumPuts();
            
            channelMonitor = channel.createChannelMonitor(this);
            channelMonitor.setFieldGroup(channelFieldGroup);
            channelMonitor.getData(initialData);            
            List<ChannelField> channelFieldList = initialData.getChannelFieldGroup().getList();
            for(int i=0; i<channelFieldList.size(); i++) {
                ChannelField channelField = channelFieldList.get(i);
                MonitorField monitorField = monitorFieldList.get(i);
                monitorField.initField(channelField.getPVField());
            }
            cd = cdQueue.getFree(true);
            List<ChannelField> initialList = initialData.getChannelFieldGroup().getList();
            for(int i=0; i<initialList.size(); i++) {
                cd.put(initialList.get(i).getPVField());
                
            }
            cdQueue.setInUse(initialData);
            callRequester.call();
            cd.clearNumPuts();
            monitorOccured = false;
            channelMonitor.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.MonitorCDPut#stop()
         */
        public void stop() {
            if(channelMonitor!=null) channelMonitor.stop();
            channelFieldGroup = null;
            channelMonitor = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return cdMonitorRequester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            cdMonitorRequester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#beginPut()
         */
        public void beginPut() {
            monitorOccured = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
         */
        public void dataPut(PVField requestedPVField, PVField modifiedPVField) {
            boolean result = cd.put(requestedPVField, modifiedPVField);
            // following allows for client monitoring a direct subfield of a field
            // This is common for enumerated fields
            if(!result) {
                String fieldName = modifiedPVField.getField().getFieldName();
                if(fieldName!=null) {
                int index =cd.getCDRecord().getPVRecord().getStructure().getFieldIndex(fieldName);
                   if(index>=0) result = cd.put(modifiedPVField);
                }
            }
            if(result) {
                monitorOccured = true;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#dataPut(org.epics.ioc.pv.PVField)
         */
        public void dataPut(PVField modifiedPVField) {
            boolean result = cd.put(modifiedPVField);
            if(result) for(int i=0; i < channelFieldList.size(); i++) {
                ChannelField channelField = channelFieldList.get(i);
                PVField data = channelField.getPVField();
                if(data==modifiedPVField) {
                    MonitorField monitorField = monitorFieldList.get(i);
                    result = monitorField.newField(modifiedPVField);
                    if(result) monitorOccured = true;
                    return;
                }
            }
            message("Logic error: newField did not find pvField.",MessageType.error);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#endPut()
         */
        public void endPut() {
            if(monitorOccured) {
                CD initial = cd;
                cd = cdQueue.getFree(true);
                List<ChannelField> initialList = initial.getChannelFieldGroup().getList();
                for(int i=0; i<initialList.size(); i++) {
                    cd.put(initialList.get(i).getPVField());
                    
                }
                cd.clearNumPuts();
                cdQueue.setInUse(initial);
                callRequester.call();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // Nothing to do until access rights implemented
        }
        
        // The calls requester via a separate thread.
        private class CallRequester implements Runnable {

            private CallRequester(){}         

            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                int number = 0;
                while(true) {
                    CD cd = cdQueue.getNext();
                    if(cd==null) {
                        if(number!=1) {
                            cdMonitorRequester.message(
                                " dequeued " + number + " monitors",
                                MessageType.info);
                        }
                        return;
                    }
                    number++;
                    int missed = cdQueue.getNumberMissed();
                    if(missed>0) cdMonitorRequester.dataOverrun(missed);
                    cdMonitorRequester.monitorCD(cd);
                    cdQueue.releaseNext(cd);
                }
            }
            private void call() {
                iocExecutor.execute(this);
            }

        }
        
        private enum MonitorType {
            onPut,
            onChange,
            absoluteChange,
            percentageChange
        }
        
        private static class MonitorField {
            private MonitorType monitorType;
            private Type type = null;
            private boolean causeMonitor;
            
            private boolean lastBooleanValue;
            private byte lastByteValue;
            private short lastShortValue;
            private int lastIntValue;
            private long lastLongValue;
            private float lastFloatValue;
            private double lastDoubleValue;
            private String lastStringValue;
            
            private double deadband;
            private double lastMonitorValue = 0.0;
    
            private MonitorField(boolean causeMonitor) {
                this.monitorType = MonitorType.onPut;
                this.causeMonitor = causeMonitor;
            }
            private MonitorField(Type type,boolean causeMonitor) {
                this.monitorType = MonitorType.onChange;
                this.type = type;
                this.causeMonitor = causeMonitor;
            }
            private MonitorField(MonitorType monitorType, Type type, double deadband) {
                causeMonitor = true;
                this.monitorType = monitorType;
                this.type = type;
                this.deadband = deadband;
            }
            
            private void initField(PVField pvField) {
                if(monitorType==MonitorType.onPut) return;
                if(monitorType==MonitorType.onChange) {
                    switch(type) {
                    case pvBoolean: {
                            PVBoolean data= (PVBoolean)pvField;
                            lastBooleanValue = data.get();
                            return;
                        }
                    case pvByte: {
                            PVByte data= (PVByte)pvField;
                            lastByteValue = data.get();
                            return;
                        }
                    case pvShort: {
                            PVShort data= (PVShort)pvField;
                            lastShortValue = data.get();
                            return;
                        }
                    case pvInt: {
                            PVInt data= (PVInt)pvField;
                            lastIntValue = data.get();
                            return;
                        }
                    case pvLong: {
                            PVLong data= (PVLong)pvField;
                            lastLongValue = data.get();
                            return;
                        }
                    case pvFloat: {
                            PVFloat data= (PVFloat)pvField;
                            lastFloatValue = data.get();
                            return;
                        }
                    case pvDouble: {
                            PVDouble data= (PVDouble)pvField;
                            lastDoubleValue = data.get();
                            return;
                        }
                    case pvString : {
                        PVString data= (PVString)pvField;
                        lastStringValue = data.get();
                        return;
                    }                       
                    default:
                        throw new IllegalStateException("Logic error. Why is type not numeric?");      
                    }
                }
                lastMonitorValue = convert.toDouble(pvField);
            }
            
            private boolean newField(PVField pvField) {
                if(monitorType==MonitorType.onPut) {
                    if(causeMonitor) return true;
                    return false;
                }
                if(monitorType==MonitorType.onChange) {
                    switch(type) {
                    case pvBoolean: {
                            PVBoolean pvData= (PVBoolean)pvField;
                            boolean data = pvData.get();
                            if(data==lastBooleanValue) return false;
                            lastBooleanValue = data;
                            return true;
                        }
                    case pvByte: {
                            PVByte pvData= (PVByte)pvField;
                            byte data = pvData.get();
                            if(data==lastByteValue) return false;
                            lastByteValue = data;
                            return true;
                        }
                    case pvShort: {
                            PVShort pvData= (PVShort)pvField;
                            short data = pvData.get();
                            if(data==lastShortValue) return false;
                            lastShortValue = data;
                            return true;
                        }
                    case pvInt: {
                            PVInt pvData= (PVInt)pvField;
                            int data = pvData.get();
                            if(data==lastIntValue) return false;
                            lastIntValue = data;
                            return true;
                        }
                    case pvLong: {
                            PVLong pvData= (PVLong)pvField;
                            long data = pvData.get();
                            if(data==lastLongValue) return false;
                            lastLongValue = data;
                            return true;
                        }
                    case pvFloat: {
                            PVFloat pvData= (PVFloat)pvField;
                            float data = pvData.get();
                            if(data==lastFloatValue) return false;
                            lastFloatValue = data;
                            return true;
                        }
                    case pvDouble: {
                            PVDouble pvData= (PVDouble)pvField;
                            double data = pvData.get();
                            if(data==lastDoubleValue) return false;
                            lastDoubleValue = data;
                            return true;
                        }
                    case pvString : {
                            PVString pvData= (PVString)pvField;
                            String data = pvData.get();
                            if(data==lastStringValue) return false;
                            if(data.equals(lastStringValue)) return false;
                            lastStringValue = data;
                            return true;
                        }                       
                    default:
                        throw new IllegalStateException("Logic error. Why is type invalid?");      
                    }
                }
                double newValue;
                switch(type) {
                case pvByte: {
                        PVByte data= (PVByte)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvShort: {
                        PVShort data= (PVShort)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvInt: {
                        PVInt data= (PVInt)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvLong: {
                        PVLong data= (PVLong)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvFloat: {
                        PVFloat data= (PVFloat)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvDouble: {
                        PVDouble data= (PVDouble)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                default:
                    throw new IllegalStateException("Logic error. Why is type not numeric?");      
                } 
                double diff = newValue - lastMonitorValue;
                if(monitorType==MonitorType.absoluteChange) {
                    if(Math.abs(diff) >= deadband) {
                        lastMonitorValue = newValue;
                        return true;
                    }
                    return false;
                }
                double lastValue = lastMonitorValue;
                if(lastValue!=0.0) {
                    if((100.0*Math.abs(diff)/Math.abs(lastValue)) < deadband) return false;
                }
                lastMonitorValue = newValue;
                return true;
            }
            
        }
    }  
}
