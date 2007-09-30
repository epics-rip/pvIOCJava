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
 * and 3) ChannelRequester.requestDone
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
    
    private static class ChannelImpl implements Channel,Requester {
        private boolean isDestroyed = false;
        private ReentrantLock lock = new ReentrantLock();
        private ChannelStateListener stateListener = null;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private LinkedList<FieldGroupImpl> fieldGroupList = 
            new LinkedList<FieldGroupImpl>();
        private LinkedList<ChannelProcessImpl> channelProcessList =
            new LinkedList<ChannelProcessImpl>();
        private LinkedList<ChannelGetImpl> channelGetList =
            new LinkedList<ChannelGetImpl>();
        private LinkedList<ChannelCDGetImpl> channelCDGetList =
            new LinkedList<ChannelCDGetImpl>();
        private LinkedList<ChannelPutImpl> channelPutList =
            new LinkedList<ChannelPutImpl>();
        private LinkedList<ChannelCDPutImpl> channelCDPutList =
            new LinkedList<ChannelCDPutImpl>();
        private LinkedList<ChannelPutGetImpl> channelPutGetList =
            new LinkedList<ChannelPutGetImpl>();
        private LinkedList<ChannelMonitorImpl> monitorList = 
            new LinkedList<ChannelMonitorImpl>();
        
        private ChannelImpl(DBRecord record,ChannelStateListener listener) {
            stateListener = listener;
            dbRecord = record;
            pvRecord = record.getPVRecord();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#getChannelName()
         */
        public String getChannelName() {
            return pvRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
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
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelCDGet)
         */
        public void destroy(ChannelCDGet channelCDGet) {
            ChannelCDGetImpl toDelete = (ChannelCDGetImpl)channelCDGet;
            lock.lock();
            try {
                Iterator<ChannelCDGetImpl> putIter = channelCDGetList.iterator();
                while(putIter.hasNext()) {
                    ChannelCDGetImpl channelDataGet = putIter.next();
                    if(channelDataGet==toDelete) {
                        channelDataGet.destroy();
                        putIter.remove();
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
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelCDPut)
         */
        public void destroy(ChannelCDPut channelCDPut) {
            ChannelCDPutImpl toDelete = (ChannelCDPutImpl)channelCDPut;
            lock.lock();
            try {
                Iterator<ChannelCDPutImpl> putIter = channelCDPutList.iterator();
                while(putIter.hasNext()) {
                    ChannelCDPutImpl channelDataPut = putIter.next();
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
         * @see org.epics.ioc.ca.Channel#findField(java.lang.String)
         */
        public ChannelField findField(String name) {
            lock.lock();
            try { //ChannelFieldImpl(dbRecord.findDBField(currentField))
                if(isDestroyed) return null;
                if(name==null || name.length()<=0) return new ChannelFieldImpl(dbRecord.getDBStructure());
                PVField pvField = pvRecord.findProperty(name);
                if(pvField==null) return null;
                return new ChannelFieldImpl(dbRecord.findDBField(pvField));               
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
         * @see org.epics.ioc.ca.Channel#createChannelProcess(org.epics.ioc.ca.ChannelProcessRequester, boolean)
         */
        public ChannelProcess createChannelProcess(ChannelProcessRequester channelProcessRequester,
            boolean processSelfOK)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    channelProcessRequester.message(
                        "channel has been destroyed",MessageType.fatalError);
                    return null;
                } else {
                    ChannelProcessImpl channelProcess;
                    try {
                        channelProcess = new ChannelProcessImpl(channelProcessRequester,processSelfOK);
                        channelProcessList.add(channelProcess);
                    } catch(IllegalStateException e) {
                        channelProcessRequester.message(
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
         * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelGetRequester)
         */
        public ChannelGet createChannelGet(
            ChannelFieldGroup channelFieldGroup,ChannelGetRequester channelGetRequester)
        {
            return createChannelGet(channelFieldGroup,channelGetRequester,false,false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelGetRequester, boolean, boolean)
         */
        public ChannelGet createChannelGet(ChannelFieldGroup channelFieldGroup,
            ChannelGetRequester channelGetRequester, boolean process, boolean processSelfOK)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelGetImpl channelGet = 
                            new ChannelGetImpl(channelFieldGroup,channelGetRequester,process,processSelfOK);
                    channelGetList.add(channelGet);
                    return channelGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelCDGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelCDGetRequester, boolean)
         */
        public ChannelCDGet createChannelCDGet(ChannelFieldGroup channelFieldGroup,
            ChannelCDGetRequester channelCDGetRequester,boolean supportAlso)
        {
            return createChannelCDGet(channelFieldGroup,channelCDGetRequester,supportAlso,false,false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelCDGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelCDGetRequester, boolean, boolean, boolean)
         */
        public ChannelCDGet createChannelCDGet(ChannelFieldGroup channelFieldGroup,
            ChannelCDGetRequester channelCDGetRequester,
            boolean supportAlso, boolean process, boolean processSelfOK)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelCDGetImpl channelDataGet = 
                        new ChannelCDGetImpl(this,channelFieldGroup,channelCDGetRequester,supportAlso,
                                process,processSelfOK);
                    channelCDGetList.add(channelDataGet);
                    return channelDataGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester)
         */
        public ChannelPut createChannelPut(
            ChannelFieldGroup channelFieldGroup,ChannelPutRequester channelPutRequester)
        {
            return createChannelPut(channelFieldGroup,channelPutRequester,false,false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester, boolean, boolean)
         */
        public ChannelPut createChannelPut(ChannelFieldGroup channelFieldGroup,
            ChannelPutRequester channelPutRequester, boolean process, boolean processSelfOK)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutImpl channelPut = 
                        new ChannelPutImpl(channelFieldGroup,channelPutRequester,process,processSelfOK);
                    channelPutList.add(channelPut);
                    return channelPut;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelCDPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelCDPutRequester, boolean)
         */
        public ChannelCDPut createChannelCDPut(ChannelFieldGroup channelFieldGroup,
                ChannelCDPutRequester channelCDPutRequester,boolean supportAlso)
        {
            return createChannelCDPut(channelFieldGroup,channelCDPutRequester,supportAlso,false,false);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelCDPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelCDPutRequester, boolean, boolean, boolean)
         */
        public ChannelCDPut createChannelCDPut(ChannelFieldGroup channelFieldGroup,
            ChannelCDPutRequester channelCDPutRequester,
            boolean supportAlso, boolean process, boolean processSelfOK)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelCDPutImpl channelDataPut = 
                        new ChannelCDPutImpl(this,channelFieldGroup,channelCDPutRequester,
                                supportAlso,process,processSelfOK);
                    channelCDPutList.add(channelDataPut);
                    return channelDataPut;
                }
            } finally {
                lock.unlock();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester)
         */
        public ChannelPutGet createChannelPutGet(
            ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
            ChannelPutGetRequester channelPutGetRequester)
        {
            return createChannelPutGet(putFieldGroup,getFieldGroup,channelPutGetRequester,false,false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester, boolean, boolean)
         */
        public ChannelPutGet createChannelPutGet(ChannelFieldGroup putFieldGroup,
            ChannelFieldGroup getFieldGroup, ChannelPutGetRequester channelPutGetRequester,
            boolean process, boolean processSelfOK)
        {
            lock.lock();
            try {
                if(isDestroyed) {
                    return null;
                } else {
                    ChannelPutGetImpl channelPutGet = 
                        new ChannelPutGetImpl(putFieldGroup,getFieldGroup,
                                channelPutGetRequester,process,processSelfOK);
                    channelPutGetList.add(channelPutGet);
                    return channelPutGet;
                }
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequester, boolean)
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
            private DBField dbField = null;
            private PVField pvField;
            
            ChannelFieldImpl(DBField dbField) {
                this.dbField = dbField;
                pvField = dbField.getPVField();
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getPropertyNames()
             */
            public String[] getPropertyNames() {
                return pvField.getPropertyNames();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#findProperty(java.lang.String)
             */
            public ChannelField findProperty(String propertyName) {
                PVField pvf = pvField.findProperty(propertyName);
                if(pvf==null) return null;
                DBField dbf = dbField.getDBRecord().findDBField(pvf);
                return new ChannelFieldImpl(dbf);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getAccessRights()
             */
            public AccessRights getAccessRights() {
                // OK until access security is implemented
                if(pvField.isMutable()) {
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

            private DBField getDBField() {
                return dbField;
            }
           
            private PVField getPVField() {
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
        
        private class ChannelProcessImpl implements ChannelProcess,RecordProcessRequester
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelProcessRequester channelProcessRequester = null;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            
            private RequestResult requestResult = null;
                 
            private ChannelProcessImpl(ChannelProcessRequester channelProcessRequester,boolean processSelfOK)
            {
                this.channelProcessRequester = channelProcessRequester;
                recordProcess = dbRecord.getRecordProcess();
                isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester) {
                    if(!processSelfOK) {
                        throw new IllegalStateException("already has process requester");
                    }
                    if(!recordProcess.canProcessSelf()) {
                        throw new IllegalStateException(
                            "already has process requester other than self");
                    }
                }
                requesterName = "ChannelProcess:" + channelProcessRequester.getRequesterName();
            }           
            private void destroy() {
                isDestroyed = true;
                recordProcess.releaseRecordProcessRequester(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcess#process()
             */
            public void process() {
                if(isDestroyed) return;
                if(!isConnected()) {
                    channelProcessRequester.message(
                            "channel is not connected",MessageType.info);
                    channelProcessRequester.processDone(RequestResult.failure);
                    return;
                }
                if(isRecordProcessRequester) {
                    if(recordProcess.process(this, false, null)) return;
                } else if(recordProcess.processSelfRequest(this)) {
                    recordProcess.processSelfProcess(this, false);
                    return;
                }
                channelProcessRequester.message(
                        "could not process record",MessageType.error);
                channelProcessRequester.processDone(RequestResult.failure);
            }    
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelProcessRequester.message(message, messageType);
            }    
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#processResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
             */
            public void recordProcessComplete() {
                channelProcessRequester.processDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#ready()
             */
            public RequestResult ready() {
                throw new IllegalStateException("Logic error. Why was this called?");
            }
        }
        
        private class ChannelGetImpl implements ChannelGet,RecordProcessRequester
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelGetRequester channelGetRequester = null;
            private boolean process;
            private FieldGroupImpl fieldGroup = null;
            private List<ChannelField> channelFieldList;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            
            private RequestResult requestResult = RequestResult.success;
            private Iterator<ChannelField> channelFieldListIter;
            private PVField pvField;
            
            private ChannelGetImpl(ChannelFieldGroup channelFieldGroup,
                ChannelGetRequester channelGetRequester,boolean process,boolean processSelfOK)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                this.channelGetRequester = channelGetRequester;
                this.process = process;
                channelFieldList = fieldGroup.getList();
                requesterName = "ChannelGet:" + channelGetRequester.getRequesterName();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!processSelfOK) {
                            throw new IllegalStateException("already has process requester");
                        }
                        if(!recordProcess.canProcessSelf()) {
                            throw new IllegalStateException(
                                "already has process requester other than self");
                        }
                    }
                }
            }
            
            private void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#get(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void get() {
                if(isDestroyed) return;
                if(!isConnected()) {
                    channelGetRequester.message(
                        "channel is not connected",MessageType.info);
                    channelGetRequester.getDone(RequestResult.failure);
                }
                requestResult = RequestResult.success;
                if(process) {
                    if(isRecordProcessRequester) {
                        if(recordProcess.process(this, true, null)) return;
                    } else {
                        if(recordProcess.processSelfRequest(this)) {
                            recordProcess.processSelfProcess(this, true);
                            return;
                        }
                    }
                    channelGetRequester.message("process failed", MessageType.warning);
                    requestResult = RequestResult.failure;
                }
                startGetData();
                channelGetRequester.getDone(requestResult);
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
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelGetRequester.message(message, messageType);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                startGetData();
                if(process) {
                    if(isRecordProcessRequester) {
                        recordProcess.setInactive(this);
                    } else {
                        recordProcess.processSelfSetInactive(this);
                    }
                }
                channelGetRequester.getDone(RequestResult.success);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            
            private void startGetData() {
                channelFieldList = fieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                getData();
            }
            
            private void getData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(isRecordProcessRequester) recordProcess.setInactive(this);
                            channelGetRequester.getDone(requestResult);
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelGetRequester.nextGetField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelGetRequester.nextDelayedGetField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    }
                }
            }
        }
        
        private class ChannelCDGetImpl implements ChannelCDGet,RecordProcessRequester
        {
            private boolean supportAlso;
            private boolean process;
            private String requesterName;
            private ChannelCDGetRequester channelCDGetRequester = null;
            private List<ChannelField> channelFieldList;
            private CD cD;
            
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            
            private RequestResult requestResult = null;
            
            private ChannelCDGetImpl(Channel channel,ChannelFieldGroup channelFieldGroup,
                ChannelCDGetRequester channelCDGetRequester,boolean supportAlso,
                boolean process,boolean processSelfOK)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.supportAlso = supportAlso;
                this.process = process;
                channelFieldList = channelFieldGroup.getList();
                this.channelCDGetRequester = channelCDGetRequester;
                cD = CDFactory.createCD(channel, channelFieldGroup, supportAlso);
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!processSelfOK) {
                            throw new IllegalStateException("already has process requester");
                        }
                        if(!recordProcess.canProcessSelf()) {
                            throw new IllegalStateException(
                                "already has process requester other than self");
                        }
                    }
                }
                requesterName = "ChannelGet:" + channelCDGetRequester.getRequesterName();
            } 
            
            private void destroy() {
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDGet#get()
             */
            public void get() {
                if(!isConnected()) {
                    channelCDGetRequester.message(
                        "channel is not connected",MessageType.info);
                    channelCDGetRequester.getDone(RequestResult.failure);
                }
                cD.clearNumPuts();
                requestResult = RequestResult.success;
                if(process) {
                    if(isRecordProcessRequester) {
                        if(recordProcess.process(this, true, null)) return;
                    } else {
                        if(recordProcess.processSelfRequest(this)) {
                            recordProcess.processSelfProcess(this, true);
                            return;
                        }
                    } 
                    channelCDGetRequester.message("process failed", MessageType.warning);
                    requestResult = RequestResult.failure;
                }
                dbRecord.lock();
                try {
                    getData();
                } finally {
                    dbRecord.unlock();
                }
                channelCDGetRequester.getDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDGet#getChannelData()
             */
            public CD getCD() {
                return cD;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                getData();
                if(process) {
                    if(isRecordProcessRequester) {
                        recordProcess.setInactive(this);
                    } else {
                        recordProcess.processSelfSetInactive(this);
                    }
                }
                channelCDGetRequester.getDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelCDGetRequester.message(message, messageType);
            }
            
            private void getData() {
                Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
                CDStructure cdStructure = cD.getCDRecord().getCDStructure();
                CDField[] cdFields = cdStructure.getFieldCDFields();
                for(CDField cdField : cdFields) {
                    ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                    copyChanges(field.getDBField(),cdField);
                }
            }
            
            private void copyChanges(DBField dbField,CDField cdField) {
                PVField fromPVField = dbField.getPVField();
                if(supportAlso) {
                    cdField.supportNamePut(fromPVField.getSupportName());
                }

                Field field = fromPVField.getField();
                Type type = field.getType();
                if(type.isScalar()) {
                    cdField.dataPut(fromPVField);
                    return;
                }
                if(type==Type.pvStructure) {
                    CDStructure cdStructure = (CDStructure)cdField;
                    CDField[] cdFields = cdStructure.getFieldCDFields();
                    DBStructure to = (DBStructure)dbField;
                    DBField[] dbFields = to.getFieldDBFields();
                    for(int i=0; i<cdFields.length; i++) {
                        copyChanges(dbFields[i],cdFields[i]);
                    }
                    return;
                }
                if(type==Type.pvArray) {
                    Array array = (Array)field;
                    Type elementType = array.getElementType();
                    if(elementType.isScalar()) {
                        cdField.dataPut(fromPVField);
                        return;
                    }
                    copyNonScalarArray((DBNonScalarArray)dbField,(CDNonScalarArray)cdField);
                }
            }
            private void copyNonScalarArray(DBNonScalarArray dbArray,CDNonScalarArray cdArray) {
                PVArray pvArray = (PVArray)cdArray.getPVField();
                int length = pvArray.getLength();
                CDField[] cdFields = cdArray.getElementCDFields();
                DBField[] dbFields = dbArray.getElementDBFields();
                for(int i=0; i<length; i++) {
                    CDField cdField = cdFields[i];
                    DBField dbField = dbFields[i];
                    if(dbField==null) continue;
                    if(cdField==null) {
                        message("why is cdField null and dbField not null?",MessageType.error);
                    }
                    copyChanges(dbField,cdField);
                }
            }
        }
        
        private class ChannelPutImpl implements ChannelPut,RecordProcessRequester
        {
            private String requesterName;
            private ChannelPutRequester channelPutRequester = null;
            private boolean process;
            private FieldGroupImpl fieldGroup = null;
            
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            
            private RequestResult requestResult = null;
            
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVField pvField;
            private ChannelFieldImpl field;
            
            private ChannelPutImpl(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequester channelPutRequester, boolean process,boolean processSelfOK)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                this.channelPutRequester = channelPutRequester;
                this.process = process;
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!processSelfOK) {
                            throw new IllegalStateException("already has process requester");
                        }
                        if(!recordProcess.canProcessSelf()) {
                            throw new IllegalStateException(
                                "already has process requester other than self");
                        }
                    }
                }
                requesterName = "ChannelPut:" + channelPutRequester.getRequesterName();
            } 
            
            private void destroy() {
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void put() {
                if(isDestroyed) {
                    channelPutRequester.putDone(RequestResult.failure);
                    return;
                }
                if(!isConnected()) {
                    message("channel is not connected",MessageType.info);
                    channelPutRequester.putDone(RequestResult.failure);
                    return;
                }
                if(process) {
                    if(isRecordProcessRequester) {
                        if(recordProcess.setActive(this)) return;
                    } else {
                        if(recordProcess.processSelfRequest(this)) {   
                            recordProcess.processSelfSetActive(this);
                            return;
                        }
                    }
                    message("could not process record",MessageType.info);
                    channelPutRequester.putDone(RequestResult.failure);
                    return;
                }
                startPutData();
                return;
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
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                channelPutRequester.putDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelPutRequester.message(message, messageType);
            }
            
            private void startPutData() {
                channelFieldList = fieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                putData();
            }
            
            private void putData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(isRecordProcessRequester) {
                                recordProcess.process(this, false, null);
                            } else if(process) {
                                recordProcess.processSelfProcess(this, false);
                            }
                            return;
                        }
                        field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelPutRequester.nextPutField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        field.getDBField().postPut();
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutRequester.nextDelayedPutField(pvField);
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
        
        private class ChannelCDPutImpl implements ChannelCDPut,RecordProcessRequester
        {
            private boolean supportAlso;
            private String requesterName;
            private ChannelCDPutRequester channelCDPutRequester = null;
            private boolean process;
            private FieldGroupImpl fieldGroup = null;
            private List<ChannelField> channelFieldList;
            private CD cD;
            
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            
            private RequestResult requestResult = null;
            
            
            
            private ChannelCDPutImpl(Channel channel,ChannelFieldGroup channelFieldGroup,
                ChannelCDPutRequester channelCDPutRequester,boolean supportAlso,
                boolean process, boolean processSelfOK)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.supportAlso = supportAlso;
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                channelFieldList = channelFieldGroup.getList();
                this.channelCDPutRequester = channelCDPutRequester;
                this.process = process;
                cD = CDFactory.createCD(
                     channel, channelFieldGroup, supportAlso);
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!processSelfOK) {
                            throw new IllegalStateException("already has process requester");
                        }
                        if(!recordProcess.canProcessSelf()) {
                            throw new IllegalStateException(
                                "already has process requester other than self");
                        }
                    }
                }
                requesterName = "ChannelPut:" + channelCDPutRequester.getRequesterName();
            } 
            
            private void destroy() {
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDPut#get()
             */
            public void get() {
                if(isDestroyed) {
                    channelCDPutRequester.getDone(RequestResult.failure);
                }
                if(!isConnected()) {
                    channelCDPutRequester.message(
                        "channel is not connected",MessageType.info);
                    channelCDPutRequester.getDone(RequestResult.failure);
                }
                Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
                dbRecord.lock();
                try {
                    channelFieldList = fieldGroup.getList();
                    channelFieldListIter = channelFieldList.iterator();
                    while(channelFieldListIter.hasNext()) {
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        PVField targetPVField = field.getPVField();
                        cD.dataPut(targetPVField);
                    }
                } finally {
                    dbRecord.unlock();
                }
                channelCDPutRequester.getDone(RequestResult.success);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelCDPut#getChannelData()
             */
            public CD getCD() {
                return cD;
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void put() {
                if(isDestroyed) {
                    channelCDPutRequester.putDone(RequestResult.failure);
                }
                if(!isConnected()) {
                    message("channel is not connected",MessageType.info);
                    channelCDPutRequester.putDone(RequestResult.failure);
                    return;
                }
                requestResult = RequestResult.success;
                if(process) {
                    if(isRecordProcessRequester) {
                        if(!recordProcess.setActive(this)) {
                            message("could not process record",MessageType.warning);
                            channelCDPutRequester.putDone(RequestResult.failure);
                            return;
                        }
                    } else {
                        if(recordProcess.processSelfRequest(this)){
                            recordProcess.processSelfSetActive(this);
                        }  else {
                            message("could not process record",MessageType.warning);
                            channelCDPutRequester.putDone(RequestResult.failure);
                            return;
                        }
                    }
                }
                if(!process) {
                    dbRecord.lock();
                    try {
                        putData();
                    } finally {
                        dbRecord.unlock();
                    }
                } else {
                    putData();
                }
                if(process) {
                    if(isRecordProcessRequester) {
                        recordProcess.process(this, false, null);
                    } else {
                        recordProcess.processSelfProcess(this, false);
                    }
                    return;
                }
                channelCDPutRequester.putDone(requestResult);
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                channelCDPutRequester.putDone(requestResult);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelCDPutRequester.message(message, messageType);
            }
            
            private void putData() {
                Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
                CDStructure cdStructure = cD.getCDRecord().getCDStructure();
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
                if(supportAlso && cdField.getNumSupportNamePuts()>0) {
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
        
        private class ChannelPutGetImpl implements ChannelPutGet,RecordProcessRequester
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelPutGetRequester channelPutGetRequester = null;
            private ChannelFieldGroup putFieldGroup = null;
            private ChannelFieldGroup getFieldGroup = null;
            private boolean process;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private RequestResult requestResult = null;
            
            private List<ChannelField> channelFieldList;
            private Iterator<ChannelField> channelFieldListIter;
            private PVField pvField;
            
            
            private ChannelPutGetImpl(
                ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
                ChannelPutGetRequester channelPutGetRequester,boolean process,boolean processSelfOK)
            {
                this.putFieldGroup = putFieldGroup;
                this.getFieldGroup = getFieldGroup;
                this.channelPutGetRequester = channelPutGetRequester;
                this.process = process;
                requesterName = "ChannelGetPut:" + channelPutGetRequester.getRequesterName();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!processSelfOK) {
                            throw new IllegalStateException("already has process requester");
                        }
                        if(!recordProcess.canProcessSelf()) {
                            throw new IllegalStateException(
                                "already has process requester other than self");
                        }
                    }
                }
            }
            
            private void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester)recordProcess.releaseRecordProcessRequester(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void putGet()
            {
                if(isDestroyed) {
                    channelPutGetRequester.putDone(RequestResult.failure);
                }
                if(!isConnected()) {
                    channelPutGetRequester.message(
                        "channel is not connected",MessageType.info);
                    channelPutGetRequester.putDone(RequestResult.failure);
                    channelPutGetRequester.getDone(RequestResult.failure);
                    return;
                }
                requestResult = RequestResult.success;
                if(process) {
                    if(isRecordProcessRequester) {
                        if(!recordProcess.setActive(this)) return;
                    } else {
                        if(!recordProcess.processSelfRequest(this)) {
                            channelPutGetRequester.message(
                                    "could not process record",MessageType.warning);
                                channelPutGetRequester.putDone(RequestResult.failure);
                                channelPutGetRequester.getDone(RequestResult.failure);
                                return;
                        }
                        recordProcess.processSelfSetActive(this);
                    }
                }
                startPutData();
                channelPutGetRequester.putDone(RequestResult.success);
                if(process) {
                    if(isRecordProcessRequester) {
                        recordProcess.process(this, true, null);
                    } else {
                        recordProcess.processSelfProcess(this, true);
                    }
                    return;
                }
                startGetData();
                channelPutGetRequester.getDone(RequestResult.failure);
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
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                startGetData();
                channelPutGetRequester.getDone(requestResult);
                if(isRecordProcessRequester) {
                    recordProcess.setInactive(this);
                } else {
                    recordProcess.processSelfSetInactive(this);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requesterName;
            }     
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelPutGetRequester.message(message, messageType);
            }
            
            private void startPutData() {
                channelFieldList = putFieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                putData();
            }
            
            private void putData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            channelPutGetRequester.putDone(RequestResult.success);
                            if(isRecordProcessRequester) {
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
                            more = channelPutGetRequester.nextPutField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequester.nextDelayedPutField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    }
                }
                
            }
            
            private void startGetData() {
                channelFieldList = getFieldGroup.getList();
                channelFieldListIter = channelFieldList.iterator();
                pvField = null;
                getData();
            }
           
            private void getData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(!channelFieldListIter.hasNext()) {
                            if(process) {
                                if(isRecordProcessRequester) {
                                    recordProcess.setInactive(this);
                                } else {
                                    recordProcess.processSelfSetInactive(this);
                                }
                            }
                            channelPutGetRequester.getDone(requestResult);
                            return;
                        }
                        ChannelFieldImpl field = (ChannelFieldImpl)channelFieldListIter.next();
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequester.nextGetField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutGetRequester.nextDelayedGetField(pvField);
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
        ChannelFieldGroupListener,ChannelMonitor,DBListener,Requester
        {
            private Channel channel;
            boolean onlyWhileProcesing;
            boolean supportAlso;
            private Requester requester;
            private Monitor monitor = null;
            private boolean isStarted = false;
            private RecordListener recordListener = null;
            private boolean processActive = false;
            private ChannelMonitorRequester channelMonitorRequester;
            private ChannelFieldGroup channelFieldGroup = null;
            private CDQueue cDQueue = null;
            private MonitorThread monitorThread;
            private CD cD = null;
            private boolean monitorOccured = false;
            
            private ChannelMonitorImpl(
                boolean onlyWhileProcesing,boolean supportAlso,
                Channel channel,Requester requester)
            {
                this.channel = channel;
                this.supportAlso = supportAlso;
                this.onlyWhileProcesing = onlyWhileProcesing;
                this.requester = requester;
                this.channelFieldGroup = channel.createFieldGroup(this);
                monitor = new Monitor(this);
            }           
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                requester.message(message, messageType);
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
             * @see org.epics.ioc.ca.ChannelMonitor#lookForChange(org.epics.ioc.ca.ChannelField)
             */
            public void lookForPut(ChannelField channelField, boolean causeMonitor) {
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
             * @see org.epics.ioc.ca.ChannelMonitor#lookForChange(org.epics.ioc.ca.ChannelField, boolean)
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
                        monitor.onChange(impl,causeMonitor);
                    }
                } finally {
                    lock.unlock();
                }
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
            public boolean start(ChannelMonitorNotifyRequester channelMonitorNotifyRequester,
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
                        channelMonitorRequester = null;
                        recordListener = dbRecord.createRecordListener(this);
                        List<ChannelFieldImpl> channelFieldList = monitor.getChannelFieldList();
                        if(threadName==null) threadName =
                            channelMonitorNotifyRequester.getRequesterName() + "NotifyThread";
                        int priority = scanPriority.getJavaPriority();
                        monitorThread = new MonitorThread(
                             threadName,priority,channelMonitorNotifyRequester);
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBField dbField = channelField.getDBField();
                            PVField pvField = dbField.getPVField();
                            monitor.initField(pvField);
                            dbField.addListener(recordListener);
                        }
                        monitor.start();
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
             * @see org.epics.ioc.ca.ChannelMonitor#start(org.epics.ioc.ca.ChannelMonitorRequester)
             */
            public boolean start(ChannelMonitorRequester channelMonitorRequester,
                int queueSize, String threadName, ScanPriority scanPriority)
            {
                lock.lock();
                try {
                    if(isDestroyed) {
                        message("channel has been destroyed",MessageType.fatalError);
                        return false;
                    } else if(isStarted) {
                        message("alreadt started",MessageType.fatalError);
                        return false;
                    } else {
                        this.channelMonitorRequester = channelMonitorRequester;
                        channelFieldGroup = channel.createFieldGroup(this);
                        List<ChannelFieldImpl> channelFieldList = monitor.getChannelFieldList();
                        for(ChannelField channelField: channelFieldList) {
                            channelFieldGroup.addChannelField(channelField);
                        }
                        cDQueue = CDFactory.createCDQueue(
                            queueSize, channel, channelFieldGroup,supportAlso);
                        if(threadName==null) threadName =
                            channelMonitorRequester.getRequesterName() + "NotifyThread";
                        int priority = scanPriority.getJavaPriority();
                        monitorThread = new MonitorThread(
                            threadName,priority,channelMonitorRequester,cDQueue);                        
                        recordListener = dbRecord.createRecordListener(this);
                        CD initialData = cDQueue.getFree(true);
                        initialData.clearNumPuts();
                        // give the initial data to the user
                        for(ChannelFieldImpl channelField: channelFieldList) {
                            DBField dbField = channelField.getDBField();
                            PVField pvField = dbField.getPVField();
                            initialData.dataPut(pvField);
                            monitor.initField(pvField);
                        }
                        monitor.start();
                        cD = cDQueue.getFree(true);
                        CDField[] initialDatas = initialData.getCDRecord().getCDStructure().getFieldCDFields();
                        CDField[] channelDatas = cD.getCDRecord().getCDStructure().getFieldCDFields();
                        for(int i=0; i<initialDatas.length; i++) {
                            channelDatas[i].dataPut(initialDatas[i].getPVField());
                        }
                        cDQueue.setInUse(initialData);
                        monitorThread.signal();
                        cD.clearNumPuts();
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
                notifyRequester();
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
                if(channelMonitorRequester!=null) {
                    cDQueue = null;
                    cD = null;
                    monitorThread.stop();
                }
                channelMonitorRequester = null;
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
                notifyRequester();
                processActive = false;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.db.DBStructure)
             */
            public void beginPut(DBStructure dbStructure) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                if(cD!=null) cD.beginPut(dbStructure.getPVStructure());
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.db.DBStructure)
             */
            public void endPut(DBStructure dbStructure) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(channelMonitorRequester!=null) {
                    if(cD!=null) cD.dataPut(dbStructure.getPVStructure());
                }                    
                if(!processActive) notifyRequester();
            }             
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
             */
            public void dataPut(DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                ChannelField channelField = monitor.newField(dbField.getPVField());
                if(channelField==null) return;
                if(channelMonitorRequester!=null) {
                    if(cD!=null) cD.dataPut(dbField.getPVField());
                }                    
                if(!processActive) notifyRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField)
             */
            public void supportNamePut(DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(cD!=null) cD.supportNamePut(dbField.getPVField());
                if(!processActive) notifyRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
             */
            public void dataPut(DBField requested, DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(cD!=null) cD.dataPut(requested.getPVField(), dbField.getPVField());              
                if(!processActive) notifyRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
             */
            public void supportNamePut(DBField requested,DBField dbField) {
                if(!isStarted) return;
                if(onlyWhileProcesing && !processActive) return;
                monitorOccured = true;
                if(cD!=null) cD.supportNamePut(requested.getPVField(),dbField.getPVField());
                if(!processActive) notifyRequester();
            }

            private void notifyRequester() {
                List<MonitorField> list = monitor.getMonitorFieldList();
                for(MonitorField field : list) {
                    if(field.monitorOccured()) {
                        field.clearMonitor();
                        if(field.causeMonitor()) monitorOccured = true;
                    }
                }
                if(!monitorOccured) return;
                monitorOccured = false;
                if(channelMonitorRequester!=null) {
                    if(cD!=null) {
                        CD initialCD = cD;
                        cD = cDQueue.getFree(true);
                        CDField[] initialDatas = initialCD.getCDRecord().getCDStructure().getFieldCDFields();
                        CDField[] channelDatas = cD.getCDRecord().getCDStructure().getFieldCDFields();
                        for(int i=0; i<initialDatas.length; i++) {
                            channelDatas[i].dataPut(initialDatas[i].getPVField());
                        }
                        cD.clearNumPuts();
                        cDQueue.setInUse(initialCD);
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
        
        private enum MonitorType {
            onPut,
            onChange,
            absoluteChange,
            percentageChange
        }
        
        private static class MonitorField {
            private MonitorType monitorType;
            private Type type = null;
            private boolean causeMonitor;
            private boolean monitorOccured = false;
            
            private boolean lastBooleanValue;
            private byte lastByteValue;
            private short lastShortValue;
            private int lastIntValue;
            private long lastLongValue;
            private float lastFloatValue;
            private double lastDoubleValue;
            private String lastStringValue;
            
            private double deadband;
            private double lastMonitorValue = 0.0;
    
            private MonitorField(boolean causeMonitor) {
                this.monitorType = MonitorType.onPut;
                this.causeMonitor = causeMonitor;
            }
            private MonitorField(Type type,boolean causeMonitor) {
                this.monitorType = MonitorType.onChange;
                this.type = type;
                this.causeMonitor = causeMonitor;
            }
            private MonitorField(MonitorType monitorType, Type type, double deadband) {
                causeMonitor = true;
                this.monitorType = monitorType;
                this.type = type;
                this.deadband = deadband;
            }
            
            private void initField(PVField pvField) {
                if(monitorType==MonitorType.onPut) return;
                if(monitorType==MonitorType.onChange) {
                    switch(type) {
                    case pvBoolean: {
                            PVBoolean data= (PVBoolean)pvField;
                            lastBooleanValue = data.get();
                            return;
                        }
                    case pvByte: {
                            PVByte data= (PVByte)pvField;
                            lastByteValue = data.get();
                            return;
                        }
                    case pvShort: {
                            PVShort data= (PVShort)pvField;
                            lastShortValue = data.get();
                            return;
                        }
                    case pvInt: {
                            PVInt data= (PVInt)pvField;
                            lastIntValue = data.get();
                            return;
                        }
                    case pvLong: {
                            PVLong data= (PVLong)pvField;
                            lastLongValue = data.get();
                            return;
                        }
                    case pvFloat: {
                            PVFloat data= (PVFloat)pvField;
                            lastFloatValue = data.get();
                            return;
                        }
                    case pvDouble: {
                            PVDouble data= (PVDouble)pvField;
                            lastDoubleValue = data.get();
                            return;
                        }
                    case pvString : {
                        PVString data= (PVString)pvField;
                        lastStringValue = data.get();
                        return;
                    }                       
                    default:
                        throw new IllegalStateException("Logic error. Why is type not numeric?");      
                    }
                }
                lastMonitorValue = convert.toDouble(pvField);
            }
            private void start() {
                clearMonitor();
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
            private boolean newField(PVField pvField) {
                if(monitorType==MonitorType.onPut) {
                    monitorOccured = true;
                    return true;
                }
                if(monitorType==MonitorType.onChange) {
                    switch(type) {
                    case pvBoolean: {
                            PVBoolean pvData= (PVBoolean)pvField;
                            boolean data = pvData.get();
                            if(data==lastBooleanValue) return false;
                            lastBooleanValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvByte: {
                            PVByte pvData= (PVByte)pvField;
                            byte data = pvData.get();
                            if(data==lastByteValue) return false;
                            lastByteValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvShort: {
                            PVShort pvData= (PVShort)pvField;
                            short data = pvData.get();
                            if(data==lastShortValue) return false;
                            lastShortValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvInt: {
                            PVInt pvData= (PVInt)pvField;
                            int data = pvData.get();
                            if(data==lastIntValue) return false;
                            lastIntValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvLong: {
                            PVLong pvData= (PVLong)pvField;
                            long data = pvData.get();
                            if(data==lastLongValue) return false;
                            lastLongValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvFloat: {
                            PVFloat pvData= (PVFloat)pvField;
                            float data = pvData.get();
                            if(data==lastFloatValue) return false;
                            lastFloatValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvDouble: {
                            PVDouble pvData= (PVDouble)pvField;
                            double data = pvData.get();
                            if(data==lastDoubleValue) return false;
                            lastDoubleValue = data;
                            monitorOccured = true;
                            return true;
                        }
                    case pvString : {
                            PVString pvData= (PVString)pvField;
                            String data = pvData.get();
                            if(data==lastStringValue) return false;
                            if(data.equals(lastStringValue)) return false;
                            lastStringValue = data;
                            monitorOccured = true;
                            return true;
                        }                       
                    default:
                        throw new IllegalStateException("Logic error. Why is type invalid?");      
                    }
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
            private Requester requester;
            private ArrayList<MonitorField> monitorFieldList
                = new ArrayList<MonitorField>();
            private ArrayList<ChannelFieldImpl> channelFieldList
                = new ArrayList<ChannelFieldImpl>();
            
            private Monitor(Requester requester) {
                this.requester = requester;
            }
            private List<MonitorField> getMonitorFieldList() {
                return monitorFieldList;
            }
            private List<ChannelFieldImpl> getChannelFieldList() {
                return channelFieldList;
            }
            
            private void onPut(ChannelFieldImpl channelField,boolean causeMonitor) {
                MonitorField monitorField = new MonitorField(causeMonitor);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
            }
            private void onChange(ChannelFieldImpl channelField,boolean causeMonitor) {
                Type type = channelField.getField().getType();
                if(!type.isPrimitive()) {
                    requester.message("field is not primitive", MessageType.error);
                    onPut(channelField,true);
                    return;
                }
                MonitorField monitorField = new MonitorField(type,causeMonitor);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
            }
            private void onAbsoluteChange(ChannelFieldImpl channelField, double value) {
                Type type = channelField.getField().getType();
                if(!type.isNumeric()) {
                    requester.message("field is not a numeric scalar", MessageType.error);
                    onPut(channelField,true);
                    return;
                }
                MonitorField monitorField
                    = new MonitorField(MonitorType.absoluteChange,type,value);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
            }         
            private void onPercentageChange(ChannelFieldImpl channelField, double value) {
                Type type = channelField.getField().getType();
                if(!type.isNumeric()) {
                    requester.message("field is not a numeric scalar", MessageType.error);
                    onPut(channelField,true);
                    return;
                }
                MonitorField monitorField
                    = new MonitorField(MonitorType.percentageChange,type,value);
                monitorFieldList.add(monitorField);
                channelFieldList.add(channelField);
            }
            private void start() {
                for(MonitorField monitorField: monitorFieldList) {
                    monitorField.start();
                }
            }
            
            private ChannelField initField(PVField pvField) {
                for(int i=0; i < channelFieldList.size(); i++) {
                    ChannelFieldImpl channelField = channelFieldList.get(i);
                    PVField data = channelField.getPVField();
                    if(data==pvField) {
                        MonitorField monitorField = monitorFieldList.get(i);
                        monitorField.initField(pvField);
                        return null;
                    }
                }
                return null;
            }
            private ChannelField newField(PVField pvField) {
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
            private ChannelMonitorNotifyRequester channelMonitorNotifyRequester;
            private ChannelMonitorRequester channelMonitorRequester;
            private CDQueue cDQueue;
            private Thread thread = null;
            private ReentrantLock lock = new ReentrantLock();
            private Condition moreWork = lock.newCondition();
            private boolean isRunning = false;

            private MonitorThread(
            String name,int priority,
            ChannelMonitorRequester channelMonitorRequester,
            CDQueue cDQueue)
            {
                channelMonitorNotifyRequester = null;
                this.channelMonitorRequester = channelMonitorRequester;
                this.cDQueue = cDQueue;
                thread = new Thread(this,name);
                thread.setPriority(priority);
                thread.start();
                while(!isRunning) {
                    try {
                    Thread.sleep(1);
                    } catch(InterruptedException e) {}
                }
            } 
            
            private MonitorThread(
            String name,int priority,
            ChannelMonitorNotifyRequester channelMonitorNotifyRequester)
            {
                this.channelMonitorNotifyRequester = channelMonitorNotifyRequester;
                channelMonitorRequester = null;
                cDQueue = null;
                thread = new Thread(this,name);
                thread.setPriority(priority);
                thread.start();
                while(!isRunning) {
                    try {
                    Thread.sleep(1);
                    } catch(InterruptedException e) {}
                }
            }            
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                isRunning = true;
                try {
                    while(true) {
                        CD cD = null;
                        lock.lock();
                        try {
                            while(true) {
                                if(cDQueue!=null) {
                                    cD = cDQueue.getNext();
                                    if(cD!=null) break;
                                } 
                                moreWork.await();
                                if(channelMonitorNotifyRequester!=null) break;
                            }
                        }finally {
                            lock.unlock();
                        }
                        if(cD!=null) {
                            int missed = cDQueue.getNumberMissed();
                            if(missed>0) channelMonitorRequester.dataOverrun(missed);
                            channelMonitorRequester.monitorCD(cD);
                            cDQueue.releaseNext(cD);
                        } else if(channelMonitorNotifyRequester!=null){
                            channelMonitorNotifyRequester.monitorEvent();
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
