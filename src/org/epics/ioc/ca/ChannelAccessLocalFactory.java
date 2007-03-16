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
    private static Convert convert = ConvertFactory.getConvert();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    /**
     * Register. This is called by ChannelFactory.createChannel when it is called
     * before ChannelFactory.registerLocalChannelAccess is called, i.e.
     * it is the default implementation.
     */
    static public void register() {
        channelAccess.register();
    }
    
    private static class ChannelAccessLocal implements ChannelAccess{
        private static AtomicBoolean isRegistered = new AtomicBoolean(false);
        private static ReentrantLock lock = new ReentrantLock();
        private IOCDB iocdb = IOCDBFactory.getMaster();
        
        void register() {
            boolean result = false;
            lock.lock();
            try {
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
    
    private static class ChannelImpl implements Channel,Requestor {
        private boolean isDestroyed = false;
        private ReentrantLock lock = new ReentrantLock();
        private ChannelStateListener stateListener = null;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private PVAccess pvAccess;
        private PVField currentField = null;
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
        private LinkedList<ChannelDataPutImpl> channelDataPutList =
            new LinkedList<ChannelDataPutImpl>();
        private LinkedList<ChannelPutGetImpl> channelPutGetList =
            new LinkedList<ChannelPutGetImpl>();
        private LinkedList<ChannelMonitorImpl> monitorList = 
            new LinkedList<ChannelMonitorImpl>();
        
        ChannelImpl(DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbRecord = record;
            pvRecord = record.getPVRecord();
            pvAccess = PVAccessFactory.createPVAccess(pvRecord);
            if(pvAccess==null) {
                throw new IllegalStateException("ChannelLink createAccess failed. Why?");
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#getChannelName()
         */
        public String getChannelName() {
            return pvRecord.getRecordName();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return getChannelName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            stateListener.message(message, messageType);   
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
                    ChannelGetImpl channelGet = getIter.next();
                    if(channelGet==toDelete) {
                        channelGet.destroy();
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
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelDataPut)
         */
        public void destroy(ChannelDataPut dataPut) {
            ChannelDataPutImpl toDelete = (ChannelDataPutImpl)dataPut;
            lock.lock();
            try {
                Iterator<ChannelDataPutImpl> putIter = channelDataPutList.iterator();
                while(putIter.hasNext()) {
                    ChannelDataPutImpl channelDataPut = putIter.next();
                    if(channelDataPut==toDelete) {
                        channelDataPut.destroy();
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
        public ChannelFindFieldResult findField(String name) {
            lock.lock();
            try {
                if(isDestroyed) return ChannelFindFieldResult.failure;
                AccessSetResult result = pvAccess.findField(name);
                if(result==AccessSetResult.notFound) return ChannelFindFieldResult.notFound;
                if(result==AccessSetResult.otherRecord) {
                    otherChannel = pvAccess.getOtherRecord();
                    otherField = pvAccess.getOtherField();
                    currentField = null;
                    return ChannelFindFieldResult.otherChannel;
                }
                if(result==AccessSetResult.thisRecord) {
                    currentField = pvAccess.getField();
                    otherChannel = null;
                    otherField = null;
                    return ChannelFindFieldResult.thisChannel;
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
                    return new ChannelFieldImpl(dbRecord.findDBField(currentField));
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
        public ChannelGet createChannelGet(
            ChannelFieldGroup channelFieldGroup,ChannelGetRequestor channelGetRequestor, boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelGetImpl channelGet = 
                            new ChannelGetImpl(channelFieldGroup,channelGetRequestor,process);
                    channelGetList.add(channelGet);
                    return channelGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelPutRequestor, org.epics.ioc.ca.ChannelProcessRequestor)
         */
        public ChannelPut createChannelPut(
            ChannelFieldGroup channelFieldGroup,ChannelPutRequestor channelPutRequestor, boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutImpl channelPut = 
                        new ChannelPutImpl(channelFieldGroup,channelPutRequestor,process);
                    channelPutList.add(channelPut);
                    return channelPut;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createDataChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelDataPutRequestor, boolean)
         */
        public ChannelDataPut createChannelDataPut(ChannelFieldGroup channelFieldGroup,
                ChannelDataPutRequestor channelDataPutRequestor, boolean process,boolean supportAlso)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelDataPutImpl channelDataPut = 
                        new ChannelDataPutImpl(this,channelFieldGroup,channelDataPutRequestor,process,supportAlso);
                    channelDataPutList.add(channelDataPut);
                    return channelDataPut;
                }
            } finally {
                lock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelPutGetRequestor, org.epics.ioc.ca.ChannelProcessRequestor)
         */
        public ChannelPutGet createChannelPutGet(
            ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
            ChannelPutGetRequestor channelPutGetRequestor, boolean process)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutGetImpl channelPutGet = 
                        new ChannelPutGetImpl(putFieldGroup,getFieldGroup,
                                channelPutGetRequestor,process);
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
            boolean onlyWhileProcessing,boolean supportAlso)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    stateListener.message(
                        "channel has been destroyed",MessageType.fatalError);
                    return null;
                } else {
                    ChannelMonitorImpl impl = 
                        new ChannelMonitorImpl(onlyWhileProcessing,supportAlso,this,this);
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
            private DBField dbField;
            private PVField pvField;
            
            ChannelFieldImpl(DBField dbField) {
                this.dbField = dbField;
                pvField = dbField.getPVField();
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getAccessRights()
             */
            public AccessRights getAccessRights() {
                // OK until access security is implemented
                if(pvField.getField().isMutable()) {
                    return AccessRights.readWrite;
                } else {
                    return AccessRights.read;
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getField()
             */
            public Field getField() {
                return pvField.getField();
            }

            DBField getDBField() {
                return dbField;
            }
           
            PVField getPVField() {
                return pvField;
            }
            /* (non-Javadoc)
             * @see java.lang.Object#toString()
             */
            public String toString() {
                return pvField.getField().toString();
            }
    
        }
        
        private class FieldGroupImpl implements ChannelFieldGroup {
            private LinkedList<ChannelField> fieldList = 
                new LinkedList<ChannelField>();
    
            FieldGroupImpl(ChannelFieldGroupListener listener) {}
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
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#getList()
             */
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
                 
            ChannelProcessImpl(ChannelProcessRequestor channelRequestor)
            {
                this.channelProcessRequestor = channelRequestor;
                recordProcess = dbRecord.getRecordProcess();
                boolean isRequestor = recordProcess.setRecordProcessRequestor(this);
                if(!isRequestor) {
                    throw new IllegalStateException("record already has recordProcessRequestor"); 
                }
                requestorName = "ChannelProcess:" + channelRequestor.getRequestorName();
            }           
            void destroy() {
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
            private List<ChannelField> channelFieldList;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequestor = false;
            
            private RequestResult requestResult = RequestResult.success;
            private Iterator<ChannelField> channelFieldListIter;
            private PVField pvField;
            
            ChannelGetImpl(ChannelFieldGroup channelFieldGroup,
                ChannelGetRequestor channelGetRequestor,boolean process)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                this.channelGetRequestor = channelGetRequestor;
                channelFieldList = fieldGroup.getList();
                requestorName = "ChannelGet:" + channelGetRequestor.getRequestorName();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequestor = recordProcess.setRecordProcessRequestor(this);
                    if(!isRecordProcessRequestor) {
                        channelGetRequestor.message(
                             "Already has a recordProcessRequestor", MessageType.warning);
                    }
                }
            }
            
            void destroy() {
                if(isRecordProcessRequestor) recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#get(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public boolean get() {
                if(!isConnected()) {
                    channelGetRequestor.message(
                        "channel is not connected",MessageType.info);
                    return false;
                }
                requestResult = RequestResult.success;
                if(isRecordProcessRequestor) {
                    return recordProcess.process(this, true, null);
                }
                startGetData();
                return true;
            }                
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#getDelayed(org.epics.ioc.pv.PVField)
             */
            public void getDelayed(PVField pvField) {
                if(pvField!=this.pvField) {
                    throw new IllegalStateException("pvField is not correct"); 
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
            
            void startGetData() {
                channelFieldList = fieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                getData();
            }
            
            void getData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(isRecordProcessRequestor) recordProcess.setInactive(this);
                            channelGetRequestor.getDone(requestResult);
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelGetRequestor.nextGetField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelGetRequestor.nextDelayedGetField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
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
            private boolean isRecordProcessRequestor = false;
            
            private RequestResult requestResult = null;
            
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVField pvField;
            private ChannelFieldImpl field;
            
            ChannelPutImpl(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequestor channelPutRequestor, boolean process)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                this.channelPutRequestor = channelPutRequestor;
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequestor = recordProcess.setRecordProcessRequestor(this);
                    if(!isRecordProcessRequestor) {
                        channelPutRequestor.message(
                                "Already has a recordProcessRequestor", MessageType.warning);
                    }
                }
                requestorName = "ChannelPut:" + channelPutRequestor.getRequestorName();
            } 
            
            void destroy() {
                if(isRecordProcessRequestor) recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public boolean put() {
                if(isDestroyed) return false;
                if(!isConnected()) {
                    channelPutRequestor.message(
                        "channel is not connected",MessageType.info);
                    return false;
                }
                if(isRecordProcessRequestor) {
                    if(!recordProcess.setActive(this)) return false;
                    startPutData();
                    recordProcess.process(this, false, null);
                    return true;
                }
                startPutData();
                return true;
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#putDelayed(org.epics.ioc.pv.PVField)
             */
            public void putDelayed(PVField pvField) {
                if(pvField!=this.pvField) {
                    throw new IllegalStateException("pvField is not correct"); 
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
            
            void startPutData() {
                channelFieldList = fieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                putData();
            }
            
            void putData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(isRecordProcessRequestor) {
                                recordProcess.process(this, false, null);
                            } else {
                                channelPutRequestor.putDone(requestResult);
                            }
                            return;
                        }
                        field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelPutRequestor.nextPutField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        field.getDBField().postPut();
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutRequestor.nextDelayedPutField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        field.getDBField().postPut();
                        pvField = null;
                    }
                }
            }
        }
        
        private class ChannelDataPutImpl implements ChannelDataPut,RecordProcessRequestor
        {
            private String requestorName;
            private ChannelDataPutRequestor channelDataPutRequestor = null;
            private FieldGroupImpl fieldGroup = null;
            private List<ChannelField> channelFieldList;
            private ChannelData channelData;
            
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequestor = false;
            
            private RequestResult requestResult = null;
            
            
            
            ChannelDataPutImpl(Channel channel,ChannelFieldGroup channelFieldGroup,
                ChannelDataPutRequestor channelDataPutRequestor, boolean process,boolean supportAlso)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                channelFieldList = channelFieldGroup.getList();
                this.channelDataPutRequestor = channelDataPutRequestor;
                channelData = ChannelDataFactory.createChannelData(
                     channel, channelFieldGroup, supportAlso);
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequestor = recordProcess.setRecordProcessRequestor(this);
                    if(!isRecordProcessRequestor) {
                        channelDataPutRequestor.message(
                                "Already has a recordProcessRequestor", MessageType.warning);
                    }
                }
                requestorName = "ChannelPut:" + channelDataPutRequestor.getRequestorName();
            } 
            
            void destroy() {
                if(isRecordProcessRequestor) recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelDataPut#get()
             */
            public boolean get() {
                Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
                dbRecord.lock();
                try {
                    channelFieldList = fieldGroup.getList();
                    channelFieldListIter = channelFieldList.iterator();
                    while(channelFieldListIter.hasNext()) {
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        PVField targetPVField = field.getPVField();
                        channelData.dataPut(targetPVField);
                    }
                } finally {
                    dbRecord.unlock();
                }
                channelDataPutRequestor.getDone();
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelDataPut#getChannelData()
             */
            public ChannelData getChannelData() {
                return channelData;
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public boolean put() {
                if(isDestroyed) return false;
                if(!isConnected()) {
                    channelDataPutRequestor.message(
                        "channel is not connected",MessageType.info);
                    return false;
                }
                if(isRecordProcessRequestor) {
                    if(!recordProcess.setActive(this)) return false;
                    putData();
                    recordProcess.process(this, false, null);
                    return true;
                }
                dbRecord.lock();
                try {
                    putData();
                } finally {
                    dbRecord.unlock();
                }
                channelDataPutRequestor.getDone();
                return true;
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
             */
            public void recordProcessComplete() {
                channelDataPutRequestor.putDone(requestResult);
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
                channelDataPutRequestor.message(message, messageType);
            }
            
            private void putData() {
                Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
                CDStructure cdStructure = channelData.getCDRecord().getCDStructure();
                CDField[] cdFields = cdStructure.getFieldCDFields();
                for(CDField cdField : cdFields) {
                    ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                    copyChanges(cdField, field.getDBField());
                }
                
            }
            
            private void copyChanges(CDField cdField, DBField dbField) {
                PVField fromPVField = cdField.getPVField();
                PVField targetPVField = dbField.getPVField();
                boolean post = false;
                if(cdField.getNumSupportNamePuts()>0) {
                    targetPVField.setSupportName(fromPVField.getSupportName());
                    post = true;
                }
                Field field = fromPVField.getField();
                Type type = field.getType();
                if(type.isScalar()) {
                    if(cdField.getNumPuts()>0) {
                        convert.copyScalar(fromPVField, targetPVField);
                        post = true;
                    }
                    if(post) dbField.postPut();
                    return;
                }
                if(type==Type.pvEnum) {
                    CDEnum cdEnum = (CDEnum)cdField;
                    PVEnum from = (PVEnum)fromPVField;
                    DBEnum dbEnum = (DBEnum)dbField;
                    if(cdEnum.getNumIndexPuts()>0) {
                        dbEnum.setIndex(from.getIndex());
                    }
                    if(cdEnum.getNumChoicesPut()>0) {
                        dbEnum.setChoices(from.getChoices());
                    }
                    
                    return;
                }
                if(type==Type.pvMenu) {
                    CDMenu cdMenu = (CDMenu)cdField;
                    PVMenu from = (PVMenu)fromPVField;
                    DBMenu dbMenu = (DBMenu)dbField;
                    if(cdMenu.getNumIndexPuts()>0) {
                        dbMenu.setIndex(from.getIndex());
                    }
                    return;
                }
                if(type==Type.pvLink) {
                    CDLink cdLink = (CDLink)cdField;
                    PVLink from = (PVLink)fromPVField;
                    DBLink dbLink = (DBLink)dbField;
                    if(cdLink.getNumConfigurationStructurePuts()>0) {
                        dbLink.setConfigurationStructure(from.getConfigurationStructure());
                    }
                    return;
                }
                if(type==Type.pvStructure) {
                    CDStructure cdStructure = (CDStructure)cdField;
                    CDField[] cdFields = cdStructure.getFieldCDFields();
                    DBStructure to = (DBStructure)dbField;
                    DBField[] dbFields = to.getFieldDBFields();
                    for(int i=0; i<cdFields.length; i++) {
                        copyChanges(cdFields[i],dbFields[i]);
                    }
                    return;
                }
                if(type==Type.pvArray) {
                    Array array = (Array)field;
                    Type elementType = array.getElementType();
                    if(elementType.isScalar()) {
                        PVArray from = (PVArray)fromPVField;
                        PVArray to = (PVArray)targetPVField;
                        if(cdField.getNumPuts()>0) {
                            convert.copyArray(from, 0, to, 0, from.getLength());
                            dbField.postPut();
                        }
                        return;
                    }
                    copyNonScalarArray((CDNonScalarArray)cdField,(DBNonScalarArray)dbField);
                }
            }
            private void copyNonScalarArray(CDNonScalarArray cdArray,DBNonScalarArray dbArray) {
                PVArray pvArray = (PVArray)cdArray.getPVField();
                int length = pvArray.getLength();
                Array array = (Array)pvArray.getField();
                Type elementType = array.getElementType();
                CDField[] cdFields = cdArray.getElementCDFields();
                DBField[] dbFields = dbArray.getElementDBFields();
                for(int i=0; i<length; i++) {
                    CDField cdField = cdFields[i];
                    DBField dbField = dbFields[i];
                    if(cdField==null) continue;
                    if(cdField.getMaxNumPuts()==0) continue;
                    PVField pvNew = null;
                    if(dbField==null) {
                        PVField parent = dbArray.getPVField();
                        PVField thisField = cdField.getPVField();
                        Field field = cdField.getPVField().getField();

                        if(elementType==Type.pvArray) {
                            int capacity = ((PVArray)thisField).getLength();
                            pvNew = pvDataCreate.createPVArray(parent, field, capacity, true);
                        } else {
                            pvNew = pvDataCreate.createPVField(parent, field);
                        }
                        DBRecord dbRecord = dbArray.getDBRecord();
                        switch(elementType) {
                        case pvEnum:
                            dbField = new BaseDBEnum(dbArray,dbRecord,pvNew); break;
                        case pvMenu:
                            dbField = new BaseDBMenu(dbArray,dbRecord,pvNew); break;
                        case pvLink:
                            dbField = new BaseDBMenu(dbArray,dbRecord,pvNew); break;
                        case pvStructure:
                            dbField = new BaseDBStructure(dbArray,dbRecord,pvNew); break;
                        case pvArray:
                            dbField = new BaseDBNonScalarArray(dbArray,dbRecord,(PVArray)pvNew); break;
                        }
                        dbFields[i] = dbField;
                    }
                    copyChanges(cdField,dbField);
                }
            }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,RecordProcessRequestor
        {
            private String requestorName;
            private ChannelPutGetRequestor channelPutGetRequestor = null;
            private ChannelFieldGroup putFieldGroup = null;
            private ChannelFieldGroup getFieldGroup = null;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequestor = false;
            private RequestResult requestResult = null;
            
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVField pvField;
            
            
            ChannelPutGetImpl(
                ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
                ChannelPutGetRequestor channelPutGetRequestor,boolean process)
            {
                this.putFieldGroup = putFieldGroup;
                this.getFieldGroup = getFieldGroup;
                this.channelPutGetRequestor = channelPutGetRequestor;
                requestorName = "ChannelGetPut:" + channelPutGetRequestor.getRequestorName();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequestor = recordProcess.setRecordProcessRequestor(this);
                    if(!isRecordProcessRequestor) {
                        throw new IllegalStateException("record already has recordProcessRequestor"); 
                    }
                }
            }
            
            void destroy() {
                if(isRecordProcessRequestor)recordProcess.releaseRecordProcessRequestor(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup)
             */
            public boolean putGet()
            {
                if(isDestroyed) return false;
                requestResult = RequestResult.success;
                if(isRecordProcessRequestor) {
                    boolean result = recordProcess.setActive(this);
                    if(result==false) return result;
                }
                startPutData();
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#getDelayed(org.epics.ioc.pv.PVField)
             */
            public void getDelayed(PVField pvField) {
                getData();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putDelayed(org.epics.ioc.pv.PVField)
             */
            public void putDelayed(PVField pvField) {
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
            
            void startPutData() {
                channelFieldList = putFieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                putData();
            }
            
            void putData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            channelPutGetRequestor.putDone(RequestResult.success);
                            if(isRecordProcessRequestor) {
                                recordProcess.process(this, true, null);
                            } else {
                                startGetData();
                            }
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextPutField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextDelayedPutField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    }
                }
                
            }
            
            void startGetData() {
                channelFieldList = getFieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                getData();
            }
           
            void getData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(isRecordProcessRequestor) recordProcess.setInactive(this);
                            channelPutGetRequestor.getDone(requestResult);
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextGetField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequestor.nextDelayedGetField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    }
                }
            }
        }
        
        
              
        private class ChannelMonitorImpl implements
        ChannelFieldGroupListener,ChannelMonitor,DBListener,Requestor
        {
            private Channel channel;
            boolean onlyWhileProcesing;
            boolean supportAlso;
            private Requestor requestor;
            private Monitor monitor = null;
            private boolean isStarted = false;
            private RecordListener recordListener = null;
            private boolean processActive = false;
            private ChannelMonitorRequestor channelMonitorRequestor;
            private ChannelFieldGroup channelFieldGroup = null;
            private ChannelDataQueue channelDataQueue = null;
            private MonitorThread monitorThread;
            private ChannelData channelData = null;
            private boolean monitorOccured = false;
            
            ChannelMonitorImpl(
                boolean onlyWhileProcesing,boolean supportAlso,
                Channel channel,Requestor requestor)
            {
                this.channel = channel;
                this.supportAlso = supportAlso;
                this.onlyWhileProcesing = onlyWhileProcesing;
                this.requestor = requestor;
                this.channelFieldGroup = channel.createFieldGroup(this);
                monitor = new Monitor(this);
            }           
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#getRequestorName()
             */
            public String getRequestorName() {
                return requestor.getRequestorName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                requestor.message(message, messageType);
            }

            void destroy() {
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
                        message("channel has been destroyed",MessageType.fatalError);
                    } else if(isStarted) {
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
                        message("channel has been destroyed",MessageType.fatalError);
                    } else if(isStarted) {
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
                        message("channel has been destroyed",MessageType.fatalError);
                    } else if(isStarted) {
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
                        message("channel has been destroyed",MessageType.fatalError);
                        return false;
                    } else if(isStarted) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        channelMonitorRequestor = null;
                        monitor.start();
                        recordListener = dbRecord.createRecordListener(this);
                        List<ChannelFieldImpl> channelFieldList = monitor.getChannelFieldList();
                        if(threadName==null) threadName =
                            channelMonitorNotifyRequestor.getRequestorName() + "NotifyThread";
                        int priority = scanPriority.getJavaPriority();
                        monitorThread = new MonitorThread(
                             threadName,priority,channelMonitorNotifyRequestor);
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBField dbField = channelField.getDBField();
                            dbField.addListener(recordListener);
                        }
                        isStarted = true;
                        processActive = false;
                        monitorOccured = false;
                    }
                } finally {
                    lock.unlock();
                }
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#start(org.epics.ioc.ca.ChannelMonitorRequestor)
             */
            public boolean start(ChannelMonitorRequestor channelMonitorRequestor,
                int queueSize, String threadName, ScanPriority scanPriority)
            {
                lock.lock();
                try {
                    if(isDestroyed) {
                        message("channel has been destroyed",MessageType.fatalError);
                        return false;
                    } else if(isStarted) {
                        throw new IllegalStateException("illegal request. monitor active");
                    } else {
                        this.channelMonitorRequestor = channelMonitorRequestor;
                        channelFieldGroup = channel.createFieldGroup(this);
                        List<ChannelFieldImpl> channelFieldList = monitor.getChannelFieldList();
                        for(ChannelField channelField: channelFieldList) {
                            channelFieldGroup.addChannelField(channelField);
                        }
                        channelDataQueue = ChannelDataFactory.createDataQueue(
                            queueSize, channel, channelFieldGroup,supportAlso);
                        if(threadName==null) threadName =
                            channelMonitorRequestor.getRequestorName() + "NotifyThread";
                        int priority = scanPriority.getJavaPriority();
                        monitorThread = new MonitorThread(
                            threadName,priority,channelMonitorRequestor,channelDataQueue);
                        monitor.start();
                        recordListener = dbRecord.createRecordListener(this);
                        ChannelData initialData = channelDataQueue.getFree(true);
                        initialData.clearNumPuts();
                        // give the initial data to the user
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBField dbField = channelField.getDBField();
                            initialData.dataPut(dbField.getPVField());
                        }
                        channelData = channelDataQueue.getFree(true);
                        CDField[] initialDatas = initialData.getCDRecord().getCDStructure().getFieldCDFields();
                        CDField[] channelDatas = channelData.getCDRecord().getCDStructure().getFieldCDFields();
                        for(int i=0; i<initialDatas.length; i++) {
                            channelDatas[i].dataPut(initialDatas[i].getPVField());
                        }
                        channelDataQueue.setInUse(initialData);
                        monitorThread.signal();
                        channelData.clearNumPuts();
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBField dbField = channelField.getDBField();
                            dbField.addListener(recordListener);
                        }
                        isStarted = true;
                        processActive = false;
                        monitorOccured = false;
                    }
                } finally {
                    lock.unlock();
                }
                notifyRequestor();
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#stop()
             */
            public void stop() {
                lock.lock();
                try {
                    if(!isStarted) return;
                    isStarted = false;
                } finally {
                    lock.unlock();
                }
                dbRecord.removeRecordListener(recordListener);
                recordListener = null;
                if(channelMonitorRequestor!=null) {
                    channelDataQueue = null;
                    channelData = null;
                    monitorThread.stop();
                }
                channelMonitorRequestor = null;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#isStarted()
             */
            public boolean isStarted() {
                return isStarted;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginProcess()
             */
            public void beginProcess() {
                if(!isStarted) return;
                processActive = true;
            } 
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endProcess()
             */
            public void endProcess() {
                if(!isStarted) return;
                notifyRequestor();
                processActive = false;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.db.DBStructure)
             */
            public void beginPut(DBStructure dbStructure) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                if(channelData!=null) channelData.beginPut(dbStructure.getPVStructure());
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.db.DBStructure)
             */
            public void endPut(DBStructure dbStructure) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) channelData.dataPut(dbStructure.getPVStructure());
                }                    
                if(!processActive) notifyRequestor();
            }             
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
             */
            public void dataPut(DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                ChannelField channelField = monitor.newField(dbField.getPVField());
                if(channelField==null) return;
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) channelData.dataPut(dbField.getPVField());
                }                    
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.db.DBEnum)
             */
            public void enumChoicesPut(DBEnum dbEnum) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.enumChoicesPut(dbEnum.getPVEnum());
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.db.DBEnum)
             */
            public void enumIndexPut(DBEnum dbEnum) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.enumIndexPut(dbEnum.getPVEnum());
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField)
             */
            public void supportNamePut(DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.supportNamePut(dbField.getPVField());
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.db.DBLink)
             */
            public void configurationStructurePut(DBLink dbLink) {
                monitorOccured = true;
                channelData.configurationStructurePut(dbLink.getPVLink());
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
             */
            public void dataPut(DBField requested, DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.dataPut(requested.getPVField(), dbField.getPVField());              
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBEnum)
             */
            public void enumChoicesPut(DBField requested,DBEnum dbEnum) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.enumChoicesPut(requested.getPVField(),dbEnum.getPVEnum());
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBEnum)
             */
            public void enumIndexPut(DBField requested,DBEnum dbEnum) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.enumIndexPut(requested.getPVField(),dbEnum.getPVEnum());
                if(!processActive) notifyRequestor();
            }           
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
             */
            public void supportNamePut(DBField requested,DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelData!=null) channelData.supportNamePut(requested.getPVField(),dbField.getPVField());
                if(!processActive) notifyRequestor();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBLink)
             */
            public void configurationStructurePut(DBField requested,DBLink dbLink) {
                monitorOccured = true;
                if(channelData!=null) channelData.configurationStructurePut(requested.getPVField(),dbLink.getPVLink());
            }

            void notifyRequestor() {
                List<MonitorField> list = monitor.getMonitorFieldList();
                for(MonitorField field : list) {
                    if(field.monitorOccured()) {
                        field.clearMonitor();
                        if(field.causeMonitor()) monitorOccured = true;
                    }
                }
                if(!monitorOccured) return;
                monitorOccured = false;
                if(channelMonitorRequestor!=null) {
                    if(channelData!=null) {
                        ChannelData initialData = channelData;
                        channelData = channelDataQueue.getFree(true);
                        CDField[] initialDatas = initialData.getCDRecord().getCDStructure().getFieldCDFields();
                        CDField[] channelDatas = channelData.getCDRecord().getCDStructure().getFieldCDFields();
                        for(int i=0; i<initialDatas.length; i++) {
                            channelDatas[i].dataPut(initialDatas[i].getPVField());
                        }
                        channelData.clearNumPuts();
                        channelDataQueue.setInUse(initialData);
                        monitorThread.signal();
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
        
        enum MonitorType {
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
    
            MonitorField(MonitorType monitorType,boolean causeMonitor) {
                this.causeMonitor = causeMonitor;
                this.monitorType = monitorType;
                this.causeMonitor = causeMonitor;
            }
            MonitorField(MonitorType monitorType, Type type, double deadband) {
                causeMonitor = true;
                this.monitorType = monitorType;
                this.type = type;
                this.deadband = deadband;
            }
            
            public void start() {
                clearMonitor();
                firstMonitor = true;
            }
            public void clearMonitor() {
                monitorOccured = false;
            }
            public boolean monitorOccured() {
                return monitorOccured;
            }
            public boolean causeMonitor() {
                return causeMonitor;
            }
            public boolean newField(PVField pvField) {
                if(monitorType==MonitorType.onPut) {
                    monitorOccured = true;
                    return true;
                }
                double newValue;
                switch(type) {
                case pvByte: {
                        PVByte data= (PVByte)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvShort: {
                        PVShort data= (PVShort)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvInt: {
                        PVInt data= (PVInt)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvLong: {
                        PVLong data= (PVLong)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvFloat: {
                        PVFloat data= (PVFloat)pvField;
                        newValue = (double)data.get();
                        break;
                    }
                case pvDouble: {
                        PVDouble data= (PVDouble)pvField;
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
            
            Monitor(Requestor requestor) {
                this.requestor = requestor;
            }
            public List<MonitorField> getMonitorFieldList() {
                return monitorFieldList;
            }
            public List<ChannelFieldImpl> getChannelFieldList() {
                return channelFieldList;
            }
            public boolean onAbsoluteChange(ChannelFieldImpl channelField, double value) {
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
            public void onPut(ChannelFieldImpl channelField,boolean causeMonitor) {
                MonitorField monitorField = new MonitorField(MonitorType.onPut,causeMonitor);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
            }
            public boolean onPercentageChange(ChannelFieldImpl channelField, double value) {
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
            public void start() {
                for(MonitorField monitorField: monitorFieldList) {
                    monitorField.start();
                }
            }
            
            public ChannelField newField(PVField pvField) {
                for(int i=0; i < channelFieldList.size(); i++) {
                    ChannelFieldImpl channelField = channelFieldList.get(i);
                    PVField data = channelField.getPVField();
                    if(data==pvField) {
                        MonitorField monitorField = monitorFieldList.get(i);
                        boolean result = monitorField.newField(pvField);
                        if(result) return channelField;
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

            MonitorThread(
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
            MonitorThread(
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
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
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
            public void signal() {
                lock.lock();
                try {
                    moreWork.signal();
                } finally {
                    lock.unlock();
                }
            }
            public void stop() {
                thread.interrupt();
            }
        }
    }
}
