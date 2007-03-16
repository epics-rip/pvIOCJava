/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca.test;

import junit.framework.TestCase;
import java.util.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.ca.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
import org.epics.ioc.db.*;
import org.epics.ioc.dbd.*;

/**
 * JUnit test for RecordProcess.
 * @author mrk
 *
 */
public class PowerSupplyTest extends TestCase {
    /**
     * test PVAccess.
     */
    public static void testPowerSupply() {
        DBD dbd = DBDFactory.getMasterDBD();
        Requestor iocRequestor = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/ca/test/powerSupplyDBD.xml",
                 iocRequestor);        
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/ca/test/powerSupplyDB.xml",iocRequestor);
        if(!initOK) return;
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        String[] list = null;
        list = iocdbMaster.recordList(".*");
//        System.out.print("masterIOCDB records: "); printList(list);
//        for(int i=0; i<list.length; i++) {
//            DBRecord dbRecord = iocdbMaster.findRecord(list[i]);
//            System.out.println(dbRecord.getPVRecord().toString());
//        }
        Put powerSupplyPowerPut = new Put("examplePowerSupply",true);
        Monitor powerSupplyPowerMonitor = 
            new Monitor("monitor","examplePowerSupply","powerSupply.power");
        boolean result = powerSupplyPowerMonitor.connect(3);
        if(result) powerSupplyPowerMonitor.lookForChange();
        Get powerSupplyPowerGet = new Get("examplePowerSupply",false);
        Get powerSupplyCurrentGet = new Get("examplePowerSupply",false);
        if(!powerSupplyPowerPut.connect("powerSupply.power")) return;
        if(!powerSupplyPowerGet.connect("powerSupply.power")) return;
        if(!powerSupplyCurrentGet.connect("powerSupply.current")) return;
        powerSupplyPowerPut.put(1.0);
        powerSupplyPowerGet.get();
        powerSupplyCurrentGet.get();
        
        Put powerSupplyArrayPowerPut = new Put("examplePowerSupplyArray",true);
        Monitor powerSupplyArrayPowerMonitor = 
            new Monitor("monitor","examplePowerSupplyArray","powerSupply[1].power");
        result = powerSupplyArrayPowerMonitor.connect(3);
        if(result) powerSupplyArrayPowerMonitor.lookForChange();
        Get powerSupplyArrayPowerGet = new Get("examplePowerSupplyArray",false);
        Get powerSupplyArrayCurrentGet = new Get("examplePowerSupplyArray",false);
        if(!powerSupplyArrayPowerPut.connect("powerSupply[1].power")) return;
        if(!powerSupplyArrayPowerGet.connect("powerSupply[1].power")) return;
        if(!powerSupplyArrayCurrentGet.connect("powerSupply[1].current")) return;
        powerSupplyArrayPowerPut.put(1.0);
        powerSupplyArrayPowerGet.get();
        powerSupplyArrayCurrentGet.get();
        Get powerSupplyArrayGet = new Get("examplePowerSupplyArray",false);
        if(!powerSupplyArrayGet.connect("powerSupply")) return;
        powerSupplyArrayGet.get();
        
        for(int i=1; i<8; i++) {
            delay(1);
            powerSupplyPowerPut.put(i*2.0);
            delay(1);
            powerSupplyArrayPowerPut.put(i*10.0);
        }
        
//        for(int i=0; i<list.length; i++) {
//            DBRecord dbRecord = iocdbMaster.findRecord(list[i]);
//            System.out.println(dbRecord.getPVRecord().toString());
//        }
        
        Map<String,DBRecord> recordMap = iocdbMaster.getRecordMap();
        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            RecordProcess recordProcess = recordMap.get(key).getRecordProcess();
//            recordProcess.setTrace(true);
//        }
        
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
    }
    
    private static void printList(String[] list) {
        for(int i=0; i<list.length; i++) {
            if((i+1)%5 == 0) {
                System.out.println();
                System.out.print("    ");
            } else {
                System.out.print(" ");
            }
            System.out.print(list[i]);
        }
        System.out.println();
    }
    
    private static void delay(long milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
        }   
    }
    
    private static class Put implements
    ChannelPutRequestor,
    ChannelStateListener, ChannelFieldGroupListener
    {
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private String pvname = null;
        private boolean process;
        private Channel channel;
        private ChannelPut channelPut;
        private ChannelFieldGroup putFieldGroup;
        private ChannelField valueField;
        private double value;
        
        private Put(String pvname, boolean process) {
            this.pvname = pvname;
            this.process = process;
            channel = ChannelFactory.createChannel(pvname, this, false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#nextDelayedPutData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedPutField(PVField pvField) {
            // TODO Auto-generated method stub
            return false;
        }
        private void destroy() {
            channel.destroy();
        }
        private boolean connect(String fieldName) {
            putFieldGroup = channel.createFieldGroup(this);
            ChannelFindFieldResult result;
            result = channel.findField(fieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return false;
            }
            valueField = channel.getChannelField();
            putFieldGroup.addChannelField(valueField);
            channelPut = channel.createChannelPut(putFieldGroup, this, process);
            return true;   
        }
        
        private void put(double value) {
            this.value = value;
            allDone = false;
            channelPut.put();
            lock.lock();
            try {
                if(!allDone) {                       
                    waitDone.await();
                }
            } catch (InterruptedException ie) {
                return;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "Put:" + pvname;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.printf("put.massage %s%n", message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#nextPutData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextPutField(ChannelField channelField, PVField pvField) {
            PVDouble pvDouble = (PVDouble)pvField;
            pvDouble.put(value);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelRequestor#requestDone(org.epics.ioc.ca.Channel, org.epics.ioc.util.RequestResult)
         */
        public void putDone(RequestResult requestResult) {
            lock.lock();
            try {
                allDone = true;
                    waitDone.signal();
            } finally {
                lock.unlock();
            }
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
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // TODO Auto-generated method stub
            
        }
    }
    
    private static class Get implements
    ChannelGetRequestor,
    ChannelStateListener, ChannelFieldGroupListener
    {
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private String pvname = null;
        private boolean process;
        private Channel channel;
        private ChannelGet channelGet;
        private ValueData valueData;
        private ChannelFieldGroup getFieldGroup;
        
        private Get(String pvname,boolean process) {
            this.pvname = pvname;
            this.process = process;
            channel = ChannelFactory.createChannel(pvname, this, false);            
        }
        private void destroy() {
            channel.destroy();
        }
        private boolean connect(String fieldName) {
            
            valueData = new ValueData(channel);
            getFieldGroup = valueData.init(fieldName);
            if(getFieldGroup==null) return false;
            channelGet = channel.createChannelGet(getFieldGroup, this, process);
            return true;   
        }
        
        private void get() {
            allDone = false;
            valueData.clear();
            channelGet.get();
            lock.lock();
            try {
                if(!allDone) {                       
                    waitDone.await();
                }
            } catch (InterruptedException ie) {
                return;
            } finally {
                lock.unlock();
            }
            valueData.printResults();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetField(PVField pvField) {
            // TODO Auto-generated method stub
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "PutGet:" + pvname;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.printf("putGet.massage %s%n", message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextGetData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetField(ChannelField channelField, PVField pvField) {
            valueData.nextGetField(channel, channelField, pvField);
            return false;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#getDone(org.epics.ioc.util.RequestResult)
         */
        public void getDone(RequestResult requestResult) {
            lock.lock();
            try {
                allDone = true;
                    waitDone.signal();
            } finally {
                lock.unlock();
            }
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
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // TODO Auto-generated method stub
            
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
            valueData = new ValueData(channel);
            fieldGroup = valueData.init(fieldName);
            if(fieldGroup==null) return false;
            channelMonitor = channel.createChannelMonitor(false,true);
            return true;   
        }
        
        private void lookForChange() {
            valueData.lookForChange(channelMonitor);
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
    
    private static class ValueData implements ChannelFieldGroupListener{
        private Channel channel;
        private ChannelData channelData;
        private ChannelFieldGroup channelFieldGroup;
        private String fieldName;
        private ChannelField valueField;
        private ChannelField statusField;
        private ChannelField severityField;
        private ChannelField timeStampField;
        
        private ValueData(Channel channel) {
            this.channel = channel;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // TODO Auto-generated method stub
            
        }
        private ChannelFieldGroup init(String fieldName) {
            this.fieldName = fieldName;
            channelFieldGroup = channel.createFieldGroup(this);
            ChannelFindFieldResult result;
            channel.findField(null);
            result = channel.findField(fieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            valueField = channel.getChannelField();
            channelFieldGroup.addChannelField(valueField);
            channel.findField(null);
            result = channel.findField(fieldName + ".status");
            if(result==ChannelFindFieldResult.thisChannel) {  
                statusField = channel.getChannelField();
                channelFieldGroup.addChannelField(statusField);
                channel.findField(null);
            }
            result = channel.findField(fieldName + ".severity");
            if(result==ChannelFindFieldResult.thisChannel) {
                severityField = channel.getChannelField();
                channelFieldGroup.addChannelField(severityField);
                channel.findField(null);
            }
            result = channel.findField(fieldName + ".timeStamp");
            if(result==ChannelFindFieldResult.thisChannel) {
                timeStampField = channel.getChannelField();
                channelFieldGroup.addChannelField(timeStampField);
                channel.findField(null);
            }
            channelData = ChannelDataFactory.createChannelData(channel,channelFieldGroup,true);
            if(channelData==null) {
                System.out.printf("ChannelDataFactory.createData failed");
                return null;
            }
            return channelFieldGroup;
        }
        
        private void lookForChange(ChannelMonitor channelMonitor) {
            channelMonitor.lookForChange(valueField, true);
            if(statusField!=null) channelMonitor.lookForChange(statusField, true);
            if(severityField!=null) channelMonitor.lookForChange(severityField, true);
            if(timeStampField!=null) channelMonitor.lookForChange(timeStampField, false);
        }
        
        private void clear() {
            channelData.clearNumPuts();
        }
        private boolean nextGetField(Channel channel, ChannelField channelField, PVField pvField) {
            channelData.dataPut(pvField);
            return false;
        }
        
        private void printResults() {
            ChannelFieldGroup channelFieldGroup = channelData.getChannelFieldGroup();
            List<ChannelField> channelFieldList = channelFieldGroup.getList();
            CDStructure cdStructure = channelData.getCDRecord().getCDStructure();
            CDField[] cdbDatas = cdStructure.getFieldCDFields();
            System.out.println(channel.getChannelName() + "." + fieldName);
            for(int i=0;i<cdbDatas.length; i++) {
                CDField cdField = cdbDatas[i];
                PVField pvField = cdField.getPVField();
                ChannelField channelField = channelFieldList.get(i);
                if(channelField==valueField) {
                    System.out.printf("value %s numPuts %d", pvField.toString(),cdField.getMaxNumPuts());
                } else if (channelField==severityField) {
                    PVEnum pvEnum = (PVEnum)pvField;
                    int index = pvEnum.getIndex();
                    System.out.printf(" severity %s numPuts %d",
                        AlarmSeverity.getSeverity(index).toString(),cdField.getMaxNumPuts());
                } else if(channelField==statusField) {
                    PVString pvString = (PVString)pvField;
                    String value = pvString.get();
                    System.out.printf(" status %s numPuts %d",value,cdField.getMaxNumPuts());
                } else if(channelField==timeStampField) {
                    PVTimeStamp pvTimeStamp = PVTimeStamp.create(pvField);
                    TimeStamp timeStamp = new TimeStamp();
                    pvTimeStamp.get(timeStamp);
                    long seconds = timeStamp.secondsPastEpoch;
                    int nano = timeStamp.nanoSeconds;
                    long now = nano/1000000 + seconds*1000;
                    Date date = new Date(now);
                    System.out.printf(" time %s numPuts %d%n",date.toLocaleString(),cdField.getMaxNumPuts());
                } else {
                    System.out.printf("%npvField not found %s%n",pvField.toString());
                }
                if(cdField.getNumSupportNamePuts()>0) {
                    System.out.printf("supportName %s numSupportNamePuts %d%n",
                        pvField.getSupportName(),cdField.getNumSupportNamePuts());
                }
            }
            System.out.println();
            cdStructure.clearNumPuts();
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
}
