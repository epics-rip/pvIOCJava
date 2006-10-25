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
 * and 3) ChannelRequestor.requestDone
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
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelAccess#createChannel(java.lang.String, org.epics.ioc.channelAccess.ChannelStateListener)
         */
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
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelAccess#createLinkChannel(org.epics.ioc.dbAccess.DBLink, java.lang.String, org.epics.ioc.channelAccess.ChannelStateListener)
         */
        public Channel createLinkChannel(DBLink dbLink, String name, ChannelStateListener listener) {
            lock.lock();
            try {
                DBRecord dbRecord = iocdb.findRecord(name);
                if(dbRecord==null) return null;
                return new ChannelImpl(dbLink,dbRecord,listener);
            } finally {
                lock.unlock();  
            }
        }
    }
        
    private static class ChannelImpl implements Channel {
        private boolean isDestroyed = false;
        private ReentrantLock lock = new ReentrantLock();
        private ChannelStateListener stateListener = null;
        private DBLink dbLink = null;
        private DBAccess dbAccess;
        private DBData currentData = null;
        private String otherChannel = null;
        private String otherField = null;
        private LinkedList<ChannelFieldGroup> fieldGroupList = 
            new LinkedList<ChannelFieldGroup>();
        private LinkedList<ChannelSubscribe> subscribeList = 
            new LinkedList<ChannelSubscribe>();
        
        private ChannelImpl(DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbAccess = record.getIOCDB().createAccess(record.getRecordName());
            if(dbAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        private ChannelImpl(DBLink dbLink,DBRecord record,ChannelStateListener listener) {
            this.dbLink = dbLink;
            stateListener = listener;
            dbAccess = record.getIOCDB().createAccess(record.getRecordName());
            if(dbAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#getDBLink()
         */
        public DBLink getDBLink() {
            return dbLink;
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
        public ChannelProcess createChannelProcess(ChannelProcessRequestor channelProcessRequestor) {
            lock.lock();
            try {
                if(isDestroyed) {
                    channelProcessRequestor.message(this,"channel has been destroyed");
                    return null;
                } else {
                    ChannelProcess channelProcess;
                    try {
                        channelProcess = new ChannelProcessInstance(
                            this,dbAccess.getDbRecord(),channelProcessRequestor);
                    } catch(IllegalStateException e) {
                        channelProcessRequestor.message(this, e.getMessage());
                        return null;
                    }
                    return channelProcess;
                }
            } finally {
                lock.unlock();
            }
        }
        public ChannelPreProcess createChannelPreProcess(ChannelPreProcessRequestor channelPreProcessRequestor) {
            lock.lock();
            try {
                if(isDestroyed) {
                    channelPreProcessRequestor.message(this,"channel has been destroyed");
                    return null;
                } else {
                    ChannelPreProcess channelPreProcess;
                    try {
                        channelPreProcess = new ChannelPreProcessInstance(
                            this,dbAccess.getDbRecord(),channelPreProcessRequestor);
                    } catch(IllegalStateException e) {
                        channelPreProcessRequestor.message(this, e.getMessage());
                        return null;
                    }
                    return channelPreProcess;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelGet()
         */
        public ChannelGet createChannelGet(ChannelGetRequestor channelGetRequestor, boolean processBeforeGet) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelGet channelGet;
                    try {
                        channelGet = new ChannelGetInstance(
                            this,dbAccess.getDbRecord(),channelGetRequestor,processBeforeGet);
                    } catch(IllegalStateException e) {
                        channelGetRequestor.message(this, e.getMessage());
                        return null;
                    }
                    return channelGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPut()
         */
        public ChannelPut createChannelPut(ChannelPutRequestor channelPutRequestor, boolean processAfterPut) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPut dataPut = new ChannelPutInstance(
                            this,dbAccess.getDbRecord(),channelPutRequestor,processAfterPut);
                    return dataPut;
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPutGet()
         */
        public ChannelPutGet createChannelPutGet(
        ChannelPutGetRequestor channelPutGetRequestor, boolean processAfterPut)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutGet dataPutGet =  new ChannelPutGetInstance(
                        this,dbAccess.getDbRecord(),channelPutGetRequestor,processAfterPut);
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
        private LinkedList<ChannelField> fieldList = 
            new LinkedList<ChannelField>();

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
            fieldList.add(channelField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroup#removeChannelField(org.epics.ioc.channelAccess.ChannelField)
         */
        public void removeChannelField(ChannelField channelField) {
            if(isDestroyed) return;
            fieldList.remove(channelField);
        }
        
        public List<ChannelField> getList() {
            return fieldList;
        }
    }
    
    private static class ChannelProcessInstance
    implements ChannelProcess,ProcessCallbackListener,
    RecordProcessRequestor,ProcessContinueListener
    {
        private Channel channel;
        private ChannelProcessRequestor channelProcessRequestor = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        
        private DBLink dbLink = null;
        private DBRecord linkRecord = null;
        private RecordProcessSupport linkRecordProcessSupport = null;
        private TimeStamp timeStamp = null;
        private RequestResult requestResult = null;
             
        private ChannelProcessInstance(
        Channel channel,DBRecord dbRecord,ChannelProcessRequestor channelProcessRequestor)
        {
            this.channel = channel;
            this.channelProcessRequestor = channelProcessRequestor;
            this.dbRecord = dbRecord;
            recordProcess = dbRecord.getRecordProcess();
            boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
            if(!isRequestor) {
                throw new IllegalStateException("record already has recordProcessRequestor"); 
            }
            dbLink = channel.getDBLink();
            if(dbLink!=null) {
                linkRecord = dbLink.getRecord();
                linkRecordProcessSupport = linkRecord.getRecordProcess().getRecordProcessSupport();
                timeStamp = new TimeStamp();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#process()
         */
        public RequestResult process() {
            if(!channel.isConnected()) {
                channelProcessRequestor.message(channel, "channel is not connected");
                return RequestResult.failure;
            }
            if(dbLink==null) {
                return recordProcess.process(this, null);
            } else {
                // This is called with linked record locked
                linkRecordProcessSupport.getTimeStamp(timeStamp);
                linkRecordProcessSupport.requestProcessCallback(this);
                return RequestResult.active;
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#processCallback()
         */
        public void processCallback() {
            RequestResult requestResult = recordProcess.process(this, timeStamp);
            if(requestResult!=RequestResult.active) {
                recordProcessComplete(requestResult);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#getRecordProcessRequestorName()
         */
        public String getRecordProcessRequestorName() {
            return channelProcessRequestor.getChannelRequestorName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#processResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            if(dbLink==null) {
                channelProcessRequestor.processResult(channel, alarmSeverity, status, timeStamp);
            } else {
                dbRecord.lockOtherRecord(linkRecord);
                try {
                    channelProcessRequestor.processResult(channel, alarmSeverity, status, timeStamp);
                } finally {
                    linkRecord.unlock();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.dbProcess.RequestResult)
         */
        public void recordProcessComplete(RequestResult requestResult) {
            if(dbLink==null) {
                channelProcessRequestor.requestDone(channel, requestResult);
            } else {
                // must call channelProcessRequestor via processContinue
                this.requestResult = requestResult;
                linkRecordProcessSupport.processContinue(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueListener#processContinue()
         */
        public void processContinue() {
            channelProcessRequestor.requestDone(channel, requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueListener#getName()
         */
        public String getName() {
            return "ChannelProcess:" + channelProcessRequestor.getChannelRequestorName();
        }
    }

    private static class ChannelPreProcessInstance
    implements ChannelPreProcess,ProcessCallbackListener,
    SupportPreProcessRequestor,RecordProcessRequestor,ProcessContinueListener
    {
        private Channel channel;
        private ChannelPreProcessRequestor channelPreProcessRequestor = null;
        private RecordProcess recordProcess = null;
        
        private DBLink dbLink = null;
        private RecordProcessSupport linkRecordProcessSupport = null;
        private TimeStamp timeStamp = null;
        private RequestResult requestResult = null;
             
        private ChannelPreProcessInstance(
        Channel channel,DBRecord dbRecord,ChannelPreProcessRequestor channelPreProcessRequestor)
        {
            this.channel = channel;
            this.channelPreProcessRequestor = channelPreProcessRequestor;
            recordProcess = dbRecord.getRecordProcess();
            boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
            if(!isRequestor) {
                throw new IllegalStateException("record already has recordProcessRequestor"); 
            }
            dbLink = channel.getDBLink();
            if(dbLink!=null) {
                linkRecordProcessSupport = dbLink.getRecord().getRecordProcess().getRecordProcessSupport();
                timeStamp = new TimeStamp();
            }
        }
            
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPreProcess#preProcess()
         */
        public RequestResult preProcess() {
            if(!channel.isConnected()) {
                channelPreProcessRequestor.message(channel, "channel is not connected");
                return RequestResult.failure;
            }
            if(dbLink==null) {
                return recordProcess.preProcess(this, this);
            } else {
                linkRecordProcessSupport.getTimeStamp(timeStamp);
                linkRecordProcessSupport.requestProcessCallback(this);
                return RequestResult.active;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPreProcess#processNow()
         */
        public RequestResult processNow() {
            return recordProcess.processNow(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackListener#processCallback()
         */
        public void processCallback() {
            RequestResult requestResult = recordProcess.preProcess(this, this, timeStamp);
            if(requestResult!=RequestResult.active) {
                recordProcessComplete(requestResult);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getSupportProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return channelPreProcessRequestor.getChannelRequestorName();
        }   
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#processComplete(org.epics.ioc.dbProcess.RequestResult)
         */
        public void processComplete(RequestResult requestResult) {
            channelPreProcessRequestor.requestDone(channel,requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportPreProcessRequestor#ready()
         */
        public RequestResult ready() {
            return channelPreProcessRequestor.ready();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#getRecordProcessRequestorName()
         */
        public String getRecordProcessRequestorName() {
            return channelPreProcessRequestor.getChannelRequestorName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            channelPreProcessRequestor.processResult(channel, alarmSeverity, status, timeStamp);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.dbProcess.RequestResult)
         */
        public void recordProcessComplete(RequestResult requestResult) {
            if(dbLink==null) {
                channelPreProcessRequestor.requestDone(channel, requestResult);
            } else {
                // must call channelPreProcessRequestor via processContinue
                this.requestResult = requestResult;
                linkRecordProcessSupport.processContinue(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueListener#processContinue()
         */
        public void processContinue() {
            channelPreProcessRequestor.requestDone(channel, requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueListener#getName()
         */
        public String getName() {
            return "ChannelPreProcess:" + channelPreProcessRequestor.getChannelRequestorName();
        }
    }
    
    private static class ChannelGetInstance implements ChannelGet,
    ChannelProcessRequestor
    {
        private Channel channel;
        private DBRecord dbRecord = null;
        private ChannelGetRequestor channelGetRequestor = null;
        private ChannelProcess channelProcess = null;
        private FieldGroup fieldGroup = null;
        
        private DBLink dbLink = null;
        private DBRecord linkRecord = null;
        
        private ChannelGetInstance(Channel channel,DBRecord dbRecord,
        ChannelGetRequestor channelGetRequestor, boolean processBeforeGet) {
            this.channel = channel;
            this.dbRecord = dbRecord;
            this.channelGetRequestor = channelGetRequestor;
            if(processBeforeGet) {
                channelProcess = channel.createChannelProcess(this);
            }
            dbLink = channel.getDBLink();
            if(dbLink!=null) {
                linkRecord = dbLink.getRecord();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#get(org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public RequestResult get(ChannelFieldGroup fieldGroup) {
            if(!channel.isConnected()) {
                channelGetRequestor.message(channel, "channel is not connected");
                return RequestResult.failure;
            }
            if(fieldGroup==null) {
                throw new IllegalStateException("no field group");
            }
            this.fieldGroup = (FieldGroup)fieldGroup;
            if(channelProcess!=null) return channelProcess.process();
            if(dbLink==null) {
                dbRecord.lock();
            } else {
                linkRecord.lockOtherRecord(dbRecord);
            }
            try {
                transferData();
            } finally {
                dbRecord.unlock();
            }
            return RequestResult.success;
        }         
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessRequestor#processResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void processResult(Channel channel,
        AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            channelGetRequestor.processResult(channel, alarmSeverity, status, timeStamp);
            transferData();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#getChannelRequestorName()
         */
        public String getChannelRequestorName() {
            return "ChannelGet:" +channelGetRequestor.getChannelRequestorName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel, String message) {
            channelGetRequestor.message(channel, message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            channelGetRequestor.requestDone(channel, requestResult);
        }
        
        private void transferData()
        {
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                channelGetRequestor.newData(channel,field,field.getDBData());
            }
        }
    }
    
    private static class ChannelPutInstance implements ChannelPut,
    ChannelPreProcessRequestor
    {
        private Channel channel;
        private DBRecord dbRecord = null;
        private ChannelPutRequestor channelPutRequestor = null;
        private ChannelPreProcess channelPreProcess = null;
        private FieldGroup fieldGroup = null;
        
        private DBLink dbLink = null;
        private DBRecord linkRecord = null;
        
        private ChannelPutInstance(Channel channel,DBRecord dbRecord,
        ChannelPutRequestor channelPutRequestor, boolean processAfterPut) {
            this.channel = channel;
            this.dbRecord = dbRecord;
            this.channelPutRequestor = channelPutRequestor;
            if(processAfterPut) {
                channelPreProcess = channel.createChannelPreProcess(this);
            }
            dbLink = channel.getDBLink();
            if(dbLink!=null) {
                linkRecord = dbLink.getRecord();
            }
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#put(org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public RequestResult put(ChannelFieldGroup fieldGroup) {
            if(!channel.isConnected()) {
                channelPutRequestor.message(channel, "channel is not connected");
                return RequestResult.failure;
            }
            if(fieldGroup==null) {
                throw new IllegalStateException("no field group");
            }
            this.fieldGroup = (FieldGroup)fieldGroup;
            if(channelPreProcess!=null) return channelPreProcess.preProcess();
            if(dbLink==null) {
                dbRecord.lock();
            } else {
                linkRecord.lockOtherRecord(dbRecord);
            }
            try {
                transferData();        
            } finally {
                dbRecord.unlock();
            }
            return RequestResult.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPreProcessRequestor#processResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void processResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            channelPutRequestor.processResult(channel, alarmSeverity, status, timeStamp);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPreProcessRequestor#ready()
         */
        public RequestResult ready() {
            if(dbLink==null) {
                transferData();
            }
            dbRecord.lockOtherRecord(linkRecord);
            try {
                transferData();
            } finally {
                linkRecord.unlock();
            }
            return channelPreProcess.processNow();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#getChannelRequestorName()
         */
        public String getChannelRequestorName() {
            return "ChannelPut:" + channelPutRequestor.getChannelRequestorName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel, String message) {
            channelPutRequestor.message(channel, message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            channelPutRequestor.requestDone(channel, requestResult);
        }

        private void transferData() {
            List<ChannelField> list = ((FieldGroup)fieldGroup).getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                channelPutRequestor.nextData(channel,field,field.dbData);
            }
        }
    }
    
    private static class ChannelPutGetInstance implements ChannelPutGet,
    ChannelPutRequestor,ChannelGetRequestor
    {
        private ChannelPutGetRequestor channelPutGetRequestor = null;
        private ChannelPut channelPut = null;
        private ChannelGet channelGet = null;
        private ChannelFieldGroup getFieldGroup = null;
        private ChannelPutGetInstance(Channel channel,DBRecord dbRecord,
        ChannelPutGetRequestor channelPutGetRequestor, boolean processAfterPut)
        {
            this.channelPutGetRequestor = channelPutGetRequestor;
            channelPut = channel.createChannelPut(this, processAfterPut);
            channelGet = channel.createChannelGet(this,false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#putGet(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public RequestResult putGet(
        ChannelFieldGroup putFieldGroup, ChannelFieldGroup getFieldGroup)
        {
            RequestResult requestResult;
            this.getFieldGroup = getFieldGroup;
                requestResult = channelPut.put(putFieldGroup);
            if(requestResult==RequestResult.active) return requestResult;
            if(requestResult!=RequestResult.success) return requestResult;
            return channelGet.get(getFieldGroup);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutRequestor#nextData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void nextData(Channel channel, ChannelField field, PVData data) {
            channelPutGetRequestor.newData(channel, field, data);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#newData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void newData(Channel channel, ChannelField field, PVData data) {
            channelPutGetRequestor.newData(channel, field, data);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPreProcessRequestor#processResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void processResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            channelPutGetRequestor.processResult(channel, alarmSeverity, status, timeStamp);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPreProcessRequestor#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException("ready should never be called for ChannelPutGet");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#getChannelRequestorName()
         */
        public String getChannelRequestorName() {
            return "ChannelPutGet:" + channelPutGetRequestor.getChannelRequestorName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel, String message) {
            channelPutGetRequestor.message(channel, message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            channelGet.get(getFieldGroup);
            channelPutGetRequestor.requestDone(channel, requestResult);
        }       
            
    }
    
    private static class Subscribe implements ChannelSubscribe,DBListener {
        private static CallbackThread callbackThread = new CallbackThread();
        private static Convert convert = ConvertFactory.getConvert();
        private Channel channel;
        private DBRecord dbRecord;
        private DBLink dbLink = null;
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
        
        private Subscribe(Channel channel,DBRecord dbRecord,int queueCapacity)
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
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
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
            notifyData.dataList.clear();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
            if(isDestroyed||notifyData==null) return;
            callbackThread.add(notifyDataQueue,notifyData);
            notifyData = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData dbData) {
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
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
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
            private List<NotifyDataQueue> notifyDataQueueList = new ArrayList<NotifyDataQueue>();
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
                            NotifyDataQueue notifyDataQueue = notifyDataQueueList.get(0);
                            notify(notifyData);
                            notifyData.subscribe.notifyDone(notifyData);
                            notifyDataQueue.setFree();
                        }finally {
                            lock.unlock();
                        }
                    }
                } catch(InterruptedException e) {
                    
                }
            }
            
            private void add(NotifyDataQueue notifyDataQueue,NotifyData notifyData) {
                lock.lock();
                try {
                    boolean isEmpty = notifyDataList.isEmpty();
                    notifyDataQueueList.add(notifyDataQueue);
                    notifyDataList.add(notifyData);
                    if(isEmpty) moreWork.signal();
                } finally {
                    lock.unlock();
                }
            }
            
            private void notify(NotifyData notifyData) {
                DBRecord dbRecord = notifyData.dbRecord;
                DBLink dbLink = notifyData.dbLink;                
                if(dbLink==null) {
                    notifyCommon(notifyData);
                } else {
                    DBRecord linkRecord = dbLink.getRecord();
                    dbRecord.lockOtherRecord(linkRecord);
                    try {
                        notifyCommon(notifyData);
                    } finally {
                        linkRecord.unlock();
                    }
                }
            }
            
            private void notifyCommon(NotifyData notifyData) {
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
                    List<ChannelField> list = fieldGroup.getList();
                    Iterator<ChannelField> iter = list.iterator();
                    
                    while(iter.hasNext()) {
                        ChannelFieldInstance fieldNow = (ChannelFieldInstance)iter.next();
                        if(dbData.getField()==fieldNow.getField()) {
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
                    
            }
        }
    }
}
