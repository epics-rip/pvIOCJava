/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

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
         * @see org.epics.ioc.ca.ChannelAccess#createChannel(java.lang.String, org.epics.ioc.ca.ChannelStateListener)
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
        private DBRecord dbRecord;
        private DBAccess dbAccess;
        private DBData currentData = null;
        private String otherChannel = null;
        private String otherField = null;
        private LinkedList<FieldGroupImpl> fieldGroupList = 
            new LinkedList<FieldGroupImpl>();
        private LinkedList<ChannelProcessImpl> channelProcessList =
            new LinkedList<ChannelProcessImpl>();
        private LinkedList<ChannelGetImpl> channelGetList =
            new LinkedList<ChannelGetImpl>();
        private LinkedList<ChannelPutImpl> channelPutList =
            new LinkedList<ChannelPutImpl>();
        private LinkedList<ChannelPutGetImpl> channelPutGetList =
            new LinkedList<ChannelPutGetImpl>();
        private LinkedList<ChannelMonitorImpl> monitorList = 
            new LinkedList<ChannelMonitorImpl>();
        
        private ChannelImpl(DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbRecord = record;
            dbAccess = record.getIOCDB().createAccess(record.getRecordName());
            if(dbAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy()
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
                Iterator<ChannelMonitorImpl> monitorIter = monitorList.iterator();
                while(monitorIter.hasNext()) {
                    ChannelMonitorImpl impl = monitorIter.next();
                    impl.destroy();
                    monitorIter.remove();
                }
                isDestroyed = true;
            } finally {
                lock.unlock();
            }
            stateListener.disconnect(this);
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelGet)
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
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelProcess)
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
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelPut)
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
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelPutGet)
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
         * @see org.epics.ioc.ca.ChannelMonitor#destroy(org.epics.ioc.ca.ChannelMonitor)
         */
        public void destroy(ChannelMonitor channelMonitor) {
            ChannelMonitorImpl toDelete = (ChannelMonitorImpl)channelMonitor;
            lock.lock();
            try {
                Iterator<ChannelMonitorImpl> iter = monitorList.iterator();
                while(iter.hasNext()) {
                    ChannelMonitorImpl impl = iter.next();
                    if(impl==toDelete) {
                        impl.destroy();
                        iter.remove();
                        return;
                    }
                }
            } finally {
                lock.unlock();
            }
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#isConnected()
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
         * @see org.epics.ioc.ca.Channel#setField(java.lang.String)
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
         * @see org.epics.ioc.ca.Channel#getOtherChannel()
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
         * @see org.epics.ioc.ca.Channel#getOtherField()
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
         * @see org.epics.ioc.ca.Channel#getChannelField()
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
         * @see org.epics.ioc.ca.Channel#createFieldGroup(org.epics.ioc.ca.ChannelFieldGroupListener)
         */
        public FieldGroupImpl createFieldGroup(ChannelFieldGroupListener listener) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    FieldGroupImpl fieldGroupImpl = new FieldGroupImpl(listener);
                    fieldGroupList.add(fieldGroupImpl);
                    return fieldGroupImpl;
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelProcess()
         */
        public ChannelProcess createChannelProcess(ChannelProcessRequestor channelProcessRequestor) {
            lock.lock();
            try {
                if(isDestroyed) {
                    channelProcessRequestor.message(
                        "channel has been destroyed",MessageType.fatalError);
                    return null;
                } else {
                    ChannelProcessImpl channelProcess;
                    try {
                        channelProcess = new ChannelProcessImpl(channelProcessRequestor);
                        channelProcessList.add(channelProcess);
                    } catch(IllegalStateException e) {
                        channelProcessRequestor.message(
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
         * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelGetRequestor, org.epics.ioc.ca.ChannelProcessRequestor)
         */
        public ChannelGet createChannelGet(ChannelGetRequestor channelGetRequestor,boolean process) {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelGetImpl channelGet;
                    try {
                        channelGet = new ChannelGetImpl(channelGetRequestor,process);
                        channelGetList.add(channelGet);
                    } catch(IllegalStateException e) {
                        channelGetRequestor.message(
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
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelPutRequestor, org.epics.ioc.ca.ChannelProcessRequestor)
         */
        public ChannelPut createChannelPut(ChannelPutRequestor channelPutRequestor,boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutImpl channelPut = new ChannelPutImpl(channelPutRequestor,process);
                    channelPutList.add(channelPut);
                    return channelPut;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelPutGetRequestor, org.epics.ioc.ca.ChannelProcessRequestor)
         */
        public ChannelPutGet createChannelPutGet(ChannelPutGetRequestor channelPutGetRequestor,boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutGetImpl channelPutGet =  new ChannelPutGetImpl(channelPutGetRequestor,process);
                    channelPutGetList.add(channelPutGet);
                    return channelPutGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequestor, boolean)
         */
        public ChannelMonitor createChannelMonitor(
            boolean onlyWhileProcessing)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    stateListener.message(
                        "channel has been destroyed",MessageType.fatalError);
                    return null;
                } else {
                    ChannelMonitorImpl impl = 
                        new ChannelMonitorImpl(this,onlyWhileProcessing);
                    monitorList.add(impl);
                    return impl;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#isLocal()
         */
        public boolean isLocal() {
            return true;
        }

    
        private static class ChannelFieldImpl implements ChannelField {
            private PVData pvData;
            
            private ChannelFieldImpl(PVData dbData) {
                this.pvData = dbData;
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getAccessRights()
             */
            public AccessRights getAccessRights() {
                // OK until access security is implemented
                if(pvData.getField().isMutable()) {
                    return AccessRights.readWrite;
                } else {
                    return AccessRights.read;
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getField()
             */
            public Field getField() {
                return pvData.getField();
            }       
            private PVData getPVData() {
                return pvData;
            }
    
        }
        
        private class FieldGroupImpl implements ChannelFieldGroup {
            private LinkedList<ChannelField> fieldList = 
                new LinkedList<ChannelField>();
    
            private FieldGroupImpl(ChannelFieldGroupListener listener) {}
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#addChannelField(org.epics.ioc.ca.ChannelField)
             */
            public void addChannelField(ChannelField channelField) {
                if(isDestroyed) return;
                fieldList.add(channelField);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#removeChannelField(org.epics.ioc.ca.ChannelField)
             */
            public void removeChannelField(ChannelField channelField) {
                if(isDestroyed) return;
                fieldList.remove(channelField);
            }
            
            public List<ChannelField> getList() {
                return fieldList;
            }
        }
        
        private class ChannelProcessImpl implements ChannelProcess,RecordProcessRequestor
        {
            private String requestorName;
            private ChannelProcessRequestor channelProcessRequestor = null;
            private RecordProcess recordProcess = null;
            
            private RequestResult requestResult = null;
                 
            private ChannelProcessImpl(ChannelProcessRequestor channelRequestor)
            {
                this.channelProcessRequestor = channelRequestor;
                recordProcess = dbRecord.getRecordProcess();
                boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
                if(!isRequestor) {
                    throw new IllegalStateException("record already has recordProcessRequestor"); 
                }
                requestorName = "ChannelProcess:" + channelRequestor.getRequestorName();
            }
            
            private void destroy() {
                recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcess#process()
             */
            public boolean process() {
                if(!isConnected()) {
                    channelProcessRequestor.message(
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
                channelProcessRequestor.message(message, messageType);
            }
    
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#processResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.process.RequestResult)
             */
            public void recordProcessComplete() {
                channelProcessRequestor.processDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#ready()
             */
            public RequestResult ready() {
                throw new IllegalStateException("Logic error. Why was this called?");
            }
        }
        
        private class ChannelGetImpl implements ChannelGet,RecordProcessRequestor
        {
            private String requestorName;
            private ChannelGetRequestor channelGetRequestor = null;
            private FieldGroupImpl fieldGroup = null;
            private RequestResult requestResult = RequestResult.success;
            
            private RecordProcess recordProcess = null;
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVData pvData;
            
            private ChannelGetImpl(ChannelGetRequestor channelGetRequestor,boolean process) {
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
                if(recordProcess!=null) recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#get(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public boolean get(ChannelFieldGroup channelFieldGroup) {
                if(!isConnected()) {
                    channelGetRequestor.message(
                        "channel is not connected",MessageType.info);
                    return false;
                }
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                requestResult = RequestResult.success;
                if(recordProcess!=null) {
                    return recordProcess.process(this, true, null);
                }
                channelFieldList = channelFieldGroup.getList();
                startGetData();
                return true;
            }                
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#getDelayed(org.epics.ioc.pvAccess.PVData)
             */
            public void getDelayed(PVData pvData) {
                if(pvData!=this.pvData) {
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
                channelGetRequestor.message(message, messageType);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
             */
            public void recordProcessComplete() {
                startGetData();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            
            private void startGetData() {
                channelFieldList = fieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvData = null;
                getData();
            }
            private void getData() {
                boolean more;
                while(true) {
                    if(pvData==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(recordProcess!=null) recordProcess.setInactive(this);
                            channelGetRequestor.getDone(requestResult);
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvData = field.getPVData();
                        dbRecord.lock();
                        try {
                            more = channelGetRequestor.nextGetData(field,pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelGetRequestor.nextDelayedGetData(pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    }
                }
            }
        }
        
        private class ChannelPutImpl implements ChannelPut,RecordProcessRequestor
        {
            private String requestorName;
            private ChannelPutRequestor channelPutRequestor = null;
            private FieldGroupImpl fieldGroup = null;
            
            private RecordProcess recordProcess = null;
            
            private RequestResult requestResult = null;
            
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVData pvData;
            
            private ChannelPutImpl(ChannelPutRequestor channelPutRequestor, boolean process) {
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
                if(recordProcess!=null) recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public boolean put(ChannelFieldGroup channelFieldGroup) {
                if(isDestroyed) return false;
                if(!isConnected()) {
                    channelPutRequestor.message(
                        "channel is not connected",MessageType.info);
                    return false;
                }
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
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
             * @see org.epics.ioc.ca.ChannelPut#putDelayed(org.epics.ioc.pvAccess.PVData)
             */
            public void putDelayed(PVData pvData) {
                if(pvData!=this.pvData) {
                    throw new IllegalStateException("pvData is not correct"); 
                }
                putData();
            }
    
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
             */
            public void recordProcessComplete() {
                channelPutRequestor.putDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
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
                channelPutRequestor.message(message, messageType);
            }
            private void startPutData() {
                channelFieldList = fieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvData = null;
                putData();
            }
            private void putData() {
                boolean more;
                while(true) {
                    if(pvData==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(recordProcess!=null) {
                                recordProcess.process(this, false, null);
                            } else {
                                channelPutRequestor.putDone(requestResult);
                            }
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvData = field.getPVData();
                        dbRecord.lock();
                        try {
                            more = channelPutRequestor.nextPutData(field,pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutRequestor.nextDelayedPutData(pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    }
                }
            }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,RecordProcessRequestor
        {
            private String requestorName;
            private ChannelPutGetRequestor channelPutGetRequestor = null;
            private ChannelFieldGroup getFieldGroup = null;
            private ChannelFieldGroup putFieldGroup = null;
            private RecordProcess recordProcess = null;
            private RequestResult requestResult = null;
            
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVData pvData;
            
            
            private ChannelPutGetImpl(ChannelPutGetRequestor channelPutGetRequestor,boolean process)
            {
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
                if(recordProcess!=null)recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup)
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
             * @see org.epics.ioc.ca.ChannelPutGet#getDelayed(org.epics.ioc.pvAccess.PVData)
             */
            public void getDelayed(PVData pvData) {
                getData();
            }
    
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putDelayed(org.epics.ioc.pvAccess.PVData)
             */
            public void putDelayed(PVData pvData) {
                putData();
            }
    
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
             */
            public void recordProcessComplete() {
                startGetData();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
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
                channelPutGetRequestor.message(message, messageType);
            }
            
            private void startPutData() {
                channelFieldList = putFieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvData = null;
                putData();
            }
            private void putData() {
                boolean more;
                while(true) {
                    if(pvData==null) {
                        if(!channelFieldListIter.hasNext()) {
                            channelPutGetRequestor.putDone(RequestResult.success);
                            if(recordProcess!=null) {
                                recordProcess.process(this, true, null);
                            } else {
                                startGetData();
                            }
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvData = field.getPVData();
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextPutData(field,pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextDelayedPutData(pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    }
                }
                
            }
            
            private void startGetData() {
                channelFieldList = getFieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvData = null;
                getData();
            }
            private void getData() {
                boolean more;
                while(true) {
                    if(pvData==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(recordProcess!=null) recordProcess.setInactive(this);
                            channelPutGetRequestor.getDone(requestResult);
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvData = field.getPVData();
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextGetData(field,pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextDelayedGetData(pvData);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvData = null;
                    }
                }
            }
        }
        
        
              
        private class ChannelMonitorImpl implements
        ChannelFieldGroupListener,ChannelMonitor,DBListener
        {
            private Channel channel;
            boolean onlyWhileProcesing;
            private Monitor monitor = null;
            private boolean isActive = false;
            private RecordListener recordListener = null;
            private boolean processActive = false;
            
            private ChannelMonitorNotifyRequestor channelMonitorNotifyRequestor;
            
            private boolean firstNotify = false;
            private ChannelMonitorRequestor channelMonitorRequestor;
            private ChannelFieldGroup channelFieldGroup = null;
            private ChannelDataQueue channelDataQueue = null;
            private MonitorThread monitorThread;
            private ChannelData channelData = null;
            
            private ChannelMonitorImpl(Channel channel,boolean onlyWhileProcesing)
            {
                this.channel = channel;
                this.onlyWhileProcesing = onlyWhileProcesing;
                this.channelFieldGroup = channel.createFieldGroup(this);
                monitor = new Monitor(channelMonitorNotifyRequestor);
            }
            
            private void destroy() {
                stop();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // nothing to do for now
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#lookForAbsoluteChange(org.epics.ioc.ca.ChannelField, double)
             */
            public void lookForAbsoluteChange(ChannelField channelField, double value) {
                lock.lock();
                try {
                    if(isDestroyed) {
                        channelMonitorNotifyRequestor.message(
                            "channel has been destroyed",MessageType.fatalError);
                    } else if(isActive) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        ChannelFieldImpl impl = (ChannelFieldImpl)channelField;
                        monitor.onAbsoluteChange(impl, value);
                    }
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#lookForChange(org.epics.ioc.ca.ChannelField)
             */
            public void lookForChange(ChannelField channelField, boolean causeMonitor) {
                lock.lock();
                try {
                    if(isDestroyed) {
                        channelMonitorNotifyRequestor.message(
                            "channel has been destroyed",MessageType.fatalError);
                    } else if(isActive) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        if(isDestroyed) return;
                        ChannelFieldImpl impl = (ChannelFieldImpl)channelField;
                        monitor.onPut(impl,causeMonitor);
                    }
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#lookForPercentageChange(org.epics.ioc.ca.ChannelField, double)
             */
            public void lookForPercentageChange(ChannelField channelField, double value) {
                lock.lock();
                try {
                    if(isDestroyed) {
                        channelMonitorNotifyRequestor.message(
                            "channel has been destroyed",MessageType.fatalError);
                    } else if(isActive) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        if(isDestroyed) return;
                        ChannelFieldImpl impl = (ChannelFieldImpl)channelField;
                        monitor.onPercentageChange(impl,value);
                    }
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#start()
             */
            public boolean start(ChannelMonitorNotifyRequestor channelMonitorNotifyRequestor,
                String threadName, ScanPriority scanPriority)
            {
                lock.lock();
                try {
                    if(isDestroyed) {
                        channelMonitorNotifyRequestor.message(
                            "channel has been destroyed",MessageType.fatalError);
                        return false;
                    } else if(isActive) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        this.channelMonitorNotifyRequestor = channelMonitorNotifyRequestor;
                        channelMonitorRequestor = null;
                        monitor.start();
                        recordListener = dbRecord.createListener(this);
                        List<ChannelFieldImpl> channelFieldList = monitor.getChannelFieldList();
                        if(threadName==null) threadName =
                            channelMonitorNotifyRequestor.getRequestorName() + "NotifyThread";
                        int priority = scanPriority.getJavaPriority();
                        monitorThread = new MonitorThread(
                             threadName,priority,channelMonitorNotifyRequestor);
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBData dbData = (DBData)channelField.getPVData();
                            dbData.addListener(recordListener);
                        }
                        isActive = true;
                        processActive = false;
                    }
                } finally {
                    lock.unlock();
                }
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#start(org.epics.ioc.ca.ChannelMonitorRequestor)
             */
            public boolean start(ChannelMonitorRequestor channelMonitorRequestor, int queueSize, String threadName, ScanPriority scanPriority) {
                lock.lock();
                try {
                    if(isDestroyed) {
                        channelMonitorRequestor.message(
                            "channel has been destroyed",MessageType.fatalError);
                        return false;
                    } else if(isActive) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        firstNotify = true;
                        this.channelMonitorRequestor = channelMonitorRequestor;
                        channelMonitorNotifyRequestor = null;
                        channelFieldGroup = channel.createFieldGroup(this);
                        List<ChannelFieldImpl> channelFieldList = monitor.getChannelFieldList();
                        for(ChannelField channelField: channelFieldList) {
                            channelFieldGroup.addChannelField(channelField);
                        }
                        channelDataQueue = ChannelDataFactory.createQueue(
                            queueSize, channel, channelFieldGroup);
                        if(threadName==null) threadName =
                            channelMonitorRequestor.getRequestorName() + "NotifyThread";
                        int priority = scanPriority.getJavaPriority();
                        monitorThread = new MonitorThread(
                            threadName,priority,channelMonitorRequestor,channelDataQueue);
                        monitor.start();
                        recordListener = dbRecord.createListener(this);
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBData dbData = (DBData)channelField.getPVData();
                            dbData.addListener(recordListener);
                        }
                        isActive = true;
                        processActive = false;
                    }
                } finally {
                    lock.unlock();
                }
                return true;
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#stop()
             */
            public void stop() {
                lock.lock();
                try {
                    if(!isActive) return;
                    isActive = false;
                } finally {
                    lock.unlock();
                }
                dbRecord.removeListener(recordListener);
                recordListener = null;
                if(channelMonitorRequestor!=null) {
                    channelDataQueue = null;
                    channelData = null;
                    monitorThread.stop();
                }
                channelMonitorNotifyRequestor = null;
                channelMonitorRequestor = null;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginProcess()
             */
            public void beginProcess() {
                if(!isActive) return;
                processActive = true;
                if(channelMonitorRequestor!=null && channelData==null) {
                    channelData = channelDataQueue.getFree(true);
                }
            }
 
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endProcess()
             */
            public void endProcess() {
                if(!isActive) return;
                notifyRequestor();
                processActive = false;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.pv.PVStructure)
             */
            public void beginPut(PVStructure pvStructure) {
                // nothing to do
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.pv.PVStructure)
             */
            public void endPut(PVStructure pvStructure) {
                if(!isActive) return;
                if(onlyWhileProcesing && !processActive) return;
                ChannelField channelField = monitor.newData(pvStructure);
                if(channelField==null) return;                
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) channelData.add(pvStructure);
                }                    
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#newData(org.epics.ioc.db.PVData)
             */
            public void newData(PVStructure pvStructure,DBData dbData) {
                if(!isActive) return;
                if(onlyWhileProcesing && !processActive) return;
                ChannelField channelField = monitor.newData(dbData);
                if(channelField==null) return;                
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) channelData.add(dbData);
                }                    
                if(!processActive) notifyRequestor();
            }
           
            
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.pv.PVLink)
             */
            public void configurationStructurePut(PVLink pvLink) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBData)
             */
            public void dataPut(DBData dbData) {
                if(!isActive) return;
                if(onlyWhileProcesing && !processActive) return;
                ChannelField channelField = monitor.newData(dbData);
                if(channelField==null) return;                
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) channelData.add(dbData);
                }                    
                if(!processActive) notifyRequestor();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.pv.PVEnum)
             */
            public void enumChoicesPut(PVEnum pvEnum) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.pv.PVEnum)
             */
            public void enumIndexPut(PVEnum pvEnum) {
                // TODO Auto-generated method stub
                
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#structurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.db.DBData)
             */
            public void structurePut(PVStructure pvStructure, DBData dbData) {
                if(!isActive) return;
                if(onlyWhileProcesing && !processActive) return;
                ChannelField channelField = monitor.newData(dbData);
                if(channelField==null) return;                
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) channelData.add(dbData);
                }                    
                if(!processActive) notifyRequestor();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBData)
             */
            public void supportNamePut(DBData dbData) {
                // TODO Auto-generated method stub
                
            }

            private void notifyRequestor() {
                boolean monitorOccured = false;
                List<MonitorField> list = monitor.getMonitorFieldList();
                for(MonitorField field : list) {
                    if(field.monitorOccured()) {
                        field.clearMonitor();
                        if(field.causeMonitor()) monitorOccured = true;
                    }
                }
                if(!monitorOccured) return;
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) {
                        if(firstNotify) {
                            firstNotify = false;
                            List<ChannelFieldImpl> channelFieldList
                                = monitor.getChannelFieldList();
                            for(ChannelFieldImpl channelField : channelFieldList) {
                                channelData.add(channelField.getPVData());
                            }
                        }
                        monitorThread.signal();
                        channelData = null;
                    }
                } else {
                    monitorThread.signal();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
             */
            public void unlisten(RecordListener listener) {
                stop();
                channel.destroy();
            }
        }
        
        private  enum MonitorType {
            onPut,
            absoluteChange,
            percentageChange
        }
        
        private static class MonitorField {
            private MonitorType monitorType;
            private Type type = null;
            private boolean causeMonitor;
            private boolean monitorOccured = false;
            private boolean firstMonitor = true;
            private double deadband;
            private double lastMonitorValue = 0.0;
    
            private MonitorField(MonitorType monitorType,boolean causeMonitor) {
                this.causeMonitor = causeMonitor;
                this.monitorType = monitorType;
                this.causeMonitor = causeMonitor;
            }
            private MonitorField(MonitorType monitorType, Type type, double deadband) {
                causeMonitor = true;
                this.monitorType = monitorType;
                this.type = type;
                this.deadband = deadband;
            }
            private void start() {
                clearMonitor();
                firstMonitor = true;
            }
            private void clearMonitor() {
                monitorOccured = false;
            }
            private boolean monitorOccured() {
                return monitorOccured;
            }
            private boolean causeMonitor() {
                return causeMonitor;
            }
            private boolean newData(PVData pvData) {
                if(monitorType==MonitorType.onPut) {
                    monitorOccured = true;
                    return true;
                }
                double newValue;
                switch(type) {
                case pvByte: {
                        PVByte data= (PVByte)pvData;
                        newValue = (double)data.get();
                        break;
                    }
                case pvShort: {
                        PVShort data= (PVShort)pvData;
                        newValue = (double)data.get();
                        break;
                    }
                case pvInt: {
                        PVInt data= (PVInt)pvData;
                        newValue = (double)data.get();
                        break;
                    }
                case pvLong: {
                        PVLong data= (PVLong)pvData;
                        newValue = (double)data.get();
                        break;
                    }
                case pvFloat: {
                        PVFloat data= (PVFloat)pvData;
                        newValue = (double)data.get();
                        break;
                    }
                case pvDouble: {
                        PVDouble data= (PVDouble)pvData;
                        newValue = (double)data.get();
                        break;
                    }
                default:
                    throw new IllegalStateException("Logic error. Why is type not numeric?");      
                } 
                if(firstMonitor) {
                    firstMonitor = false;
                    lastMonitorValue = newValue;
                    monitorOccured = true;
                    return true;
                }
                double diff = newValue - lastMonitorValue;
                if(monitorType==MonitorType.absoluteChange) {
                    if(Math.abs(diff) >= deadband) {
                        lastMonitorValue = newValue;
                        monitorOccured = true;
                        return true;
                    }
                    return false;
                }
                double lastValue = lastMonitorValue;
                if(lastValue!=0.0) {
                    if((100.0*Math.abs(diff)/Math.abs(lastValue)) < deadband) return false;
                }
                lastMonitorValue = newValue;
                monitorOccured = true;
                return true;
            }
            
        }
        
        private static class Monitor {
            private Requestor requestor;
            private ArrayList<MonitorField> monitorFieldList
                = new ArrayList<MonitorField>();
            private ArrayList<ChannelFieldImpl> channelFieldList
                = new ArrayList<ChannelFieldImpl>();
            
            private Monitor(Requestor requestor) {
                this.requestor = requestor;
            }
            private List<MonitorField> getMonitorFieldList() {
                return monitorFieldList;
            }
            private List<ChannelFieldImpl> getChannelFieldList() {
                return channelFieldList;
            }
            private boolean onAbsoluteChange(ChannelFieldImpl channelField, double value) {
                Type type = channelField.getField().getType();
                if(!type.isNumeric()) {
                    requestor.message("field is not a numeric scalar", MessageType.error);
                    return false;
                }
                MonitorField monitorField
                    = new MonitorField(MonitorType.absoluteChange,type,value);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
                return true;
            }         
            private void onPut(ChannelFieldImpl channelField,boolean causeMonitor) {
                MonitorField monitorField = new MonitorField(MonitorType.onPut,causeMonitor);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
            }
            private boolean onPercentageChange(ChannelFieldImpl channelField, double value) {
                Type type = channelField.getField().getType();
                if(!type.isNumeric()) {
                    requestor.message("field is not a numeric scalar", MessageType.error);
                    return false;
                }
                MonitorField monitorField
                    = new MonitorField(MonitorType.percentageChange,type,value);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
                return true;
            }
            private void start() {
                for(MonitorField monitorField: monitorFieldList) {
                    monitorField.start();
                }
            }
            private ChannelField newData(PVData dbData) {
                for(int i=0; i < channelFieldList.size(); i++) {
                    ChannelFieldImpl field = channelFieldList.get(i);
                    PVData data = field.getPVData();
                    if(data==dbData) {
                        MonitorField monitorField = monitorFieldList.get(i);
                        boolean result = monitorField.newData(dbData);
                        if(result) return field;
                        return null;
                    }
                }
                return null;
            }
        }
        
        static private class MonitorThread implements Runnable {
            private ChannelMonitorNotifyRequestor channelMonitorNotifyRequestor;
            private ChannelMonitorRequestor channelMonitorRequestor;
            private ChannelDataQueue channelDataQueue;
            private Thread thread = null;
            private ReentrantLock lock = new ReentrantLock();
            private Condition moreWork = lock.newCondition();

            private MonitorThread(
            String name,int priority,
            ChannelMonitorRequestor channelMonitorRequestor,
            ChannelDataQueue channelDataQueue)
            {
                channelMonitorNotifyRequestor = null;
                this.channelMonitorRequestor = channelMonitorRequestor;
                this.channelDataQueue = channelDataQueue;
                thread = new Thread(this,name);
                thread.setPriority(priority);
                thread.start();
            } 
            private MonitorThread(
            String name,int priority,
            ChannelMonitorNotifyRequestor channelMonitorNotifyRequestor)
            {
                this.channelMonitorNotifyRequestor = channelMonitorNotifyRequestor;
                channelMonitorRequestor = null;
                channelDataQueue = null;
                thread = new Thread(this,name);
                thread.setPriority(priority);
                thread.start();
            } 
            
            public void run() {
                try {
                    while(true) {
                        ChannelData channelData = null;
                        lock.lock();
                        try {
                            while(true) {
                                if(channelDataQueue!=null) {
                                    channelData = channelDataQueue.getNext();
                                    if(channelData!=null) break;
                                } 
                                moreWork.await();
                                if(channelMonitorNotifyRequestor!=null) break;
                            }
                        }finally {
                            lock.unlock();
                        }
                        if(channelData!=null) {
                            int missed = channelDataQueue.getNumberMissed();
                            if(missed>0) channelMonitorRequestor.dataOverrun(missed);
                            channelMonitorRequestor.monitorData(channelData);
                            channelDataQueue.releaseNext(channelData);
                        } else if(channelMonitorNotifyRequestor!=null){
                            channelMonitorNotifyRequestor.monitorEvent();
                        }
                    }
                } catch(InterruptedException e) {
                    
                }
            }
            private void signal() {
                lock.lock();
                try {
                    moreWork.signal();
                } finally {
                    lock.unlock();
                }
            }
            private void stop() {
                thread.interrupt();
            }
        }
    }
}
