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
     * Set the IOC database to be used by local channel access.
     * @param iocdb The iocdb.
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
        private LinkedList<ChannelProcessImpl> channelProcessList =
            new LinkedList<ChannelProcessImpl>();
        private LinkedList<ChannelGetImpl> channelGetList =
            new LinkedList<ChannelGetImpl>();
        private LinkedList<ChannelPutImpl> channelPutList =
            new LinkedList<ChannelPutImpl>();
        private LinkedList<ChannelPutGetImpl> channelPutGetList =
            new LinkedList<ChannelPutGetImpl>();
        private LinkedList<ChannelSubscribeImpl> channelSubscribeList = 
            new LinkedList<ChannelSubscribeImpl>();
        
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
                Iterator<ChannelProcessImpl> processIter = channelProcessList.iterator();
                while(processIter.hasNext()) {
                    ChannelProcessImpl channelProcess = processIter.next();
                    channelProcess.destroy();
                    processIter.remove();
                }
                Iterator<ChannelGetImpl> getIter = channelGetList.iterator();
                while(getIter.hasNext()) {
                    ChannelGetImpl channelGet = getIter.next();
                    channelGet.destroy();
                    getIter.remove();
                }
                Iterator<ChannelPutImpl> putIter = channelPutList.iterator();
                while(putIter.hasNext()) {
                    ChannelPutImpl channelPut = putIter.next();
                    channelPut.destroy();
                    putIter.remove();
                }
                Iterator<ChannelPutGetImpl> putGetIter = channelPutGetList.iterator();
                while(putGetIter.hasNext()) {
                    ChannelPutGetImpl channelPutGet = putGetIter.next();
                    channelPutGet.destroy();
                    putGetIter.remove();
                }
                Iterator<ChannelSubscribeImpl> subscribeIter = channelSubscribeList.iterator();
                while(subscribeIter.hasNext()) {
                    ChannelSubscribeImpl channelSubscribe = subscribeIter.next();
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
         * @see org.epics.ioc.channelAccess.Channel#destroy(org.epics.ioc.channelAccess.ChannelGet)
         */
        public void destroy(ChannelGet get) {
            ChannelGetImpl toDelete = (ChannelGetImpl)get;
            lock.lock();
            try {
                Iterator<ChannelGetImpl> getIter = channelGetList.iterator();
                while(getIter.hasNext()) {
                    ChannelGetImpl channelProcess = getIter.next();
                    if(channelProcess==toDelete) {
                        channelProcess.destroy();
                        getIter.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy(org.epics.ioc.channelAccess.ChannelProcess)
         */
        public void destroy(ChannelProcess process) {
            ChannelProcessImpl toDelete = (ChannelProcessImpl)process;
            lock.lock();
            try {
                Iterator<ChannelProcessImpl> processIter = channelProcessList.iterator();
                while(processIter.hasNext()) {
                    ChannelProcessImpl channelProcess = processIter.next();
                    if(channelProcess==toDelete) {
                        channelProcess.destroy();
                        processIter.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy(org.epics.ioc.channelAccess.ChannelPut)
         */
        public void destroy(ChannelPut put) {
            ChannelPutImpl toDelete = (ChannelPutImpl)put;
            lock.lock();
            try {
                Iterator<ChannelPutImpl> putIter = channelPutList.iterator();
                while(putIter.hasNext()) {
                    ChannelPutImpl channelPut = putIter.next();
                    if(channelPut==toDelete) {
                        channelPut.destroy();
                        putIter.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy(org.epics.ioc.channelAccess.ChannelPutGet)
         */
        public void destroy(ChannelPutGet putGet) {
            ChannelPutGetImpl toDelete = (ChannelPutGetImpl)putGet;
            lock.lock();
            try {
                Iterator<ChannelPutGetImpl> putGetIter = channelPutGetList.iterator();
                while(putGetIter.hasNext()) {
                    ChannelPutGetImpl channelPutGet = putGetIter.next();
                    if(channelPutGet==toDelete) {
                        channelPutGet.destroy();
                        putGetIter.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.Channel#destroy(org.epics.ioc.channelAccess.ChannelSubscribe)
         */
        public void destroy(ChannelSubscribe subscribe) {
            ChannelSubscribeImpl toDelete = (ChannelSubscribeImpl)subscribe;
            lock.lock();
            try {
                Iterator<ChannelSubscribeImpl> subscribeIter = channelSubscribeList.iterator();
                while(subscribeIter.hasNext()) {
                    ChannelSubscribeImpl channelSubscribe = subscribeIter.next();
                    if(channelSubscribe==toDelete) {
                        channelSubscribe.destroy();
                        subscribeIter.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
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
                    return new ChannelFieldImpl(currentData);
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
                    ChannelProcessImpl channelProcess;
                    try {
                        channelProcess = new ChannelProcessImpl(
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
                    ChannelGetImpl channelGet;
                    try {
                        channelGet = new ChannelGetImpl(
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
                    ChannelPutImpl channelPut = new ChannelPutImpl(
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
                    ChannelPutGetImpl channelPutGet =  new ChannelPutGetImpl(
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
                    ChannelSubscribeImpl subscribe =  new ChannelSubscribeImpl(this,dbAccess.getDbRecord());
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
    
    private static class ChannelFieldImpl implements ChannelField {
        private DBData dbData;
        
        private ChannelFieldImpl(DBData dbData) {
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
    
    private static class ChannelProcessImpl implements ChannelProcess,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private Channel channel;
        private String requestorName;
        private ChannelRequestor channelRequestor = null;
        private RecordProcess recordProcess = null;
        
        private RequestResult requestResult = null;
             
        private ChannelProcessImpl(
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
        
        private void destroy() {
            if(isDestroyed) return;
            recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcess#process()
         */
        public boolean process() {
            if(!channel.isConnected()) {
                channelRequestor.message(channel,
                    "channel is not connected",MessageType.info);
                return false;
            }
            return recordProcess.process(this, false, null);
            
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
            channelRequestor.message(message, messageType);
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
    
    private static class ChannelGetImpl implements ChannelGet,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private ChannelImpl channel;
        private String requestorName;
        private DBRecord dbRecord = null;
        private ChannelGetRequestor channelGetRequestor = null;
        private FieldGroup fieldGroup = null;
        private RequestResult requestResult = RequestResult.success;
        
        private RecordProcess recordProcess = null;
        private List<ChannelField> channelFieldList;
        private Iterator<ChannelField> channelFieldListIter;
        private DBData dbData;
        
        private ChannelGetImpl(ChannelImpl channel,DBRecord dbRecord,
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
        
        private void destroy() {
            if(isDestroyed) return;
            if(recordProcess!=null) recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#get(org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public boolean get(ChannelFieldGroup fieldGroup) {
            if(!channel.isConnected()) {
                channelGetRequestor.message(channel,
                    "channel is not connected",MessageType.info);
                return false;
            }
            if(fieldGroup==null) {
                throw new IllegalStateException("no field group");
            }
            this.fieldGroup = (FieldGroup)fieldGroup;
            if(recordProcess!=null) {
                return recordProcess.process(this, true, null);
            }
            channelFieldList = fieldGroup.getList();
            startGetData();
            channelGetRequestor.requestDone(channel, RequestResult.success);
            return true;
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGet#getDelayed(org.epics.ioc.pvAccess.PVData)
         */
        public void getDelayed(PVData pvData) {
            if(pvData!=this.dbData) {
                throw new IllegalStateException("pvData is not correct"); 
            }
            getData();
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
            channelGetRequestor.message(channel,message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            startGetData();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }
        
        private void startGetData() {
            channelFieldList = fieldGroup.getList();
            channelFieldListIter = channelFieldList.iterator();
            dbData = null;
            getData();
        }
        private void getData() {
            boolean more;
            while(true) {
                if(dbData==null) {
                    if(!channelFieldListIter.hasNext()) {
                        if(recordProcess!=null) recordProcess.setInactive(this);
                        channelGetRequestor.requestDone(channel, requestResult);
                        return;
                    }
                    ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                    DBData dbData = field.getDBData();
                    dbRecord.lock();
                    try {
                        more = channelGetRequestor.nextGetData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                } else {
                    dbRecord.lock();
                    try {
                        more = channelGetRequestor.nextDelayedGetData(dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                }
            }
        }
    }
    
    private static class ChannelPutImpl implements ChannelPut,RecordProcessRequestor
    {
        private boolean isDestroyed = false;
        private ChannelImpl channel;
        private String requestorName;
        private DBRecord dbRecord = null;
        private ChannelPutRequestor channelPutRequestor = null;
        private FieldGroup fieldGroup = null;
        
        private RecordProcess recordProcess = null;
        
        private RequestResult requestResult = null;
        
        private List<ChannelField> channelFieldList;
        private Iterator<ChannelField> channelFieldListIter;
        private DBData dbData;
        
        private ChannelPutImpl(ChannelImpl channel,DBRecord dbRecord,
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
        
        private void destroy() {
            if(isDestroyed) return;
            if(recordProcess!=null) recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#put(org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public boolean put(ChannelFieldGroup fieldGroup) {
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
                if(!recordProcess.setActive(this)) return false;
                startPutData();
                recordProcess.process(this, false, null);
                return true;
            }
            startPutData();
            return true;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPut#putDelayed(org.epics.ioc.pvAccess.PVData)
         */
        public void putDelayed(PVData pvData) {
            if(pvData!=this.dbData) {
                throw new IllegalStateException("pvData is not correct"); 
            }
            putData();
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
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            channelPutRequestor.message(channel,message, messageType);
        }
        private void startPutData() {
            channelFieldList = fieldGroup.getList();
            channelFieldListIter = channelFieldList.iterator();
            dbData = null;
            putData();
        }
        private void putData() {
            boolean more;
            while(true) {
                if(dbData==null) {
                    if(!channelFieldListIter.hasNext()) {
                        if(recordProcess!=null) {
                            recordProcess.process(this, false, null);
                        } else {
                            channelPutRequestor.requestDone(channel, requestResult);
                        }
                        return;
                    }
                    ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                    DBData dbData = field.getDBData();
                    dbRecord.lock();
                    try {
                        more = channelPutRequestor.nextPutData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                } else {
                    dbRecord.lock();
                    try {
                        more = channelPutRequestor.nextDelayedPutData(dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                }
            }
        }
    }
    
    private static class ChannelPutGetImpl implements ChannelPutGet,RecordProcessRequestor
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
        
        private List<ChannelField> channelFieldList;
        private Iterator<ChannelField> channelFieldListIter;
        private DBData dbData;
        
        
        private ChannelPutGetImpl(ChannelImpl channel,DBRecord dbRecord,
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
        
        private void destroy() {
            if(isDestroyed) return;
            if(recordProcess!=null)recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#putGet(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelFieldGroup)
         */
        public boolean putGet(ChannelFieldGroup putFieldGroup, ChannelFieldGroup getFieldGroup)
        {
            if(isDestroyed) return false;
            this.getFieldGroup = getFieldGroup;
            this.putFieldGroup = putFieldGroup;
            requestResult = RequestResult.success;
            if(recordProcess!=null) {
                boolean result = recordProcess.setActive(this);
                if(result==false) return result;
            }
            startPutData();
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#getDelayed(org.epics.ioc.pvAccess.PVData)
         */
        public void getDelayed(PVData pvData) {
            getData();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutGet#putDelayed(org.epics.ioc.pvAccess.PVData)
         */
        public void putDelayed(PVData pvData) {
            putData();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            startGetData();
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
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            channelPutGetRequestor.message(channel,message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(Channel channel, String message, MessageType messageType) {
            channelPutGetRequestor.message(channel, message, messageType);
        }
        private void startPutData() {
            channelFieldList = putFieldGroup.getList();
            channelFieldListIter = channelFieldList.iterator();
            dbData = null;
            putData();
        }
        private void putData() {
            boolean more;
            while(true) {
                if(dbData==null) {
                    if(!channelFieldListIter.hasNext()) {
                        if(recordProcess!=null) {
                            recordProcess.process(this, true, null);
                        } else {
                            startGetData();
                        }
                        return;
                    }
                    ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                    DBData dbData = field.getDBData();
                    dbRecord.lock();
                    try {
                        more = channelPutGetRequestor.nextPutData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                } else {
                    dbRecord.lock();
                    try {
                        more = channelPutGetRequestor.nextDelayedPutData(dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                }
            }
        }
        
        private void startGetData() {
            channelFieldList = getFieldGroup.getList();
            channelFieldListIter = channelFieldList.iterator();
            dbData = null;
            getData();
        }
        private void getData() {
            boolean more;
            while(true) {
                if(dbData==null) {
                    if(!channelFieldListIter.hasNext()) {
                        if(recordProcess!=null) recordProcess.setInactive(this);
                        channelPutGetRequestor.requestDone(channel, requestResult);
                        return;
                    }
                    ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                    DBData dbData = field.getDBData();
                    dbRecord.lock();
                    try {
                        more = channelPutGetRequestor.nextGetData(channel,field,dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                } else {
                    dbRecord.lock();
                    try {
                        more = channelPutGetRequestor.nextDelayedGetData(dbData);
                    } finally {
                        dbRecord.unlock();
                    }
                    if(more) return;
                    dbData = null;
                }
            }
        }
        private void transferGetData() {
            List<ChannelField> list = getFieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldImpl field = (ChannelFieldImpl)iter.next();
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
    
    private static class ChannelSubscribeImpl implements ChannelSubscribe,DBListener
    {
        private static CallbackThread callbackThread = new CallbackThread();
        private Channel channel;
        private DBRecord dbRecord;
        private boolean isDestroyed = false;
        private RecordListener recordListener = null;
        private FieldGroup fieldGroup = null;
        private ChannelSubscribeRequestor subscribeRequestor = null;
        private ChannelSubscribeGetRequestor subscribeGetListener = null;
        private ChannelDataQueue channelDataQueue;
        private ChannelData channelData = null;
        private ChannelSubscribeInfo subscribeInfo = new ChannelSubscribeInfo();
        
        
        private static class ChannelSubscribeInfo {
            Channel channel;
            FieldGroup fieldGroup;
            ChannelSubscribeRequestor subscribeRequestor = null;
            ChannelSubscribeGetRequestor subscribeGetRequestor = null;
            ChannelDataQueue channelDataQueue;
        }
              
        private ChannelSubscribeImpl(Channel channel,DBRecord dbRecord)
        {
            this.channel = channel;
            this.dbRecord = dbRecord;
            recordListener = dbRecord.createListener(this);
            subscribeInfo.channel = channel;
        }
        
        private void destroy() {
            isDestroyed = true;
            if(subscribeRequestor!=null) subscribeRequestor.message(channel,"deleted",MessageType.info);
            if(subscribeGetListener!=null) subscribeGetListener.message(channel,"deleted",MessageType.info);
            if(fieldGroup!=null) stop();
            recordListener = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelSubscribeGetRequestor, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup channelFieldGroup,int queueCapacity,
        ChannelSubscribeGetRequestor listener, Event why)
        {
            if(!beginCommon(channelFieldGroup)) return;  
            if(queueCapacity<=0) {
                throw new IllegalStateException("queueCapacity must be > 0"); 
            }
            channelDataQueue = ChannelDataFactory.createQueue(
                    queueCapacity, channel, fieldGroup);
            subscribeRequestor = null;
            subscribeGetListener = listener;
            startCommon(why);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#start(org.epics.ioc.channelAccess.ChannelFieldGroup, org.epics.ioc.channelAccess.ChannelSubscribeRequestor, org.epics.ioc.channelAccess.Event)
         */
        public void start(ChannelFieldGroup channelFieldGroup, ChannelSubscribeRequestor listener, Event why) {
            if(!beginCommon(channelFieldGroup)) return;  
            subscribeRequestor = listener;
            subscribeGetListener = null;
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
                ChannelFieldImpl field = (ChannelFieldImpl)iter.next();
                DBData dbData = field.getDBData();
                dbData.removeListener(this.recordListener);
            }
            fieldGroup = null;
            subscribeGetListener = null;
            subscribeRequestor = null;
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribe#readyForData()
         */
        public void readyForData() {
            callbackThread.add(subscribeInfo);
            channelData = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#beginSynchronous()
         */
        public void beginSynchronous() {
dbRecord.message("beginSynchonous", MessageType.info);
            if(isDestroyed) return;
            if(subscribeGetListener==null) return;
            channelData = channelDataQueue.getFree();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
dbRecord.message("endSynchonous", MessageType.info);
           if(isDestroyed) return;
           callbackThread.add(subscribeInfo);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData data) {
dbRecord.message("endSynchonous", MessageType.info);
            if(isDestroyed||channelData==null) return;
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldImpl field = (ChannelFieldImpl)iter.next();
                DBData dbData = field.getDBData();
                if(data==dbData) {
                    
                }
            }
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
            subscribeInfo.subscribeGetRequestor = subscribeGetListener;
            subscribeInfo.fieldGroup = fieldGroup;
            subscribeInfo.channelDataQueue = channelDataQueue;
            subscribeInfo.subscribeRequestor = subscribeRequestor;
            List<ChannelField> list = fieldGroup.getList();
            Iterator<ChannelField> iter = list.iterator();
            while(iter.hasNext()) {
                ChannelFieldImpl field = (ChannelFieldImpl)iter.next();
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
                ChannelSubscribeRequestor subscribeListener = subscribe.subscribeRequestor;
                if(subscribeListener!=null) {
                    subscribeListener.dataModified(channel);
                }
                ChannelSubscribeGetRequestor dataListener = subscribe.subscribeGetRequestor;
                if(dataListener==null) return;
                FieldGroup fieldGroup = subscribe.fieldGroup;
                ChannelDataQueue dataQueue = subscribe.channelDataQueue;
                ChannelData data = dataQueue.getNext();
                while(data!=null) {
                    List<PVData> dataList = data.getPVDataList();
                    int numberMissed = dataQueue.getNumberMissed();
                    if(numberMissed>0) dataListener.dataOverrun(numberMissed);
                    Iterator<PVData> iter = dataList.iterator();
                    while(iter.hasNext()) {
                        PVData pvData = iter.next();
                        ChannelFieldImpl field = null;
                        List<ChannelField> list = fieldGroup.getList();
                        Iterator<ChannelField> iter1 = list.iterator();
                        
                        while(iter1.hasNext()) {
                            ChannelFieldImpl fieldNow = (ChannelFieldImpl)iter1.next();
                            if(pvData.getField()==fieldNow.getField()) {
                                field = fieldNow;
                                break;
                            }
                        }
                        if(field==null) {
                            throw new IllegalStateException("ChannelAccessLocalFactory logic error");
                        }
                        dataListener.nextSubscribeGetData(channel, field, pvData);
                    }
                    dataQueue.releaseNext(data);
                    data = dataQueue.getNext();
                }
                    
            }
        }
    }
}
