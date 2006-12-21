/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca.test;

import junit.framework.TestCase;
import java.util.*;

import org.epics.ioc.ca.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * JUnit test for ChannelData.
 * @author mrk
 *
 */
public class ChannelMonitorTest extends TestCase {
    
    public static void testChannelData() {
        DBD dbd = DBDFactory.getMasterDBD();
        Requestor iocRequestor = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "dbd/menuStructureSupport.xml",
                 iocRequestor);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/ca/test/exampleDBD.xml",
                 iocRequestor);        
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/ca/test/exampleDB.xml",iocRequestor);
        if(!initOK) return;
        Monitor monitor;
        monitor =new Monitor("          onChange","counter");
        boolean result = monitor.connect(2);
        if(result) monitor.lookForChange();
        monitor =new Monitor("  onAbsoluteChange","counter");
        result = monitor.connect(2);
        if(result) monitor.lookForAbsoluteChange(1.1);
        monitor =new Monitor("onPercentageChange","counter");
        result = monitor.connect(2);
        if(result) monitor.lookForPercentageChange(10.0);
        
        MonitorNotify monitorNotify;
        monitorNotify =new MonitorNotify("          onChange","counter");
        result = monitorNotify.connect(2);
        if(result) monitorNotify.lookForChange();
        monitorNotify =new MonitorNotify("  onAbsoluteChange","counter");
        result = monitorNotify.connect(2);
        if(result) monitorNotify.lookForAbsoluteChange(1.1);
        monitorNotify =new MonitorNotify("onPercentageChange","counter");
        result = monitorNotify.connect(2);
        if(result) monitorNotify.lookForPercentageChange(10.0);
        
        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
    
    private static class Monitor implements
    ChannelMonitorRequestor,ChannelStateListener {
        private String requestorName;
        private String pvName;
        private Channel channel;
        private ValueData valueData;
        private ChannelFieldGroup fieldGroup;
        private ChannelMonitor channelMonitor;
        
        public Monitor(String requestorName, String pvName) {
            super();
            this.requestorName = requestorName;
            this.pvName = pvName;
            channel = ChannelFactory.createChannel(pvName, this, false);
        }
        
        private boolean connect(int queueSize) {
            if(channel==null) return false;
            valueData = new ValueData(channel);
            fieldGroup = valueData.init();
            if(fieldGroup==null) return false;
            channelMonitor = channel.createChannelMonitor(false);
            return true;   
        }
        
        private void lookForChange() {
            valueData.lookForChange(channelMonitor);
            channelMonitor.start(this, 2, null, ScanPriority.low);
        }
        
        private void lookForAbsoluteChange(double value) {
            valueData.lookForAbsoluteChange(channelMonitor,value);
            channelMonitor.start(this, 2, null, ScanPriority.low);
        }
        
        private void lookForPercentageChange(double value) {
            valueData.lookForPercentageChange(channelMonitor,value);
            channelMonitor.start(this, 2, null, ScanPriority.low);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequestor#dataOverrun(int)
         */
        public void dataOverrun(int number) {
            System.out.printf("%s %s overrun %d%n",requestorName,pvName,number);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequestor#monitorData(java.util.List, java.util.List)
         */
        public void monitorData(ChannelData channelData) {
            List<PVData> pvDataList = channelData.getPVDataList();;
            for(int i=0; i < pvDataList.size(); i++) {
                PVData pvData = pvDataList.get(i);
                channelData.add(pvData);
            }
            System.out.printf("%s %s %s%n",
                requestorName,pvName,valueData.printResults(channelData));
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return requestorName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private static class MonitorNotify implements
    ChannelMonitorNotifyRequestor,ChannelStateListener {
        private String requestorName;
        private String pvName;
        private Channel channel;
        private ValueData valueData;
        private ChannelFieldGroup fieldGroup;
        private ChannelMonitor channelMonitor;
        
        public MonitorNotify(String requestorName, String pvName) {
            super();
            this.requestorName = requestorName;
            this.pvName = pvName;
            channel = ChannelFactory.createChannel(pvName, this, false);
        }
        
        private boolean connect(int queueSize) {
            if(channel==null) return false;
            valueData = new ValueData(channel);
            fieldGroup = valueData.init();
            if(fieldGroup==null) return false;
            channelMonitor = channel.createChannelMonitor(false);
            return true;   
        }
        
        private void lookForChange() {
            valueData.lookForChange(channelMonitor);
            channelMonitor.start(this, null, ScanPriority.low);
        }
        
        private void lookForAbsoluteChange(double value) {
            valueData.lookForAbsoluteChange(channelMonitor,value);
            channelMonitor.start(this, null, ScanPriority.low);
        }
        
        private void lookForPercentageChange(double value) {
            valueData.lookForPercentageChange(channelMonitor,value);
            channelMonitor.start(this, null, ScanPriority.low);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorNotifyRequestor#monitorEvent()
         */
        public void monitorEvent() {
            System.out.printf("%s %s monitorEvent%n",requestorName,pvName);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private static class ValueData implements ChannelFieldGroupListener{
        private Channel channel;
        private ChannelFieldGroup channelFieldGroup;
        private ChannelField valueField;
        private ChannelField statusField;
        private ChannelField severityField;
        private ChannelField timeStampField;
        
        private ValueData(Channel channel) {
            this.channel = channel;
        }
        
        private void lookForOther(ChannelMonitor channelMonitor) {
            channelMonitor.lookForChange(statusField, true);
            channelMonitor.lookForChange(severityField, true);
            channelMonitor.lookForChange(timeStampField, false);
        }
        
        private void lookForChange(ChannelMonitor channelMonitor) {
            channelMonitor.lookForChange(valueField, true);
            lookForOther(channelMonitor);
        }
        private void lookForAbsoluteChange(ChannelMonitor channelMonitor,double deadband) {
            channelMonitor.lookForAbsoluteChange(valueField, deadband);
            lookForOther(channelMonitor);
        }
        private void lookForPercentageChange(ChannelMonitor channelMonitor,double deadband) {
            channelMonitor.lookForPercentageChange(valueField, deadband);
            lookForOther(channelMonitor);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // TODO Auto-generated method stub
            
        }
        private ChannelFieldGroup init() {
            channelFieldGroup = channel.createFieldGroup(this);
            ChannelSetFieldResult result;
            result = channel.setField("value");
            if(result!=ChannelSetFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            valueField = channel.getChannelField();
            channelFieldGroup.addChannelField(valueField);
            result = channel.setField("status");
            if(result!=ChannelSetFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            statusField = channel.getChannelField();
            channelFieldGroup.addChannelField(statusField);
            result = channel.setField("severity");
            if(result!=ChannelSetFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            severityField = channel.getChannelField();
            channelFieldGroup.addChannelField(severityField);
            result = channel.setField("timeStamp");
            if(result!=ChannelSetFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            timeStampField = channel.getChannelField();
            channelFieldGroup.addChannelField(timeStampField);
            return channelFieldGroup;
        }

        private String printResults(ChannelData channelData) {
            StringBuilder builder = new StringBuilder();
            List<PVData> pvDataList = channelData.getPVDataList();
            List<ChannelField> channelFieldList = channelData.getChannelFieldList();
            Iterator<PVData> pvDataIter = pvDataList.iterator();
            Iterator<ChannelField> channelFieldIter = channelFieldList.iterator();
            while(pvDataIter.hasNext()) {
                PVData data = pvDataIter.next();
                ChannelField field = channelFieldIter.next();
                if(field==valueField) {
                    PVDouble pvDouble = (PVDouble)data;
                    builder.append(String.format("value %f", pvDouble.get()));
                } else if (field==severityField) {
                    PVEnum pvEnum = (PVEnum)data;
                    int index = pvEnum.getIndex();
                    builder.append(String.format(
                        " severity %s",AlarmSeverity.getSeverity(index).toString()));
                } else if(field==statusField) {
                    PVString pvString = (PVString)data;
                    String value = pvString.get();
                    builder.append(" status " + value);
                } else if(field==timeStampField) {
                    PVTimeStamp pvTimeStamp = PVTimeStamp.create(data);
                    TimeStamp timeStamp = new TimeStamp();
                    pvTimeStamp.get(timeStamp);
                    long seconds = timeStamp.secondsPastEpoch;
                    int nano = timeStamp.nanoSeconds;
                    long now = nano/1000000 + seconds*1000;
                    Date date = new Date(now);
                    builder.append(String.format(" time %s",date.toLocaleString()));
                }
            }
            return builder.toString();
        }
    }
    
    private static class Listener implements Requestor {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "LocalChannelAccessTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    } 
}
