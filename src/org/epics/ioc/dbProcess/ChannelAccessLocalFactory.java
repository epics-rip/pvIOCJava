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
    }
    
    private static class ChannelImpl implements Channel {
        private boolean isDestroyed = false;
        private ReentrantLock lock = new ReentrantLock();
        private ChannelStateListener stateListener = null;
        private DBAccess dbAccess;
        private DBData currentData = null;
        private String otherChannel = null;
        private String otherField = null;
        private LinkedList<ChannelFieldGroup> fieldGroupList = 
            new LinkedList<ChannelFieldGroup>();
        private LinkedList<ChannelProcess> channelProcessList =
            new LinkedList<ChannelProcess>();
        private LinkedList<ChannelGet> channelGetList =
            new LinkedList<ChannelGet>();
        private LinkedList<ChannelPut> channelPutList =
            new LinkedList<ChannelPut>();
        private LinkedList<ChannelPutGet> channelPutGetList =
            new LinkedList<ChannelPutGet>();
        private LinkedList<ChannelSubscribe> channelSubscribeList = 
            new LinkedList<ChannelSubscribe>();
        
        private ChannelImpl(DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbAccess = record.getIOCDB().createAccess(record.getRecordName());
            if(dbAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy()
         */
        public void destroy() {
            lock.lock();
            try {
                if(isDestroyed) return;
                Iterator<ChannelProcess> processIter = channelProcessList.iterator();
                while(processIter.hasNext()) {
                    ChannelProcess channelProcess = processIter.next();
                    channelProcess.destroy();
                    processIter.remove();
                }
                Iterator<ChannelGet> getIter = channelGetList.iterator();
                while(getIter.hasNext()) {
                    ChannelGet channelGet = getIter.next();
                    channelGet.destroy();
                    getIter.remove();
                }
                Iterator<ChannelPut> putIter = channelPutList.iterator();
                while(putIter.hasNext()) {
                    ChannelPut channelPut = putIter.next();
                    channelPut.destroy();
                    putIter.remove();
                }
                Iterator<ChannelPutGet> putGetIter = channelPutGetList.iterator();
                while(putGetIter.hasNext()) {
                    ChannelPutGet channelPutGet = putGetIter.next();
                    channelPutGet.destroy();
                    putGetIter.remove();
                }
                Iterator<ChannelSubscribe> subscribeIter = channelSubscribeList.iterator();
                while(subscribeIter.hasNext()) {
                    ChannelSubscribe channelSubscribe = subscribeIter.next();
                    channelSubscribe.destroy();
                    subscribeIter.remove();
                }
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
        public ChannelProcess createChannelProcess(ChannelRequestor channelRequestor) {
            lock.lock();
            try {
                if(isDestroyed) {
                    channelRequestor.message(this,
                        "channel has been destroyed",MessageType.fatalError);
                    return null;
                } else {
                    ChannelProcess channelProcess;
                    try {
                        channelProcess = new ChannelProcessInstance(
                            this,dbAccess.getDbRecord(),channelRequestor);
                        channelProcessList.add(channelProcess);
                    } catch(IllegalStateException e) {
                        channelRequestor.message(this,
                            e.getMessage(),MessageType.fatalError);
                        return null;
                    }
                    return channelProcess;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelGet(org.epics.ioc.channelAccess.ChannelGetRequestor, org.epics.ioc.channelAccess.ChannelProcessRequestor)
         */
        public ChannelGet createChannelGet(ChannelGetRequestor channelGetRequestor,boolean process) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelGet channelGet;
                    try {
                        channelGet = new ChannelGetInstance(
                            this,dbAccess.getDbRecord(),channelGetRequestor,process);
                        channelGetList.add(channelGet);
                    } catch(IllegalStateException e) {
                        channelGetRequestor.message(this,
                            e.getMessage(),MessageType.fatalError);
                        return null;
                    }
                    return channelGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPut(org.epics.ioc.channelAccess.ChannelPutRequestor, org.epics.ioc.channelAccess.ChannelProcessRequestor)
         */
        public ChannelPut createChannelPut(ChannelPutRequestor channelPutRequestor,boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPut channelPut = new ChannelPutInstance(
                            this,dbAccess.getDbRecord(),channelPutRequestor,process);
                    channelPutList.add(channelPut);
                    return channelPut;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createChannelPutGet(org.epics.ioc.channelAccess.ChannelPutGetRequestor, org.epics.ioc.channelAccess.ChannelProcessRequestor)
         */
        public ChannelPutGet createChannelPutGet(ChannelPutGetRequestor channelPutGetRequestor,boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutGet channelPutGet =  new ChannelPutGetInstance(
                        this,dbAccess.getDbRecord(),channelPutGetRequestor,process);
                    channelPutGetList.add(channelPutGet);
                    return channelPutGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#createSubscribe()
         */
        public ChannelSubscribe createSubscribe() {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelSubscribe subscribe =  new Subscribe(this,dbAccess.getDbRecord());
                    channelSubscribeList.add(subscribe);
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
    
    private static class ChannelProcessInstance implements ChannelProcess,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private Channel channel;
        private String requestorName;
        private ChannelRequestor channelRequestor = null;
        private RecordProcess recordProcess = null;
        
        private RequestResult requestResult = null;
             
        private ChannelProcessInstance(
        Channel channel,DBRecord dbRecord,ChannelRequestor channelRequestor)
        {
            this.channel = channel;
            this.channelRequestor = channelRequestor;
            recordProcess = dbRecord.getRecordProcess();
            boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
            if(!isRequestor) {
                throw new IllegalStateException("record already has recordProcessRequestor"); 
            }
            requestorName = "ChannelProcess:" + channelRequestor.getRequestorName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#destroy()
         */
        public void destroy() {
            if(isDestroyed) return;
            recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#process()
         */
        public void process() {
            if(!channel.isConnected()) {
                channelRequestor.message(channel,
                    "channel is not connected",MessageType.info);
                channelRequestor.requestDone(channel, RequestResult.failure);
            }
            recordProcess.process(this, false, null);
            
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return requestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#processResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.dbProcess.RequestResult)
         */
        public void recordProcessComplete() {
            channelRequestor.requestDone(channel, requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException("Logic error. Why was this called?");
        }
    }
    
    private static class ChannelGetInstance implements ChannelGet,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private ChannelImpl channel;
        private String requestorName;
        private DBRecord dbRecord = null;
        private ChannelGetRequestor channelGetRequestor = null;
        private FieldGroup fieldGroup = null;
        private RequestResult requestResult = RequestResult.success;
        
        private RecordProcess recordProcess = null;
        
        private ChannelGetInstance(ChannelImpl channel,DBRecord dbRecord,
        ChannelGetRequestor channelGetRequestor,boolean process) {
            this.channel = channel;
            this.dbRecord = dbRecord;
            this.channelGetRequestor = channelGetRequestor;
            requestorName = "ChannelGet:" + channelGetRequestor.getRequestorName();
            if(process) {
                recordProcess = dbRecord.getRecordProcess();
                boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
                if(!isRequestor) {
                    throw new IllegalStateException("record already has recordProcessRequestor"); 
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#destroy()
         */
        public void destroy() {
            if(isDestroyed) return;
            if(recordProcess!=null) recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#get(org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public void get(ChannelFieldGroup fieldGroup) {
            if(!channel.isConnected()) {
                channelGetRequestor.message(channel,
                    "channel is not connected",MessageType.info);
                channelGetRequestor.requestDone(channel, RequestResult.failure);
            }
            if(fieldGroup==null) {
                throw new IllegalStateException("no field group");
            }
            this.fieldGroup = (FieldGroup)fieldGroup;
            if(recordProcess!=null) {
                recordProcess.process(this, true, null);
                return;
            }
            getData();
            channelGetRequestor.requestDone(channel, RequestResult.success);
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return requestorName;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            getData();
            recordProcess.setInactive(this);
            channelGetRequestor.requestDone(channel, requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }
        private void getData() {
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                while(true) {
                    DBData dbData = field.getDBData();
                    boolean more;
                    dbRecord.lock();
                    try {
                        more = channelGetRequestor.nextGetData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(!more) break;
                }
            }
        }
    }
    
    private static class ChannelPutInstance implements ChannelPut,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private ChannelImpl channel;
        private String requestorName;
        private DBRecord dbRecord = null;
        private ChannelPutRequestor channelPutRequestor = null;
        private FieldGroup fieldGroup = null;
        
        private RecordProcess recordProcess = null;
        
        private RequestResult requestResult = null;
        
        private ChannelPutInstance(ChannelImpl channel,DBRecord dbRecord,
        ChannelPutRequestor channelPutRequestor, boolean process) {
            this.channel = channel;
            this.dbRecord = dbRecord;
            this.channelPutRequestor = channelPutRequestor;
            if(process) {
                recordProcess = dbRecord.getRecordProcess();
                boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
                if(!isRequestor) {
                    throw new IllegalStateException("record already has recordProcessRequestor"); 
                }
            }
            requestorName = "ChannelPut:" + channelPutRequestor.getRequestorName();
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#destroy()
         */
        public void destroy() {
            if(isDestroyed) return;
            if(recordProcess!=null) recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#put(org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public void put(ChannelFieldGroup fieldGroup) {
            if(!channel.isConnected()) {
                channelPutRequestor.message(channel,
                    "channel is not connected",MessageType.info);
                channelPutRequestor.requestDone(channel, RequestResult.failure);
            }
            if(fieldGroup==null) {
                throw new IllegalStateException("no field group");
            }
            this.fieldGroup = (FieldGroup)fieldGroup;
            if(recordProcess!=null) {
                recordProcess.setActive(this);
                transferData();
                recordProcess.process(this, false, null);
                return;
            }
            transferData();        
            channelPutRequestor.requestDone(channel, RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            channelPutRequestor.requestDone(channel, requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return requestorName;
        }
        private void transferData() {
            List<ChannelField> list = ((FieldGroup)fieldGroup).getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                while(true) {
                    DBData dbData = field.getDBData();
                    boolean more;
                    dbRecord.lock();
                    try {
                        more = channelPutRequestor.nextPutData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(!more) break;
                }
            }
        }
    }
    
    private static class ChannelPutGetInstance implements ChannelPutGet,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private String requestorName;
        private DBRecord dbRecord = null;
        private ChannelImpl channel = null;
        private ChannelPutGetRequestor channelPutGetRequestor = null;
        private ChannelFieldGroup getFieldGroup = null;
        private ChannelFieldGroup putFieldGroup = null;
        private RecordProcess recordProcess = null;
        private RequestResult requestResult = null;
        
        private ChannelPutGetInstance(ChannelImpl channel,DBRecord dbRecord,
        ChannelPutGetRequestor channelPutGetRequestor,boolean process)
        {
            this.dbRecord = dbRecord;
            this.channel = channel;
            this.channelPutGetRequestor = channelPutGetRequestor;
            requestorName = "ChannelGetPut:" + channelPutGetRequestor.getRequestorName();
            if(process) {
                recordProcess = dbRecord.getRecordProcess();
                boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
                if(!isRequestor) {
                    throw new IllegalStateException("record already has recordProcessRequestor"); 
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#destroy()
         */
        public void destroy() {
            if(isDestroyed) return;
            if(recordProcess!=null)recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#putGet(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public void putGet(ChannelFieldGroup putFieldGroup, ChannelFieldGroup getFieldGroup)
        {
            this.getFieldGroup = getFieldGroup;
            this.putFieldGroup = putFieldGroup;
            requestResult = RequestResult.success;
            if(recordProcess!=null) {
                recordProcess.setActive(this);
                transferPutData();
                recordProcess.process(this, true, null);
                return;
            }
            transferPutData();
            transferGetData();
            channelPutGetRequestor.requestDone(channel, RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            transferGetData();
            recordProcess.setInactive(this);
            channelPutGetRequestor.requestDone(channel, requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return requestorName;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(Channel channel, String message, MessageType messageType) {
            channelPutGetRequestor.message(channel, message, messageType);
        }
        
        private void transferPutData() {
            List<ChannelField> list = ((FieldGroup)putFieldGroup).getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                while(true) {
                    DBData dbData = field.getDBData();
                    boolean more;
                    dbRecord.lock();
                    try {
                        more = channelPutGetRequestor.nextPutData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(!more) break;
                }
            }
        }
        
        private void transferGetData() {
            List<ChannelField> list = getFieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                while(true) {
                    DBData dbData = field.getDBData();
                    boolean more;
                    dbRecord.lock();
                    try {
                        more = channelPutGetRequestor.nextGetData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(!more) break;
                }
            }
        }
    }
    
    private static class Subscribe implements ChannelSubscribe,DBListener
    {
        private static CallbackThread callbackThread = new CallbackThread();
        private Channel channel;
        private boolean isDestroyed = false;
        private RecordListener recordListener = null;
        private FieldGroup fieldGroup = null;
        private ChannelNotifyRequestor notifyListener = null;
        private ChannelNotifyGetRequestor dataListener = null;
        private ChannelNotifyDataQueue notifyDataQueue;
        private ChannelNotifyData notifyData = null;
        private ChannelSubscribeInfo subscribeInfo = new ChannelSubscribeInfo();
        
        
        private static class ChannelSubscribeInfo {
            Channel channel;
            FieldGroup fieldGroup;
            ChannelNotifyRequestor notifyListener = null;
            ChannelNotifyGetRequestor dataListener = null;
            ChannelNotifyDataQueue notifyDataQueue;
        }
              
        private Subscribe(Channel channel,DBRecord dbRecord)
        {
            this.channel = channel;
            recordListener = dbRecord.createListener(this);
            subscribeInfo.channel = channel;
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#destroy()
         */
        public void destroy() {
            isDestroyed = true;
            if(notifyListener!=null) notifyListener.message(channel,"deleted",MessageType.info);
            if(dataListener!=null) dataListener.message(channel,"deleted",MessageType.info);
            if(fieldGroup!=null) stop();
            recordListener = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelNotifyGetRequestor, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup channelFieldGroup,int queueCapacity,
        ChannelNotifyGetRequestor listener, Event why)
        {
            if(!beginCommon(channelFieldGroup)) return;  
            if(queueCapacity<=0) {
                throw new IllegalStateException("queueCapacity must be > 0"); 
            }
            notifyDataQueue = ChannelNotifyDataFactory.createQueue(
                    queueCapacity, channel, fieldGroup);
            notifyListener = null;
            dataListener = listener;
            startCommon(why);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelNotifyRequestor, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup channelFieldGroup, ChannelNotifyRequestor listener, Event why) {
            if(!beginCommon(channelFieldGroup)) return;  
            notifyListener = listener;
            dataListener = null;
            startCommon(why);
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
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#readyForData()
         */
        public void readyForData() {
            callbackThread.add(subscribeInfo);
            notifyData = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#beginSynchronous()
         */
        public void beginSynchronous() {
            if(isDestroyed) return;
            notifyData = notifyDataQueue.getFree();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
            if(isDestroyed||notifyData==null) return;
            dataListener.startNotifyGetData();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData dbData) {
            if(isDestroyed||notifyData==null) return;
           // WHAT TO DO notifyData.add(dbData);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
         */
        public void unlisten(RecordListener listener) {
            if(fieldGroup!=null) stop();
        }
        
        private boolean beginCommon(ChannelFieldGroup channelFieldGroup) {
            if(isDestroyed) return false;
            if(fieldGroup!=null) throw new IllegalStateException("Channel already started");
            fieldGroup = (FieldGroup)channelFieldGroup; 
            return true;
        }
        private void startCommon(Event why) {                      
            subscribeInfo.channel = channel;
            subscribeInfo.dataListener = dataListener;
            subscribeInfo.fieldGroup = fieldGroup;
            subscribeInfo.notifyDataQueue = notifyDataQueue;
            subscribeInfo.notifyListener = notifyListener;
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldInstance field = (ChannelFieldInstance)iter.next();
                DBData dbData = field.getDBData();
                dbData.addListener(this.recordListener);
            }
            
        }
        
        private static class CallbackThread implements Runnable {
            private Thread thread;
            private ReentrantLock lock = new ReentrantLock();
            private Condition moreWork = lock.newCondition();
            private List<ChannelSubscribeInfo> subscribeList =
                new ArrayList<ChannelSubscribeInfo>();
            
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
                        ChannelSubscribeInfo subscribeInfo = null;
                        lock.lock();
                        try {
                            while(subscribeList.isEmpty()) {
                                moreWork.await();
                            }
                            subscribeInfo = subscribeList.get(0);
                        }finally {
                            lock.unlock();
                        }
                        notify(subscribeInfo);
                    }
                } catch(InterruptedException e) {}
            }
            
            private void add(ChannelSubscribeInfo subscribeInfo) {
                lock.lock();
                try {
                    subscribeList.add(subscribeInfo);
                    moreWork.signal();
                } finally {
                    lock.unlock();
                }
            }
            
            private void notify(ChannelSubscribeInfo subscribe) {
                Channel channel = subscribe.channel;
                ChannelNotifyRequestor notifyListener = subscribe.notifyListener;
                if(notifyListener!=null) {
                    notifyListener.dataModified(channel);
                }
                ChannelNotifyGetRequestor dataListener = subscribe.dataListener;
                if(dataListener==null) return;
                FieldGroup fieldGroup = subscribe.fieldGroup;
                ChannelNotifyDataQueue dataQueue = subscribe.notifyDataQueue;
                ChannelNotifyData data = dataQueue.getNext();
                while(data!=null) {
                    List<PVData> dataList = data.getPVDataList();
                    int numberMissed = dataQueue.getNumberMissed();
                    if(numberMissed>0) dataListener.dataOverrun(numberMissed);
                    Iterator<PVData> iter = dataList.iterator();
                    while(iter.hasNext()) {
                        PVData pvData = iter.next();
                        ChannelFieldInstance field = null;
                        List<ChannelField> list = fieldGroup.getList();
                        Iterator<ChannelField> iter1 = list.iterator();
                        
                        while(iter1.hasNext()) {
                            ChannelFieldInstance fieldNow = (ChannelFieldInstance)iter1.next();
                            if(pvData.getField()==fieldNow.getField()) {
                                field = fieldNow;
                                break;
                            }
                        }
                        if(field==null) {
                            throw new IllegalStateException("ChannelAccessLocalFactory logic error");
                        }
                        boolean isLast = !iter.hasNext();
                        dataListener.nextNotifyGetData(channel, field, pvData,isLast);
                    }
                    dataQueue.releaseNext(data);
                    data = dataQueue.getNext();
                }
                    
            }
        }
    }
}
