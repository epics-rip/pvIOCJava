/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;
import org.epics.ioc.channelAccess.*;

/**
 * Factory and implementation of local channel access, i.e. channel access that
 * accesses database records in the local IOC.
 * All user callbacks will be called with the appropriate records locked except for
 * 1) all methods of ChannelStateListener, 2) all methods of ChannelFieldGroupListener,
 * and 3) ChannelRequestListener.requestDone
 * @author mrk
 *
 */
public class ChannelAccessLocalFactory  {
    private static ChannelAccessLocal channelAccess = new ChannelAccessLocal();
    
    /**
     * Create local channel access.
     * Only one instance will be created.
     * @param iocdb The ioc database that the support will access.
     * @return (false,true) if it (already existed, was created).
     */
    static public void setIOCDB(IOCDB iocdb) {
        channelAccess.setIOCDB(iocdb);
    }
    
    private static class ChannelAccessLocal implements ChannelAccess{
        private static AtomicBoolean isRegistered = new AtomicBoolean(false);
        private static ReentrantLock lock = new ReentrantLock();
        private IOCDB iocdb = null;
        
        private void setIOCDB(IOCDB iocdb) {
            boolean result = false;
            lock.lock();
            try {
                this.iocdb = iocdb;
                result = isRegistered.compareAndSet(false, true);
            } finally {
              lock.unlock();  
            }
            if(result) ChannelFactory.registerLocalChannelAccess(this);
        }
        
        public Channel createChannel(String name,ChannelStateListener listener) {
            lock.lock();
            try {
                DBRecord dbRecord = iocdb.findRecord(name);
                if(dbRecord==null) return null;
                return new ChannelImpl(dbRecord,listener);
            } finally {
                lock.unlock();  
            }
        }
    }
        
    private static class ChannelImpl implements ChannelLink {
        private boolean isDestroyed = false;
        private ReentrantLock lock = new ReentrantLock();
        private ChannelStateListener stateListener = null;
        private DBAccess dbAccess;
        private DBLink dbLink = null;
        private DBData currentData = null;
        private String otherChannel = null;
        private String otherField = null;
        private LinkedList<ChannelFieldGroup> fieldGroupList = 
            new LinkedList<ChannelFieldGroup>();
        private LinkedList<ChannelProcess> dataProcessList = 
            new LinkedList<ChannelProcess>();
        private LinkedList<ChannelGet> dataGetList = 
            new LinkedList<ChannelGet>();
        private LinkedList<ChannelPut> dataPutList = 
            new LinkedList<ChannelPut>();
        private LinkedList<ChannelPutGet> dataPutGetList = 
            new LinkedList<ChannelPutGet>();
        private LinkedList<ChannelSubscribe> subscribeList = 
            new LinkedList<ChannelSubscribe>();
        
        private ChannelImpl(DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbAccess = record.getIOCDB().createAccess(record.getRecordName());
            if(dbAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ChannelLink#setLinkRecord(org.epics.ioc.dbAccess.DBRecord)
         */
        public void setDBLink(DBLink dbLink) {
            lock.lock();
            try {
                if(isDestroyed) return;
                this.dbLink = dbLink;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ChannelLink#getLinkRecord()
         */
        public DBLink getDBLink() {
            lock.lock();
            try {
                if(isDestroyed) return null;
                return this.dbLink;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy()
         */
        public void destroy() {
            lock.lock();
            try {
                if(isDestroyed) return;
                isDestroyed = true;
            } finally {
                lock.unlock();
            }
            Iterator<ChannelProcess> iter2 = dataProcessList.iterator();
            while(iter2.hasNext()) {
                ChannelProcess temp = iter2.next();
                temp.destroy();
                iter2.remove();
            }
            Iterator<ChannelGet> iter3 = dataGetList.iterator();
            while(iter3.hasNext()) {
                ChannelGet temp = iter3.next();
                temp.destroy();
                iter3.remove();
            }
            Iterator<ChannelPut> iter4 = dataPutList.iterator();
            while(iter4.hasNext()) {
                ChannelPut temp = iter4.next();
                temp.destroy();
                iter4.remove();
            }
            Iterator<ChannelPutGet> iter5 = dataPutGetList.iterator();
            while(iter5.hasNext()) {
                ChannelPutGet temp = iter5.next();
                temp.destroy();
                iter5.remove();
            }
            Iterator<ChannelSubscribe> iter6 = subscribeList.iterator();
            while(iter6.hasNext()) {
                ChannelSubscribe temp = iter6.next();
                temp.destroy();
                iter6.remove();
            }
            Iterator<ChannelFieldGroup> iter1 = fieldGroupList.iterator();
            while(iter1.hasNext()) {
                ChannelFieldGroup temp = iter1.next();
                temp.destroy();
                iter1.remove();
            }
            stateListener.disconnect(this);
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#isConnected()
         */
        public boolean isConnected() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return false;
                } else {
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#setField(java.lang.String)
         */
        public ChannelSetFieldResult setField(String name) {
            lock.lock();
            try {
                if(isDestroyed) return ChannelSetFieldResult.failure;
                AccessSetResult result = dbAccess.setField(name);
                if(result==AccessSetResult.notFound) return ChannelSetFieldResult.notFound;
                if(result==AccessSetResult.otherRecord) {
                    otherChannel = dbAccess.getOtherRecord();
                    otherField = dbAccess.getOtherField();
                    currentData = null;
                    return ChannelSetFieldResult.otherChannel;
                }
                if(result==AccessSetResult.thisRecord) {
                    currentData = dbAccess.getField();
                    otherChannel = null;
                    otherField = null;
                    return ChannelSetFieldResult.thisChannel;
                }
                throw new IllegalStateException(
                    "ChannelAccessLocal logic error unknown AccessSetResult value");
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getOtherChannel()
         */
        public String getOtherChannel() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    return otherChannel;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getOtherField()
         */
        public String getOtherField() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    return otherField;
                }
            } finally {
                lock.unlock();
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getChannelField()
         */
        public ChannelField getChannelField() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    return new ChannelFieldInstance(currentData);
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createFieldGroup(org.epics.ioc.channelAccess.ChannelFieldGroupListener)
         */
        public ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelFieldGroup fieldGroup = new FieldGroup(listener);
                    fieldGroupList.add(fieldGroup);
                    return fieldGroup;
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelProcess()
         */
        public ChannelProcess createChannelProcess() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelProcess dataProcess = new ChannelProcessInstance(this,dbAccess.getDbRecord());
                    dataProcessList.add(dataProcess);
                    return dataProcess;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelGet()
         */
        public ChannelGet createChannelGet() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelGet dataGet = new ChannelGetInstance(this,dbAccess.getDbRecord());
                    dataGetList.add(dataGet);
                    return dataGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPut()
         */
        public ChannelPut createChannelPut() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPut dataPut = new ChannelPutInstance(this,dbAccess.getDbRecord());
                    dataPutList.add(dataPut);
                    return dataPut;
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPutGet()
         */
        public ChannelPutGet createChannelPutGet() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutGet dataPutGet =  new ChannelPutGetInstance(this,dbAccess.getDbRecord());
                    dataPutGetList.add(dataPutGet);
                    return dataPutGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createSubscribe()
         */
        public ChannelSubscribe createSubscribe(int queueCapacity) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelSubscribe subscribe =  new Subscribe(this,dbAccess.getDbRecord(),queueCapacity);
                    subscribeList.add(subscribe);
                    return subscribe;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#isLocal()
         */
        public boolean isLocal() {
            return true;
        }
    }
    
    private static class ChannelFieldInstance implements ChannelField {
        private DBData dbData;
        
        private ChannelFieldInstance(DBData dbData) {
            this.dbData = dbData;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelField#getAccessRights()
         */
        public AccessRights getAccessRights() {
            // OK until access security is implemented
            if(dbData.getField().isMutable()) {
                return AccessRights.readWrite;
            } else {
                return AccessRights.read;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelField#getField()
         */
        public Field getField() {
            return dbData.getField();
        }       
        private DBData getDBData() {
            return dbData;
        }

    }
    
    private static class FieldGroup implements ChannelFieldGroup {
        private boolean isDestroyed = false;
        private LinkedList<ChannelFieldInstance> fieldList = 
            new LinkedList<ChannelFieldInstance>();

        private FieldGroup(ChannelFieldGroupListener listener) {}
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            fieldList.clear();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#addChannelField(org.epics.ioc.channelAccess.ChannelField)
         */
        public void addChannelField(ChannelField channelField) {
            if(isDestroyed) return;
            fieldList.add((ChannelFieldInstance)channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#removeChannelField(org.epics.ioc.channelAccess.ChannelField)
         */
        public void removeChannelField(ChannelField channelField) {
            if(isDestroyed) return;
            fieldList.remove(channelField);
        }
        
        private List<ChannelFieldInstance> getList() {
            return fieldList;
        }
    }
    private static class ChannelProcessInstance
    implements ChannelProcess,ProcessRequestListener {
        protected ReentrantLock lock = new ReentrantLock();
        protected boolean active = false;
        protected ChannelLink channel;
        protected DBRecord dbRecord;
        protected RecordProcess recordProcess = null;
        protected RecordProcessSupport recordProcessSupport = null;
        protected DBLink dbLink; // is null if client is not link support;
        protected RecordProcessSupport linkRecordProcessSupport = null;
        protected boolean isDestroyed = false;
        private ChannelProcessListener listener = null;
        
        
        private ChannelProcessInstance(ChannelLink channel,DBRecord dbRecord) {
            this.channel = channel;
            this.dbRecord = dbRecord;
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            dbLink = channel.getDBLink();
            if(dbLink!=null) {
                linkRecordProcessSupport = dbLink.getRecord().getRecordProcess().getRecordProcessSupport();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#destroy()
         */
        public void destroy() {
            lock.lock();
            try {
                isDestroyed = true;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#process(org.epics.ioc.channelAccess.ChannelProcessListener, boolean)
         */
        public ChannelRequestReturn process(ChannelProcessListener listener) {
            lock.lock();
            try {
                if(listener==null) {
                    throw new IllegalStateException("no listener");
                } else if(isDestroyed) {
                    listener.message(channel, "channel is destroyed");
                    return ChannelRequestReturn.failure;
                } else if(active) {
                    listener.message(channel, "channel is already active");
                    return ChannelRequestReturn.failure;
                } else {
                    active = true;
                }     
            } finally {
                lock.unlock();
            }
            this.listener = listener;
            ProcessReturn processReturn;
            if(dbLink!=null) {
                processReturn = linkRecordProcessSupport.processLinkedRecord(dbRecord, this);
            } else {
                processReturn = recordProcess.process(this);
            }
            if(processReturn==ProcessReturn.active) return ChannelRequestReturn.active;
            lock.lock();
            try {
                active = false;
                if(processReturn==ProcessReturn.success) return ChannelRequestReturn.success;
                if(processReturn==ProcessReturn.noop) return ChannelRequestReturn.success;
                return ChannelRequestReturn.failure;
            } finally {
                lock.unlock();
            }
        }  
        protected ProcessReturn preProcess(RecordPreProcessListener listener) {
            return recordProcess.preProcess(listener);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#cancelProcess()
         */
        public void cancelProcess() {
            recordProcess.removeCompletionListener(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete() {
            listener.requestDone(channel);
            lock.lock();
            try {
                listener = null;
                active = false;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            listener.requestResult(channel, alarmSeverity, status, timeStamp);
        }
    }
    
    private static class ChannelGetInstance extends ChannelProcessInstance
    implements ChannelGet,ChannelProcessListener {
        
        private FieldGroup fieldGroup = null;
        private ChannelGetListener getListener = null;
        
        private ChannelGetInstance(ChannelLink channel,DBRecord dbRecord) {
            super(channel,dbRecord);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#get(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelGetListener, boolean, boolean)
         */
        public ChannelRequestReturn get(ChannelFieldGroup fieldGroup,
        ChannelGetListener getListener,boolean process ) {
            if(getListener==null) {
                throw new IllegalStateException("no get listener");
            }
            if(fieldGroup==null) {
                throw new IllegalStateException("no field group");
            }
            this.fieldGroup = (FieldGroup)fieldGroup;
            this.getListener = getListener;
            if(process) {
                ChannelRequestReturn result = super.process(this);
                if(result!=ChannelRequestReturn.success) {
                    return result;
                }
            }
            if(isDestroyed) {
                getListener.message(channel, "channel is destroyed");
                return ChannelRequestReturn.failure;
            }
            AlarmSeverity alarmSeverity = AlarmSeverity.none;
            if(dbLink==null) {
                dbRecord.lock();
                try {
                    transferData(channel,alarmSeverity,null,null);
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbLink.getRecord().lockOtherRecord(dbRecord);
                try {
                    transferData(channel,alarmSeverity,null,null);
                } finally {
                    dbRecord.unlock();
                }
            }
            this.fieldGroup = null;
            this.getListener = null;
            return ChannelRequestReturn.success;
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel, String message) {
            getListener.message(channel, message);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(Channel channel,
        AlarmSeverity alarmSeverity, String status,TimeStamp timeStamp)
        {
            dbRecord.lock();
            try {
                if(dbLink==null) {
                    transferData(channel,alarmSeverity,status,timeStamp);
                } else {
                    DBRecord linkRecord = dbLink.getRecord();
                    dbRecord.lockOtherRecord(linkRecord);
                    try {
                        transferData(channel,alarmSeverity,status,timeStamp);
                    } finally {
                        linkRecord.unlock();
                    }
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel) {
            getListener.requestDone(channel);
            fieldGroup = null;
            getListener = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#cancelGet()
         */
        public void cancelGet() {
            super.cancelProcess();
        }
        
        private void transferData(Channel channel,
        AlarmSeverity alarmSeverity, String status,TimeStamp timeStamp)
        {
            getListener.beginSynchronous(channel);
            getListener.requestResult(channel, alarmSeverity, status, timeStamp);
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                getListener.newData(channel,field,field.getDBData());
            }
            getListener.endSynchronous(channel);
        }
    }
    
    private static class ChannelPutInstance extends ChannelProcessInstance
    implements ChannelPut,ChannelProcessListener,RecordPreProcessListener{
        private ChannelFieldGroup fieldGroup = null;
        private ChannelPutListener channelPutListener = null;
        private TimeStamp timeStamp = new TimeStamp();

        private ChannelPutInstance(ChannelLink channel,DBRecord dbRecord) {
            super(channel,dbRecord);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#put(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelPutListener, boolean, boolean)
         */
        public ChannelRequestReturn put(ChannelFieldGroup fieldGroup,
        ChannelPutListener channelPutListener,boolean process)
        {
            if(isDestroyed) return ChannelRequestReturn.failure;
            this.fieldGroup = fieldGroup;
            this.channelPutListener = channelPutListener;
            if(process) {
                if(dbLink==null) {
                    ProcessReturn processReturn = super.preProcess(this);
                    switch(processReturn) {
                    case zombie:  return ChannelRequestReturn.failure;
                    case noop:    break;
                    case success: break;
                    case failure: return ChannelRequestReturn.failure;
                    case active:  return ChannelRequestReturn.active;
                    }
                } else {
                    ProcessReturn processReturn = dbRecord.getRecordProcess().preProcess(dbLink,this);
                    switch(processReturn) {
                    case zombie:  return ChannelRequestReturn.failure;
                    case noop:    break;
                    case success: break;
                    case failure: return ChannelRequestReturn.failure;
                    case active:  return ChannelRequestReturn.active;
                    }
                }
            }
            if(dbLink==null) {
                dbRecord.lock();
                try {
                    transferData();
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbLink.getRecord().lockOtherRecord(dbRecord);
                try {
                    transferData();
                } finally {
                    dbRecord.unlock();
                }
            }
            return ChannelRequestReturn.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordPreProcessListener#failure(java.lang.String)
         */
        public void failure(String message) {
            if(channelPutListener==null) return;
            channelPutListener.message(channel, message);
            channelPutListener.requestDone(channel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordPreProcessListener#readyForProcessing(org.epics.ioc.dbProcess.RecordPreProcess)
         */
        public ProcessReturn readyForProcessing(RecordPreProcess recordPreProcess) {
            if(dbLink==null) {
                transferData();
                return recordPreProcess.processNow(this);
            } else {
                DBRecord linkRecord = dbLink.getRecord();
                dbRecord.lockOtherRecord(linkRecord);
                try {
                    linkRecordProcessSupport.getTimeStamp(timeStamp);
                    recordProcessSupport.setTimeStamp(timeStamp);
                    transferData();
                    return recordPreProcess.processNow(this);
                } finally {
                    linkRecord.unlock();
                }
            }
        }
        private void transferData() {
            List<ChannelFieldInstance> list = ((FieldGroup)fieldGroup).getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                channelPutListener.nextData(channel,field,field.dbData);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#failure(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel, String message) {
            channelPutListener.message(channel, message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#processDone(org.epics.ioc.channelAccess.Channel, org.epics.ioc.dbProcess.ProcessResult, org.epics.ioc.util.AlarmSeverity, java.lang.String)
         */
        public void requestResult(Channel channel,
                AlarmSeverity alarmSeverity, String status,TimeStamp timeStamp) {
            channelPutListener.requestResult(channel, alarmSeverity, status,timeStamp);
        }
        public void requestDone(Channel channel) {
            if(channelPutListener==null) return;
            channelPutListener.requestDone(channel);
        }
        public void cancelPut() {
            super.cancelProcess();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ChannelAccessLocalFactory.ChannelProcessInstance#processComplete()
         */
        public void processComplete() {
            channelPutListener.requestDone(channel);
            lock.lock();
            try {
                channelPutListener = null;
                active = false;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            channelPutListener.requestResult(channel, alarmSeverity, status, timeStamp);
        }
    }
    
    private static class ChannelPutGetInstance implements ChannelPutGet,ChannelPutListener {
        private ChannelPutInstance dataPut;
        private ChannelGetInstance dataGet;
        
        private boolean isDestroyed = false;
        private ChannelPutListener putCallback = null;
        private ChannelFieldGroup getFieldGroup = null;
        private ChannelGetListener getCallback = null;
       
        private ChannelPutGetInstance(ChannelLink channel,DBRecord dbRecord) {
            dataPut = new ChannelPutInstance(channel,dbRecord);
            dataGet = new ChannelGetInstance(channel,dbRecord);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#destroy()
         */
        public void destroy() { 
            isDestroyed = true;
            dataPut.destroy();
            dataGet.destroy();
            return;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#putGet(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelPutListener, org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelGetListener, boolean, boolean)
         */
        public ChannelRequestReturn putGet(  
        ChannelFieldGroup putFieldGroup, ChannelPutListener putCallback,
        ChannelFieldGroup getFieldGroup, ChannelGetListener getCallback,
        boolean process)
        {
            if(process) {
                this.putCallback = putCallback;
                this.getFieldGroup = getFieldGroup;
                this.getCallback = getCallback;
            }
            ChannelRequestReturn result = dataPut.put(putFieldGroup,this,process);
            if(result==ChannelRequestReturn.success) {
                return dataGet.get(getFieldGroup,getCallback,false);
            }
            return result;
            
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#cancelPutGet()
         */
        public void cancelPutGet() {
            if(isDestroyed) return;
            dataPut.cancelPut();
            dataGet.cancelGet();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel, String message) {
            if(isDestroyed) return;
            putCallback.message(channel, message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#processDone(org.epics.ioc.channelAccess.Channel, org.epics.ioc.dbProcess.ProcessResult, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            if(isDestroyed) return;
            putCallback.requestResult(channel, alarmSeverity, status, timeStamp);
            dataGet.get(getFieldGroup,getCallback,false);
        }
        public void requestDone(Channel channel) {
            putCallback.requestDone(channel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#nextData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void nextData(Channel channel, ChannelField field, PVData data) {
            putCallback.nextData(channel, field, data);
        }       
    }
    
    private static class Subscribe implements ChannelSubscribe,DBListener {
        private static CallbackThread callbackThread = new CallbackThread();
        private static Convert convert = ConvertFactory.getConvert();
        private ChannelLink channel;
        private DBRecord dbRecord;
        private DBLink dbLink;
        private boolean isDestroyed = false;
        private RecordListener recordListener = null;
        private FieldGroup fieldGroup = null;
        private ChannelNotifyListener notifyListener = null;
        private ChannelNotifyGetListener dataListener = null;
        NotifyDataQueue notifyDataQueue;
        NotifyData notifyData = null;
        
        
        private static class NotifyData {
            Subscribe subscribe;
            Channel channel;
            DBRecord dbRecord;
            DBLink dbLink;
            FieldGroup fieldGroup;
            ChannelNotifyListener notifyListener = null;
            ChannelNotifyGetListener dataListener = null;
            List<DBData> dataList = new ArrayList<DBData>();
        }
        
        private class NotifyDataQueue {
            ReentrantLock lock = new ReentrantLock();
            private NotifyData[] notifyDataArray;
            private int nextFree = 0;
            private int numberFree = 0;
            
            NotifyDataQueue(Subscribe subscribe,int queueCapacity) {
if(queueCapacity==0) queueCapacity = 1;
                notifyDataArray = new NotifyData[queueCapacity];
                for(int i=0; i < notifyDataArray.length; i++) {
                    NotifyData notifyData = new NotifyData();
                    notifyData.subscribe = subscribe;
                    notifyData.channel = channel;
                    notifyData.dbRecord = dbRecord;
                    notifyData.dbLink = dbLink;
                    notifyDataArray[i] = notifyData;
                    nextFree = 0;
                    numberFree = notifyDataArray.length;
                }
            }
            
            NotifyData getFree() {
                lock.lock();
                try {
                    if(numberFree==0) return null;
                    numberFree--;
                    NotifyData notifyData = notifyDataArray[nextFree];
                    nextFree++;
                    if(nextFree==notifyDataArray.length) nextFree = 0;
                    return notifyData;
                } finally {
                    lock.unlock();
                }
            }
            
            void setFree() {
                lock.lock();
                try {
                    numberFree++;
                } finally {
                    lock.unlock();
                }
            }
            
        }
        
        private Subscribe(ChannelLink channel,DBRecord dbRecord,int queueCapacity)
        {
            this.channel = channel;
            recordListener = dbRecord.createListener(this);
            this.dbRecord = dbRecord;
            dbLink = channel.getDBLink();
            notifyDataQueue = new NotifyDataQueue(this,queueCapacity);
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            if(notifyListener!=null) notifyListener.message(channel,"deleted");
            if(dataListener!=null) dataListener.message(channel,"deleted");
            if(fieldGroup!=null) stop();
            recordListener = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelNotifyGetListener, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup fieldGroup, ChannelNotifyGetListener listener, Event why) {
            if(isDestroyed) return;
            notifyListener = null;
            dataListener = listener;
            startCommon(fieldGroup,why);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelNotifyListener, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup fieldGroup, ChannelNotifyListener listener, Event why) {
            if(isDestroyed) return;
            notifyListener = listener;
            dataListener = null;
            startCommon(fieldGroup,why);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#stop()
         */
        public void stop() {
            if(isDestroyed) return;
            if(fieldGroup==null) return;
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                DBData dbData = field.getDBData();
                dbData.removeListener(this.recordListener);
            }
            fieldGroup = null;
            dataListener = null;
            notifyListener = null;
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#beginSynchronous()
         */
        public void beginSynchronous() {
            if(isDestroyed) return;
            notifyData = notifyDataQueue.getFree();
            if(notifyData==null) return;
            notifyData.fieldGroup = fieldGroup;
            notifyData.notifyListener = notifyListener;
            notifyData.dataListener = dataListener;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
            if(isDestroyed||notifyData==null) return;
            callbackThread.add(notifyData);
            notifyData = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData dbData) {
            // must be expanded to support Event
            if(isDestroyed||notifyData==null) return;
            List<DBData> dataList = notifyData.dataList;
            if(dataListener==null) {
                dataList.add(dbData);
                return;
            }
            PVData pvData = (PVData)dbData;
            Field field = pvData.getField();
            if(convert.isCopyScalarCompatible(field, field)) {
                DBData newData = FieldDataFactory.createData(dbData.getParent(), dbData.getDBDField());
                convert.copyScalar(dbData, newData);
                dataList.add(newData);
            } else {
                dataList.add(dbData);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
         */
        public void unlisten(RecordListener listener) {
            if(fieldGroup!=null) stop();
        }
        
        private void startCommon(ChannelFieldGroup channelFieldGroup, Event why) {
            if(fieldGroup!=null) throw new IllegalStateException("Channel already started");
            fieldGroup = (FieldGroup)channelFieldGroup;
            List<ChannelFieldInstance> list = fieldGroup.getList();
            Iterator<ChannelFieldInstance> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = iter.next();
                DBData dbData = field.getDBData();
                dbData.addListener(this.recordListener);
            }
        }
        private void notifyDone(NotifyData notifyData) {
            
        }
        
        
        private static class CallbackThread implements Runnable {
            private Thread thread;
            private ReentrantLock lock = new ReentrantLock();
            private Condition moreWork = lock.newCondition();
            private List<NotifyData> notifyDataList = new ArrayList<NotifyData>();
            
            CallbackThread() {
                thread = new Thread(this,"ChannelAccessLocal");
                int priority = ScanPriority.valueOf("low").getJavaPriority();
                thread.setPriority(priority);
                thread.start();
            }
                       
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                try {
                    while(true) {
                        lock.lock();
                        try {
                            while(notifyDataList.isEmpty()) {
                                moreWork.await();
                            }
                            NotifyData notifyData = notifyDataList.get(0);
                            notify(notifyData);
                            notifyData.subscribe.notifyDone(notifyData);
                        }finally {
                            lock.unlock();
                        }
                    }
                } catch(InterruptedException e) {
                    
                }
            }
            
            private void add(NotifyData notifyData) {
                lock.lock();
                try {
                    boolean isEmpty = notifyDataList.isEmpty();
                    notifyDataList.add(notifyData);
                    if(isEmpty) moreWork.signal();
                } finally {
                    lock.unlock();
                }
            }
            
            private void notify(NotifyData notifyData) {
                DBRecord dbRecord = notifyData.dbRecord;
                DBRecord linkRecord = notifyData.dbLink.getRecord();
                dbRecord.lock();
                try {
                    dbRecord.lockOtherRecord(linkRecord);
                    try {
                        ChannelNotifyGetListener dataListener = notifyData.dataListener;
                        ChannelNotifyListener notifyListener = notifyData.notifyListener;
                        Channel channel = notifyData.channel;
                        List<DBData> dataList = notifyData.dataList;
                        FieldGroup fieldGroup = notifyData.fieldGroup;
                        if(dataListener!=null) {
                            dataListener.beginSynchronous(channel);
                        }
                        if(notifyListener!=null) {
                            notifyListener.beginSynchronous(channel);
                        }
                        while(dataList.size()>0) {
                            DBData dbData = dataList.remove(0);
                            ChannelFieldInstance field = null;
                            List<ChannelFieldInstance> list = fieldGroup.getList();
                            Iterator<ChannelFieldInstance> iter = list.iterator();
                            
                            while(iter.hasNext()) {
                                ChannelFieldInstance fieldNow = iter.next();
                                if(dbData==fieldNow.getDBData()) {
                                    field = fieldNow;
                                    break;
                                }
                            }
                            if(field==null) {
                                throw new IllegalStateException("ChannelAccessLocalFactory logic error");
                            }
                            if(dataListener!=null) {
                                dataListener.newData(channel,field,dbData);
                            } else {
                                notifyListener.newData(channel,field);
                            }
                        }
                        if(dataListener!=null) {
                            dataListener.endSynchronous(channel);
                        }
                        if(notifyListener!=null) {
                            notifyListener.endSynchronous(channel);
                        }
                    } finally {
                        linkRecord.unlock();
                    }
                } finally {
                    dbRecord.unlock();
                }
            }
        }
    }
}
