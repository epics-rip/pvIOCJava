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
 * JUnit test for CDField.
 * @author mrk
 *
 */
public class ChannelMonitorTest extends TestCase {
    
    private static void delay(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
        }   
    }
    
    public static void testChannelData() {
        DBD dbd = DBDFactory.getMasterDBD();
        Requestor iocRequestor = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "dbd/menuStructureSupport.xml",
                 iocRequestor);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/process/test/exampleDBD.xml",
                 iocRequestor);        
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/process/test/monitorDB.xml",iocRequestor);
        if(!initOK) return;
        Monitor monitor;
        monitor =new Monitor("onChange","counter");
        boolean result = monitor.connect(3);
        if(result) monitor.lookForChange();
        monitor =new Monitor("onAbsoluteChange","counter");
        result = monitor.connect(3);
        if(result) monitor.lookForAbsoluteChange(1.1);
        monitor =new Monitor("onPercentageChange","counter");
        result = monitor.connect(3);
        if(result) monitor.lookForPercentageChange(10.0);
        monitor =new Monitor("link","monitorChange","input");
        result = monitor.connect(3);
        if(result) monitor.lookForChange();
        monitor =new Monitor("severity","monitorChange","severity");
        result = monitor.connect(3);
        if(result) monitor.lookForChange();
        monitor =new Monitor("monitorChange","monitorChange","value");
        result = monitor.connect(3);
        if(result) monitor.lookForChange();
        
        MonitorNotify monitorNotify;
        monitorNotify =new MonitorNotify("onChange","counter");
        result = monitorNotify.connect(3);
        if(result) monitorNotify.lookForChange();
        monitorNotify =new MonitorNotify("onAbsoluteChange","counter");
        result = monitorNotify.connect(3);
        if(result) monitorNotify.lookForAbsoluteChange(1.1);
        monitorNotify =new MonitorNotify("onPercentageChange","counter");
        result = monitorNotify.connect(3);
        if(result) monitorNotify.lookForPercentageChange(10.0);
        
        while(true) {
            delay(1000);
        }
    }
    
    private static class Monitor implements
    ChannelMonitorRequestor,ChannelStateListener {
        private String requestorName;
        private String pvName;
        private String fieldName = null;
        private Channel channel;
        private ValueData valueData;
        private ChannelFieldGroup fieldGroup;
        private ChannelMonitor channelMonitor;
        private int queueSize;
        
        public Monitor(String requestorName, String pvName, String fieldName) {
            super();
            this.requestorName = requestorName;
            this.pvName = pvName;
            this.fieldName = fieldName;
            channel = ChannelFactory.createChannel(pvName, this, false);
            if(channel==null) {
                System.out.println(pvName + " not found");
            }
        }
        
        public Monitor(String requestorName, String pvName) {
            super();
            this.requestorName = requestorName;
            this.pvName = pvName;
            channel = ChannelFactory.createChannel(pvName, this, false);
            if(channel==null) {
                System.out.println(pvName + " not found");
            }
        }
        
        private boolean connect(int queueSize) {
            if(channel==null) return false;
            this.queueSize = queueSize;
            valueData = new ValueData(channel,fieldName);
            fieldGroup = valueData.init();
            if(fieldGroup==null) return false;
            channelMonitor = channel.createChannelMonitor(false);
            return true;   
        }
        
        private void lookForChange() {
            valueData.lookForChange(channelMonitor);
            channelMonitor.start(this, queueSize, null, ScanPriority.low);
        }
        
        private void lookForAbsoluteChange(double value) {
            valueData.lookForAbsoluteChange(channelMonitor,value);
            channelMonitor.start(this, queueSize, null, ScanPriority.low);
        }
        
        private void lookForPercentageChange(double value) {
            valueData.lookForPercentageChange(channelMonitor,value);
            channelMonitor.start(this, queueSize, null, ScanPriority.low);
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
        private String fieldName = null;
        private Channel channel;
        private ValueData valueData;
        private ChannelFieldGroup fieldGroup;
        private ChannelMonitor channelMonitor;
        
        public MonitorNotify(String requestorName, String pvName,String fieldName) {
            super();
            this.requestorName = requestorName;
            this.pvName = pvName;
            this.fieldName = fieldName;
            channel = ChannelFactory.createChannel(pvName, this, false);
        }
        
        public MonitorNotify(String requestorName, String pvName) {
            super();
            this.requestorName = requestorName;
            this.pvName = pvName;
            channel = ChannelFactory.createChannel(pvName, this, false);
        }
        
        private boolean connect(int queueSize) {
            if(channel==null) return false;
            valueData = new ValueData(channel,fieldName);
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
            return requestorName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.printf("%s %s %s",messageType.toString(),requestorName,message);
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
        private String fieldName;
        private ChannelFieldGroup channelFieldGroup;
        private ChannelField valueField;
        private ChannelField statusField = null;
        private ChannelField severityField = null;
        private ChannelField timeStampField = null;
        
        private ValueData(Channel channel,String fieldName) {
            this.channel = channel;
            if(fieldName==null) fieldName = "value";
            this.fieldName = fieldName;
        }
        
        private void lookForOther(ChannelMonitor channelMonitor) {
            if(statusField!=null) channelMonitor.lookForChange(statusField, true);
            if(severityField!=null) channelMonitor.lookForChange(severityField, true);
            if(timeStampField!=null) channelMonitor.lookForChange(timeStampField, false);
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
            ChannelFindFieldResult result;
            result = channel.findField(fieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("findField returned %s%n", result.toString());
                return null;
            }
            valueField = channel.getChannelField();
            channelFieldGroup.addChannelField(valueField);
            String newFieldName = fieldName + ".status";
            channel.findField(null);
            result = channel.findField(newFieldName);
            if(result==ChannelFindFieldResult.thisChannel) {
                statusField = channel.getChannelField();
                channelFieldGroup.addChannelField(statusField);
            }
            newFieldName = fieldName + ".severity";
            channel.findField(null);
            result = channel.findField(newFieldName);
            if(result==ChannelFindFieldResult.thisChannel) {
                severityField = channel.getChannelField();
                channelFieldGroup.addChannelField(severityField);
            }
            newFieldName = fieldName + ".timeStamp";
            channel.findField(null);
            result = channel.findField(newFieldName);
            if(result==ChannelFindFieldResult.thisChannel) {
                timeStampField = channel.getChannelField();
                channelFieldGroup.addChannelField(timeStampField);
            }
            return channelFieldGroup;
        }

        private String printResults(ChannelData channelData) {
            StringBuilder builder = new StringBuilder();
            ChannelFieldGroup channelFieldGroup = channelData.getChannelFieldGroup();
            List<ChannelField> channelFieldList = channelFieldGroup.getList();
            CDStructure cdStructure = channelData.getCDRecord().getCDStructure();
            CDField[] cdbDatas = cdStructure.getFieldCDFields();
            int maxNumPuts = channelData.getMaxPutsToField();
            if(maxNumPuts!=1) {
                builder.append(String.format(
                    " maxNumPuts %d ",maxNumPuts));
            }
            for(int i=0;i<cdbDatas.length; i++) {
                CDField cdField = cdbDatas[i];
                maxNumPuts = cdField.getMaxNumPuts();
                if(maxNumPuts<=0) continue;
                if(maxNumPuts!=1) {
                    builder.append(String.format(
                        " maxNumPuts %d ",maxNumPuts));
                }
                PVField pvField = cdField.getPVField();
                ChannelField channelField = channelFieldList.get(i);
                if(channelField==valueField) {
                    builder.append(String.format(
                        "value %s ",
                        pvField.toString(2)));
                } else if (channelField==severityField) {
                    PVEnum pvEnum = (PVEnum)pvField;
                    int index = pvEnum.getIndex();
                    builder.append(String.format(
                        " severity %s",
                        AlarmSeverity.getSeverity(index).toString()));
                } else if(channelField==statusField) {
                    PVString pvString = (PVString)pvField;
                    String value = pvString.get();
                    builder.append(String.format(" status %s",
                        value));
                } else if(channelField==timeStampField) {                 
                    PVTimeStamp pvTimeStamp = PVTimeStamp.create(pvField);
                    TimeStamp timeStamp = new TimeStamp();
                    pvTimeStamp.get(timeStamp);
                    long seconds = timeStamp.secondsPastEpoch;
                    int nano = timeStamp.nanoSeconds;
                    long now = nano/1000000 + seconds*1000;
                    Date date = new Date(now);
                    builder.append(String.format(" time %s",
                        date.toLocaleString()));
                }
                if(cdField.getNumSupportNamePuts()>0) {
                    builder.append(String.format("supportName %s numSupportNamePuts %d%n",
                        pvField.getSupportName(),cdField.getNumSupportNamePuts()));
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
