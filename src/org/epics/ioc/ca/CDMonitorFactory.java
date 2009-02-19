/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.ArrayList;
import java.util.List;

import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVFloat;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVShort;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

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
    implements CDMonitor,ChannelMonitorRequester, PVListener, ChannelFieldGroupListener
    {

        private CDMonitorImpl(Channel channel,CDMonitorRequester cdMonitorRequester)
        {
            this.channel = channel;
            this.cdMonitorRequester = cdMonitorRequester;
        }
        
        private Channel channel;
        private CDMonitorRequester cdMonitorRequester;
        private Executor executor = null;
        private CDQueue cdQueue = null;    
        private CallRequester callRequester = null;;    
        private ChannelMonitor channelMonitor = null;
        private ArrayList<ChannelField> channelFieldList = new ArrayList<ChannelField>();
        private ArrayList<MonitorField> monitorFieldList = new ArrayList<MonitorField>();
        private ChannelFieldGroup channelFieldGroup = null;;
        private ChannelField[] channelFields = null;
        private MonitorField[] monitorFields;
        private CD cd = null;
        private boolean monitorOccured = true;
        private boolean groupPutActive = false;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForAbsoluteChange(org.epics.ioc.ca.ChannelField, double)
         */
        public void lookForAbsoluteChange(ChannelField channelField, double value) {
            Field field = channelField.getField();
            if(field.getType()!=Type.scalar) {
                message("field is not a numeric scalar", MessageType.error);
                lookForPut(channelField,true);
                return;
            }
            Scalar scalar = (Scalar)field;
            if(!scalar.getScalarType().isNumeric()) {
                message("field is not a numeric scalar", MessageType.error);
                lookForPut(channelField,true);
                return;
            }
            MonitorField monitorField
                = new MonitorField(MonitorType.absoluteChange,scalar.getScalarType(),value);
            monitorField.initField(channelField.getPVField());
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForChange(org.epics.ioc.ca.ChannelField, boolean)
         */
        public void lookForChange(ChannelField channelField, boolean causeMonitor) {
            Field field = channelField.getField();
            Type type = field.getType();
            if(type!=Type.scalar && type!=Type.scalarArray) {
                message("field is not primitive", MessageType.error);
                lookForPut(channelField,causeMonitor);
                return;
            }
            Scalar scalar = (Scalar)field;
            MonitorField monitorField = new MonitorField(type,scalar.getScalarType(),causeMonitor);
            monitorField.initField(channelField.getPVField());
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForPercentageChange(org.epics.ioc.ca.ChannelField, double)
         */
        public void lookForPercentageChange(ChannelField channelField, double value) {
            Field field = channelField.getField();
            Type type = field.getType();
            if(type!=Type.scalar) {
                message("field is not a numeric scalar", MessageType.error);
                lookForPut(channelField,true);
                return;
            }
            Scalar scalar = (Scalar)field;
            ScalarType scalarType = scalar.getScalarType();
            if(!scalarType.isNumeric()){
                message("field is not a numeric scalar", MessageType.error);
                lookForPut(channelField,true);
                return;
            }
            MonitorField monitorField = new MonitorField(MonitorType.percentageChange,scalarType,value);
            monitorField.initField(channelField.getPVField());
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#lookForPut(org.epics.ioc.ca.ChannelField, boolean)
         */
        public void lookForPut(ChannelField channelField, boolean causeMonitor) {
            MonitorField monitorField = new MonitorField(causeMonitor);
            monitorField.initField(channelField.getPVField());
            monitorFieldList.add(monitorField);
            channelFieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDMonitor#start(int, org.epics.ioc.util.Executor)
         */
        public void start(int queueSize, Executor executor) {
            
            channelFieldGroup = channel.createFieldGroup(this);
            for(int i=0; i<channelFieldList.size(); i++) {
                channelFieldGroup.addChannelField(channelFieldList.get(i));
            }
            channelFields = channelFieldGroup.getArray();
            monitorFields = new MonitorField[channelFields.length];
            for(int i=0; i<monitorFields.length; i++) monitorFields[i] = monitorFieldList.get(i);
            cdQueue = CDFactory.createCDQueue(queueSize, channel, channelFieldGroup);
            this.executor = executor;
            callRequester = new CallRequester();
            cd = cdQueue.getFree(true);
            cd.clearNumPuts();
            channelMonitor = channel.createChannelMonitor(this);
            channelMonitor.setFieldGroup(channelFieldGroup);
            monitorOccured = false;
            PVRecord pvRecord = channel.getPVRecord();
            pvRecord.registerListener(this);
            for(ChannelField channelField : channelFields) {
                PVField pvField = channelField.getPVField();
                pvField.addListener(this);
            }
            channelMonitor.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.MonitorCDPut#stop()
         */
        public void stop() {
            if(channelMonitor!=null) channelMonitor.stop();
            channelFieldGroup = null;
            channelMonitor = null;
            PVRecord pvRecord = channel.getPVRecord();
            pvRecord.unregisterListener(this);
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
         * @see org.epics.ioc.ca.ChannelMonitorRequester#initialDataAvailable()
         */
        @Override
        public void initialDataAvailable() {
            for(int i=0; i<channelFields.length; i++) {
                ChannelField channelField = channelFields[i];
                cd.put(channelField,channelField.getPVField());
            }
            getNewCD();
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
            int index = -1;
            ChannelField channelField = null;
            for(int i=0; i<channelFields.length; i++) {
                channelField = channelFields[i];
                if(channelField.getPVField()==pvField) {
                    index = i; break;
                }
            }
            if(index==-1) throw new IllegalStateException("Logic error. Unexpected dataPut");
            boolean result = cd.put(channelField,pvField);
            if(!result) return;
            MonitorField monitorField = monitorFields[index];
            result = monitorField.newField(pvField);
            if(!result) return;
            if(groupPutActive) {
                monitorOccured = true;
                return;
            }
            getNewCD();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#dataPut(org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVField)
         */
        public void dataPut(PVStructure requested, PVField pvField) {
            int index = -1;
            ChannelField channelField = null;
            for(int i=0; i<channelFields.length; i++) {
                channelField = channelFields[i];
                if(channelField.getPVField()==requested) {
                    index = i; break;
                }
            }
            if(index==-1) throw new IllegalStateException("Logic error. Unexpected dataPut");
            boolean result = cd.putSubfield(channelField, pvField);
            if(!result) return;
            if(groupPutActive) {
                monitorOccured = true;
                return;
            }
            getNewCD();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#endGroupPut(org.epics.pvData.pv.PVRecord)
         */
        public void endGroupPut(PVRecord pvRecord) {
            if(monitorOccured) getNewCD();
            monitorOccured = false;
            groupPutActive = false;
        }
        
        private void getNewCD() {
            CD initial = cd;
            cd = cdQueue.getFree(true);
            List<ChannelField> initialList = initial.getChannelFieldGroup().getList();
            for(ChannelField channelField: initialList) {
                cd.put(channelField, channelField.getPVField());
            }
            cd.clearNumPuts();
            cdQueue.setInUse(initial);
            callRequester.call();
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.PVListener#unlisten(org.epics.pvData.pv.PVRecord)
         */
        public void unlisten(PVRecord pvRecord) {
            stop();
            channel.getChannelListener().destroy(channel);
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
                executor.execute(this);
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
            private ScalarType scalarType = null;
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
            private MonitorField(Type type,ScalarType scalarType,boolean causeMonitor) {
                this.monitorType = MonitorType.onChange;
                this.scalarType = scalarType;
                this.causeMonitor = causeMonitor;
            }
            private MonitorField(MonitorType monitorType, ScalarType scalarType, double deadband) {
                causeMonitor = true;
                this.monitorType = monitorType;
                this.scalarType = scalarType;
                this.deadband = deadband;
            }
            
            private void initField(PVField pvField) {
                if(monitorType==MonitorType.onPut) return;
                if(scalarType!=null) {
                    switch(scalarType) {
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
                lastMonitorValue = convert.toDouble((PVScalar)pvField);
            }
            
            private boolean newField(PVField pvField) {
                if(monitorType==MonitorType.onPut) {
                    if(causeMonitor) return true;
                    return false;
                }
                if(scalarType==null) return true;
                if(monitorType==MonitorType.onChange && scalarType!=null) {
                    switch(scalarType) {
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
                switch(scalarType) {
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
