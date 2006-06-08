/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;
import java.util.concurrent.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.Field;
import org.epics.ioc.pvAccess.PVData;
import org.epics.ioc.pvAccess.Property;
import org.epics.ioc.channelAccess.*;

/**
 * @author mrk
 *
 */
public class ChannelAccessLocal implements ChannelAccess {
    
    private IOCDB iocdb;
    
    public ChannelAccessLocal(IOCDB iocdb) {
        this.iocdb = iocdb;
        if(!ChannelFactory.registerLocalChannelAccess(this)) {
            throw new IllegalStateException(
                "ChannelAccessLocal is already registered");
        }
    }
    
    public Channel createChannel(String name) {
        DBRecord dbRecord = iocdb.findRecord(name);
        if(dbRecord==null) return null;
        return new LocalChannel(iocdb,dbRecord);
    }
    
    private static class LocalChannel implements Channel {
        private IOCDB iocdb;
        private DBAccess dbAccess;
        private ChannelDataLocal recordData;
        private ChannelDataLocal currentData;
        private TreeSet<ChannelDataLocal> dataList = 
            new TreeSet<ChannelDataLocal>();
        private ConcurrentLinkedQueue<ChannelStateListener> stateListenerList = 
            new ConcurrentLinkedQueue<ChannelStateListener>();
        private String otherRecord = null;
        private String otherField = null;
        
        LocalChannel(IOCDB iocdb,DBRecord record) {
            this.iocdb = iocdb;
            dbAccess = iocdb.createAccess(record.getRecordName());
            DBData dbData = dbAccess.getField();
            recordData = new ChannelDataLocal(this,dbData);
            dataList.add(recordData);
            currentData = recordData;
        }
        
        public void destroy() {
            iocdb = null;
            dbAccess = null;
            recordData = null;
            currentData = null;
            dataList = null;
            ChannelStateListener listener;
            while((listener= stateListenerList.poll())!=null) {
                listener.notify();
            }
        }
        
        public boolean isConnected() {
            if(iocdb!=null) return true;
            return false;
        }
        
        public void addListener(ChannelStateListener listener) {
            stateListenerList.add(listener);
        }
        
        public void removeListener(ChannelStateListener listener) {
            stateListenerList.remove(listener);
        }
        
        public ChannelSetResult setField(String name) {
            AccessSetResult result = dbAccess.setField(name);
            if(result==AccessSetResult.notFound) return ChannelSetResult.notFound;
            if(result==AccessSetResult.otherRecord) {
                otherRecord = dbAccess.getOtherRecord();
                otherField = dbAccess.getOtherField();
                return ChannelSetResult.otherChannel;
            }
            DBData dbData = dbAccess.getField();
            if(dbData==null)  {
                throw new IllegalStateException(
                    "LocalChannel.setField dbData is null. Logic error");
            }
            Iterator<ChannelDataLocal> iter = dataList.iterator();
            while(iter.hasNext()) {
                ChannelDataLocal local = iter.next();
                if(local.dbData==dbData) {
                    currentData = local;
                    return ChannelSetResult.thisChannel;
                }
            }
            currentData = new ChannelDataLocal(this,dbData);
            dataList.add(currentData);
            return ChannelSetResult.thisChannel;
        }

        public String getOtherChannel() {
            return otherRecord;
        }

        public String getOtherField() {
            return otherField;
        }
        
        public ChannelData getField() {
            return currentData;
        }

        public ChannelData getPropertyField(Property property) {
            if(property==null) return null;
            DBData dbData = dbAccess.getPropertyField(property);
            if(dbData==null) return null;
            Iterator<ChannelDataLocal> iter = dataList.iterator();
            while(iter.hasNext()) {
                ChannelDataLocal local = iter.next();
                if(local.dbData==dbData) {
                    currentData = local;
                    return currentData;
                }
            }
            currentData = new ChannelDataLocal(this,dbData);
            dataList.add(currentData);
            return currentData;
        }

        public ChannelData getPropertyField(String name) {
            DBData dbData = dbAccess.getPropertyField(name);
            Property property = dbData.getField().getProperty(name);
            return getPropertyField(property);
        }
        
        public AccessRights getAccessRights() {
            // OK until access security is implemented
            if(dbAccess.getField().getField().isMutable()) {
                return AccessRights.readWrite;
            } else {
                return AccessRights.read;
            }
        }
        
        public void subscribe(ChannelDataListener listener, Event why) {
            // TODO Auto-generated method stub
            
        }

        public void subscribe(ChannelNotifyListener listener, Event why) {
            // TODO Auto-generated method stub
            
        }

        public ChannelGetReturn get(ChannelDataGet callback, ChannelOption[] options) {
            // TODO Auto-generated method stub
            return null;
        }

        
        public ChannelDataPut getChannelDataPut() {
            // TODO Auto-generated method stub
            return null;
        } 

    }
    
    private static class DataGet implements ProcessComplete {
        
        private RecordProcess recordProcess;
        private ChannelDataLocal channelData;
        private ChannelDataGet callback = null;
        
        DataGet(RecordProcess recordProcess,ChannelDataLocal channelData) {
            this.recordProcess = recordProcess;
            this.channelData = channelData;
        }

        ChannelGetReturn get(ChannelDataGet callback, ChannelOption[] options) {
            this.callback = callback;
            boolean process = false;
            boolean wait = false;
            for(ChannelOption option : options) {
                switch(option) {
                case process: process = true; break;
                case wait: wait = true; break;
                }
            }
            ProcessReturn processReturn = ProcessReturn.done;
            if(process) {
                processReturn = recordProcess.requestProcess(this);
            }
            return null;
        }
        public void complete(ProcessReturn result) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    private static class SubscriptionNotify implements DBListener, ChannelNotify{
        
        private ChannelDataLocal channelData;
        private ChannelNotifyListener listener;
        private Event event;
        private DBData dbData = null;
        
        SubscriptionNotify(ChannelDataLocal channelData,
            ChannelNotifyListener listener, Event why)
        {
            this.channelData = channelData;
            this.listener = listener;
            this.event = event;
            dbData = channelData.getDBData();
            dbData.addListener(this);
        }

        public void beginSynchronous() {
            // nothing to do
        }

        public void endSynchronous() {
            // nothing to do
        }

        public void newData(DBData dbData) {
            listener.newModification(this,event);
        }

        public Channel getChannel() {
            return channelData.getChannel();
        }

        public Field getField() {
            return channelData.getPVData().getField();
        }
        
    }
    
    private static class ChannelDataLocal implements ChannelData, Comparable {
        
        private Channel channel;
        private DBData dbData;
        private long thisInstance;
        private static long numberInstances = 0;
        
        ChannelDataLocal(Channel channel,DBData dbData) {
        this .channel = channel;
            this.dbData = dbData;
            thisInstance = numberInstances++;
        }
        
        public DBData getDBData() {
            return dbData;
        }

        public PVData getPVData() {
            return dbData;
        }

        public int compareTo(Object o) {
            ChannelDataLocal other = (ChannelDataLocal)o;
            long diff = thisInstance - other.thisInstance;
            if(diff<0) return -1;
            if(diff>0) return 1;
            return 0;
        }

        public Channel getChannel() {
            return channel;
        }
        
    }
}
