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
public class LocalChannelAccessTest extends TestCase {
    /**
     * test PVAccess.
     */
    public static void testLocalChannelAccess() {
        DBD dbd = DBDFactory.getMasterDBD();
        Requestor iocRequestor = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "dbd/menuStructureSupport.xml",
                 iocRequestor);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/process/test/exampleDBD.xml",
                 iocRequestor);        
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/process/test/localLinkDB.xml",iocRequestor);
        if(!initOK) return;
        PutGet counter = new PutGet("counter",true);
        Get getDouble01 = new Get("double01",false);
        Get getDouble02 = new Get("double02",false);
        if(!counter.connect()) return;
        if(!getDouble01.connect()) return;
        if(!getDouble02.connect()) return;
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        Map<String,DBRecord> recordMap = iocdbMaster.getRecordMap();
        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            RecordProcess recordProcess = recordMap.get(key).getRecordProcess();
//            recordProcess.setTrace(true);
//        }
        for(int i=0; i<10; i+= 5) {
            System.out.printf("%nputGet ");
            counter.putGet(i);
            System.out.printf("get double01 ");
            getDouble01.get();
            System.out.printf("get double02 ");
            getDouble02.get();
        }
        counter.destroy();
        Process counterProcess = new Process("counter");
        System.out.printf("%nprocess%n");
        counterProcess.process();
        System.out.printf("get double01 ");
        getDouble01.get();
        System.out.printf("get double02 ");
        getDouble02.get();
        Put counterPut;
        // this should fail
        try {
            counterPut = new Put("counter",true);
        } catch (IllegalStateException e) {
            System.out.println(e.getLocalizedMessage());
        }
        counterProcess.destroy();
        counterPut = new Put("counter",true);
        if(!counterPut.connect()) return;
        System.out.printf("%nput%n");
        counterPut.put(1000.0);
        System.out.printf("get double01 ");
        getDouble01.get();
        System.out.printf("get double02 ");
        getDouble02.get();
        counterPut.destroy();
        counterPut = new Put("counter",false);        
        if(!counterPut.connect()) return;
        System.out.printf("%nput%n");
        counterPut.put(2000.0);
        System.out.printf("get double01 ");
        getDouble01.get();
        System.out.printf("get double02 ");
        getDouble01.get();
        Get getDouble03 = new Get("double03",false);
        if(!getDouble03.connect()) return;
        System.out.printf("%nget double03 ");
        getDouble03.get();
        getDouble03.destroy();
        getDouble03 = new Get("double03",true);
        if(!getDouble03.connect()) return;
        System.out.printf("%nget double03 ");
        getDouble03.get();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
    }
    
    private static class Process implements
    ChannelProcessRequestor,ChannelStateListener
    {
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private String pvname = null;
        private Channel channel;
        private ChannelProcess channelProcess;
        
        private Process(String pvname) {
            this.pvname = pvname;
            channel = ChannelFactory.createChannel(pvname, this, false);            
            channelProcess = channel.createChannelProcess(this);
        }
        private void destroy() {
            channel.destroy();
        }
        private void process() {
            allDone = false;
            channelProcess.process();
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
            System.out.printf("putGet.massage %s%n", message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProcessRequestor#processDone(org.epics.ioc.util.RequestResult)
         */
        public void processDone(RequestResult requestResult) {
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
    
    private static class PutGet implements
    ChannelPutGetRequestor,
    ChannelStateListener, ChannelFieldGroupListener
    {
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private String pvname = null;
        private Channel channel;
        private ChannelPutGet channelPutGet;
        private ChannelFieldGroup putFieldGroup;
        private ValueData valueData;
        private ChannelFieldGroup getFieldGroup;
        private ChannelField valueField;
        private double value;
        
        private PutGet(String pvname,boolean process) {
            this.pvname = pvname;
            channel = ChannelFactory.createChannel(pvname, this, false);           
            channelPutGet = channel.createChannelPutGet(this, process);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#nextDelayedPutData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedPutData(PVData data) {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // TODO Auto-generated method stub
            return false;
        }

        private boolean connect() {
            putFieldGroup = channel.createFieldGroup(this);
            ChannelFindFieldResult result;
            result = channel.findField("value");
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return false;
            }
            valueField = channel.getChannelField();
            putFieldGroup.addChannelField(valueField);
            valueData = new ValueData(channel);
            getFieldGroup = valueData.init();
            if(getFieldGroup==null) return false;
            return true;   
        }
        
        private void destroy() {
            channel.destroy();
        }
        
        private void putGet(double value) {
            this.value = value;
            allDone = false;
            valueData.clear();
            channelPutGet.putGet(putFieldGroup, getFieldGroup);
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
        public boolean nextGetData(ChannelField field, PVData data) {
            valueData.nextGetData(channel, field, data);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#nextPutData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextPutData(ChannelField field, PVData data) {
            PVDouble pvDouble = (PVDouble)data;
            pvDouble.put(value);
            return false;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#putDone(org.epics.ioc.util.RequestResult)
         */
        public void putDone(RequestResult requestResult) {
            // nothing to do
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
    
    private static class Put implements
    ChannelPutRequestor,
    ChannelStateListener, ChannelFieldGroupListener
    {
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private String pvname = null;
        private Channel channel;
        private ChannelPut channelPut;
        private ChannelFieldGroup putFieldGroup;
        private ChannelField valueField;
        private double value;
        
        private Put(String pvname, boolean process) {
            this.pvname = pvname;
            channel = ChannelFactory.createChannel(pvname, this, false);
            channelPut = channel.createChannelPut(this, process);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#nextDelayedPutData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedPutData(PVData data) {
            // TODO Auto-generated method stub
            return false;
        }
        private void destroy() {
            channel.destroy();
        }
        private boolean connect() {
            putFieldGroup = channel.createFieldGroup(this);
            ChannelFindFieldResult result;
            result = channel.findField("value");
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return false;
            }
            valueField = channel.getChannelField();
            putFieldGroup.addChannelField(valueField);
            return true;   
        }
        
        private void put(double value) {
            this.value = value;
            allDone = false;
            channelPut.put(putFieldGroup);
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
        public boolean nextPutData(ChannelField field, PVData data) {
            PVDouble pvDouble = (PVDouble)data;
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
        private Channel channel;
        private ChannelGet channelGet;
        private ValueData valueData;
        private ChannelFieldGroup getFieldGroup;
        
        private Get(String pvname,boolean process) {
            this.pvname = pvname;
            channel = ChannelFactory.createChannel(pvname, this, false);            
            channelGet = channel.createChannelGet(this, process);
        }
        private void destroy() {
            channel.destroy();
        }
        private boolean connect() {
            
            valueData = new ValueData(channel);
            getFieldGroup = valueData.init();
            if(getFieldGroup==null) return false;
            return true;   
        }
        
        private void get() {
            allDone = false;
            valueData.clear();
            channelGet.get(getFieldGroup);
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
        public boolean nextDelayedGetData(PVData data) {
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
        public boolean nextGetData(ChannelField field, PVData data) {
            valueData.nextGetData(channel, field, data);
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
    
    private static class ValueData implements ChannelFieldGroupListener{
        private Channel channel;
        private ChannelData channelData;
        private ChannelFieldGroup channelFieldGroup;
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
        private ChannelFieldGroup init() {
            channelFieldGroup = channel.createFieldGroup(this);
            ChannelFindFieldResult result;
            channel.findField(null);
            result = channel.findField("value");
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            valueField = channel.getChannelField();
            channelFieldGroup.addChannelField(valueField);
            channel.findField(null);
            result = channel.findField("status");
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            statusField = channel.getChannelField();
            channelFieldGroup.addChannelField(statusField);
            channel.findField(null);
            result = channel.findField("severity");
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            severityField = channel.getChannelField();
            channelFieldGroup.addChannelField(severityField);
            channel.findField(null);
            result = channel.findField("timeStamp");
            if(result!=ChannelFindFieldResult.thisChannel) {
                System.out.printf("PutGet:set returned %s%n", result.toString());
                return null;
            }
            timeStampField = channel.getChannelField();
            channelFieldGroup.addChannelField(timeStampField);
            channelData = ChannelDataFactory.createData(channel,channelFieldGroup);
            if(channelData==null) {
                System.out.printf("ChannelDataFactory.createData failed");
                return null;
            }
            return channelFieldGroup;
        }
        
        private void clear() {
            channelData.clear();
        }
        private boolean nextGetData(Channel channel, ChannelField field, PVData data) {
            channelData.dataPut(data);
            return false;
        }
        
        private void printResults() {
            List<ChannelDataPV> channelDataPVList = channelData.getChannelDataPVList();
            Iterator<ChannelDataPV> iter = channelDataPVList.iterator();
            while(iter.hasNext()) {
                ChannelDataPV channelDataPV = iter.next();
                PVData data = channelDataPV.getPVData();
                ChannelField field = channelDataPV.getChannelField();
                if(field==valueField) {
                    PVDouble pvDouble = (PVDouble)data;
                    System.out.printf("value %f", pvDouble.get());
                } else if (field==severityField) {
                    PVEnum pvEnum = (PVEnum)data;
                    int index = pvEnum.getIndex();
                    System.out.printf(" severity %s",AlarmSeverity.getSeverity(index).toString());
                } else if(field==statusField) {
                    PVString pvString = (PVString)data;
                    String value = pvString.get();
                    System.out.printf(" status %s",value);
                } else if(field==timeStampField) {
                    PVTimeStamp pvTimeStamp = PVTimeStamp.create(data);
                    TimeStamp timeStamp = new TimeStamp();
                    pvTimeStamp.get(timeStamp);
                    long seconds = timeStamp.secondsPastEpoch;
                    int nano = timeStamp.nanoSeconds;
                    long now = nano/1000000 + seconds*1000;
                    Date date = new Date(now);
                    System.out.printf(" time %s%n",date.toLocaleString());
                }
            }
        }
    }
}
