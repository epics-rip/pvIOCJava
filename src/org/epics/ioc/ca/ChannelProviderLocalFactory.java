/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBListener;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.RecordListener;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.RecordProcessRequester;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;

/**
 * Factory and implementation of local channel access, i.e. channel access that
 * accesses database records in the local IOC.
 * All user callbacks will be called with the appropriate records locked except for
 * 1) all methods of ChannelStateListener, 2) all methods of ChannelFieldGroupListener,
 * and 3) ChannelRequester.requestDone
 * @author mrk
 *
 */
public class ChannelProviderLocalFactory  {
    private static ChannelProviderLocal channelAccess = new ChannelProviderLocal();
    /**
     * Register. This is called by ChannelFactory.
     */
    static public void register() {
        channelAccess.register();
    }
    
    private static class ChannelProviderLocal implements ChannelProvider{
        private static boolean isRegistered = false; 
        private static IOCDB iocdb = IOCDBFactory.getMaster();
        private static final String providerName = "local";

        synchronized void register() {
            if(isRegistered) return;
            isRegistered = true;
            ChannelFactory.registerChannelProvider(this);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#createChannel(java.lang.String, org.epics.ioc.ca.ChannelStateListener)
         */
        public synchronized Channel createChannel(String pvName,ChannelStateListener listener) {
            String recordName = null;
            String fieldName = null;
            String options = null;
            String[] names = periodPattern.split(pvName,2);
            recordName = names[0];
            if(names.length==2) {
                names = leftBracePattern.split(names[1], 2);
                fieldName = names[0];
                if(fieldName.length()==0) fieldName = null;
                if(names.length==2) {
                    names = rightBracePattern.split(names[1], 2);
                    options = names[0];
                }
            }
            DBRecord dbRecord = iocdb.findRecord(recordName);
            if(dbRecord==null) return null;
            if(fieldName!=null) {
                PVField pvField = dbRecord.getPVRecord().findProperty(fieldName);
                if(pvField==null) return null;
            }
            return new ChannelImpl(dbRecord,listener,fieldName,options);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#getProviderName()
         */
        public String getProviderName() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#isProvider(java.lang.String)
         */
        public boolean isProvider(String channelName) {            
            int index = channelName.indexOf('.');
            String recordName = channelName;
            if(index>=0) recordName = channelName.substring(0,index);
            if(iocdb.findRecord(recordName)==null) return false;
            return true;
        }
    }
    
    static private Pattern periodPattern = Pattern.compile("[.]");
    static private Pattern leftBracePattern = Pattern.compile("[{]");
    static private Pattern rightBracePattern = Pattern.compile("[}]");
    
    private static class ChannelImpl implements Channel,Requester {
        private boolean isDestroyed = false;
        private ChannelStateListener stateListener = null;
        private DBRecord dbRecord;
        private PVRecord pvRecord;
        private String channelName;
        private String fieldName;
        private String options;
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
        private LinkedList<MonitorImpl> monitorList = 
            new LinkedList<MonitorImpl>();
        
        private ChannelImpl(DBRecord record,ChannelStateListener listener,
                String fieldName, String options)
        {
            stateListener = listener;
            dbRecord = record;
            pvRecord = record.getPVRecord();
            this.fieldName = fieldName;
            this.options = options;
            if(fieldName==null) {
                channelName = pvRecord.getRecordName();
            } else {
                channelName =  pvRecord.getRecordName() + "." + fieldName;
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#getChannelName()
         */
        public synchronized String getChannelName() {
            return channelName;
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
         * @see org.epics.ioc.ca.Channel#getChannelStateListener()
         */
        public ChannelStateListener getChannelStateListener() {
            return stateListener;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy()
         */
        public void destroy() {
            ChannelStateListener stateListener = destroyPvt();
            if(stateListener!=null) stateListener.disconnect(this);
        }
        
        private synchronized ChannelStateListener destroyPvt() {
            if(isDestroyed) return null;
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
            Iterator<MonitorImpl> monitorIter = monitorList.iterator();
            while(monitorIter.hasNext()) {
                MonitorImpl impl = monitorIter.next();
                impl.destroy();
                monitorIter.remove();
            }
            isDestroyed = true;
            return stateListener;
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelGet)
         */
        public synchronized void destroy(ChannelGet get) {
            ChannelGetImpl toDelete = (ChannelGetImpl)get;            
            Iterator<ChannelGetImpl> getIter = channelGetList.iterator();
            while(getIter.hasNext()) {
                ChannelGetImpl channelGet = getIter.next();
                if(channelGet==toDelete) {
                    channelGet.destroy();
                    getIter.remove();
                    return;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelProcess)
         */
        public synchronized void destroy(ChannelProcess process) {
            ChannelProcessImpl toDelete = (ChannelProcessImpl)process;
            Iterator<ChannelProcessImpl> processIter = channelProcessList.iterator();
            while(processIter.hasNext()) {
                ChannelProcessImpl channelProcess = processIter.next();
                if(channelProcess==toDelete) {
                    channelProcess.destroy();
                    processIter.remove();
                    return;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelPut)
         */
        public synchronized void destroy(ChannelPut put) {
            ChannelPutImpl toDelete = (ChannelPutImpl)put;
            Iterator<ChannelPutImpl> putIter = channelPutList.iterator();
            while(putIter.hasNext()) {
                ChannelPutImpl channelPut = putIter.next();
                if(channelPut==toDelete) {
                    channelPut.destroy();
                    putIter.remove();
                    return;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelPutGet)
         */
        public synchronized void destroy(ChannelPutGet putGet) {
            ChannelPutGetImpl toDelete = (ChannelPutGetImpl)putGet;
            Iterator<ChannelPutGetImpl> putGetIter = channelPutGetList.iterator();
            while(putGetIter.hasNext()) {
                ChannelPutGetImpl channelPutGet = putGetIter.next();
                if(channelPutGet==toDelete) {
                    channelPutGet.destroy();
                    putGetIter.remove();
                    return;
                }
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#destroy(org.epics.ioc.ca.ChannelMonitor)
         */
        public synchronized void destroy(ChannelMonitor channelMonitor) {
            MonitorImpl toDelete = (MonitorImpl)channelMonitor;
            Iterator<MonitorImpl> iter = monitorList.iterator();
            while(iter.hasNext()) {
                MonitorImpl impl = iter.next();
                if(impl==toDelete) {
                    impl.destroy();
                    iter.remove();
                    return;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#isConnected()
         */
        public synchronized boolean isConnected() {
            if(isDestroyed) {
                return false;
            } else {
                return true;
            }
        }                       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelField(java.lang.String)
         */
        public synchronized ChannelField createChannelField(String name) {
            if(isDestroyed) return null;
            if(name==null || name.length()<=0) return new ChannelFieldImpl(dbRecord.getDBStructure());
            PVField pvField = pvRecord.findProperty(name);
            if(pvField==null) return null;
            return new ChannelFieldImpl(dbRecord.findDBField(pvField));               
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#getFieldName()
         */
        public synchronized String getFieldName() {
            return fieldName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#getOptions()
         */
        public synchronized String getOptions() {
            return options;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#getPropertyName()
         */
        public synchronized String getPropertyName() {
            if(fieldName==null||fieldName.length()<=0) return "value";
            PVField pvField = pvRecord.findProperty(fieldName);
            if(pvField!=null && pvField.getField().getType()==Type.pvStructure) {
                PVStructure pvStructure = (PVStructure)pvField;
                if(pvStructure.getStructure().getFieldIndex("value") >=0) {
                    return fieldName + ".value";
                }
            }
            return fieldName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createFieldGroup(org.epics.ioc.ca.ChannelFieldGroupListener)
         */
        public synchronized FieldGroupImpl createFieldGroup(ChannelFieldGroupListener listener) {
            if(isDestroyed) return null;
            FieldGroupImpl fieldGroupImpl = new FieldGroupImpl(this,listener);
            fieldGroupList.add(fieldGroupImpl);
            return fieldGroupImpl;
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelProcess(org.epics.ioc.ca.ChannelProcessRequester)
         */
        public synchronized ChannelProcess createChannelProcess(ChannelProcessRequester channelProcessRequester)
        {
            if(isDestroyed) {
                channelProcessRequester.message(
                        "channel has been destroyed",MessageType.fatalError);
                return null;
            }
            ChannelProcessImpl channelProcess;
            try {
                channelProcess = new ChannelProcessImpl(channelProcessRequester);
                channelProcessList.add(channelProcess);
            } catch(IllegalStateException e) {
                channelProcessRequester.message(
                        e.getMessage(),MessageType.fatalError);
                return null;
            }
            return channelProcess;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelGetRequester, boolean)
         */
        public synchronized ChannelGet createChannelGet(ChannelFieldGroup channelFieldGroup,
                ChannelGetRequester channelGetRequester, boolean process)
        {
            if(isDestroyed) return null;
            ChannelGetImpl channelGet = 
                new ChannelGetImpl(channelFieldGroup,channelGetRequester,process);
            channelGetList.add(channelGet);
            return channelGet;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester, boolean)
         */
        public synchronized ChannelPut createChannelPut(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequester channelPutRequester, boolean process)
        {
            if(isDestroyed) return null;
            ChannelPutImpl channelPut = 
                new ChannelPutImpl(channelFieldGroup,channelPutRequester,process);
            channelPutList.add(channelPut);
            return channelPut;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester, boolean)
         */
        public synchronized ChannelPutGet createChannelPutGet(ChannelFieldGroup putFieldGroup,
            ChannelFieldGroup getFieldGroup, ChannelPutGetRequester channelPutGetRequester,
            boolean process)
        {
            if(isDestroyed) return null;
            ChannelPutGetImpl channelPutGet = 
                new ChannelPutGetImpl(putFieldGroup,getFieldGroup,
                        channelPutGetRequester,process);
            channelPutGetList.add(channelPutGet);
            return channelPutGet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequester, boolean)
         */
        public synchronized ChannelMonitor createChannelMonitor(ChannelMonitorRequester channelMonitorRequester)
        {
            if(isDestroyed) {
                stateListener.message(
                        "channel has been destroyed",MessageType.fatalError);
                return null;
            }
            MonitorImpl impl = new MonitorImpl(this,channelMonitorRequester);
            monitorList.add(impl);
            return impl;
        }

    
        private static class ChannelFieldImpl implements ChannelField {
            private DBField dbField = null;
            private PVField pvField;
            
            ChannelFieldImpl(DBField dbField) {
                this.dbField = dbField;
                pvField = dbField.getPVField();
            }        
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getPVField()
             */
            public PVField getPVField() {
                return pvField;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#postPut()
             */
            public void postPut() {
                dbField.postPut();
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
             * @see org.epics.ioc.ca.ChannelField#createChannelField(java.lang.String)
             */
            public ChannelField createChannelField(String fieldName) {
                PVField pvf = pvField.getSubField(fieldName);
                if(pvf==null) return null;
                DBField dbf = dbField.getDBRecord().findDBField(pvf);
                return new ChannelFieldImpl(dbf);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelField#getEnumerated()
             */
            public PVEnumerated getEnumerated() {
                Create create = dbField.getCreate();
                if (create instanceof Enumerated) {
                    return (Enumerated)create;
                }
                return null;
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
           
            /* (non-Javadoc)
             * @see java.lang.Object#toString()
             */
            public String toString() {
                return pvField.getField().toString();
            }
    
        }
        
        private static class FieldGroupImpl implements ChannelFieldGroup {
            private Channel channel;
            private LinkedList<ChannelField> fieldList = 
                new LinkedList<ChannelField>();
    
            FieldGroupImpl(Channel channel,ChannelFieldGroupListener listener) {
                this.channel = channel;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#addChannelField(org.epics.ioc.ca.ChannelField)
             */
            public synchronized void addChannelField(ChannelField channelField) {
                fieldList.add(channelField);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#removeChannelField(org.epics.ioc.ca.ChannelField)
             */
            public synchronized void removeChannelField(ChannelField channelField) {
                fieldList.remove(channelField);
            }            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#getList()
             */
            public synchronized List<ChannelField> getList() {
                return fieldList;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroup#getArray()
             */
            public ChannelField[] getArray() {
                int length = fieldList.size();
                ChannelField[] channelFields = new ChannelField[length];
                for(int i=0; i<length; i++) {
                    channelFields[i] = fieldList.get(i);
                }
                return channelFields;
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
                 
            private ChannelProcessImpl(ChannelProcessRequester channelProcessRequester)
            {
                this.channelProcessRequester = channelProcessRequester;
                recordProcess = dbRecord.getRecordProcess();
                isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester && !recordProcess.canProcessSelf()) {
                    throw new IllegalStateException(
                            "already has process requester other than self");
                }
                requesterName = "Process:" + channelProcessRequester.getRequesterName();
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
                ChannelGetRequester channelGetRequester,boolean process)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.fieldGroup = (FieldGroupImpl)channelFieldGroup;
                this.channelGetRequester = channelGetRequester;
                this.process = process;
                channelFieldList = fieldGroup.getList();
                requesterName = "Get:" + channelGetRequester.getRequesterName();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester && !recordProcess.canProcessSelf()) {
                        channelGetRequester.message(
                                "already has process requester other than self",MessageType.warning);
                        this.process = false;
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
                        if(!channelFieldListIter.hasNext()) return;
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
        
        private class ChannelPutImpl implements ChannelPut,RecordProcessRequester
        {
            private String requesterName;
            private ChannelPutRequester channelPutRequester = null;
            private boolean process;
            
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            
            private RequestResult requestResult = null;
                       
            private ChannelField[] channelFields;
            private PVField pvField;
            private int fieldIndex;
            
            private ChannelPutImpl(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequester channelPutRequester, boolean process)
            {
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("no field group");
                }
                this.channelPutRequester = channelPutRequester;
                this.process = process;
                channelFields = channelFieldGroup.getArray();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester && !recordProcess.canProcessSelf()) {
                        channelPutRequester.message(
                                "already has process requester other than self",MessageType.warning);
                        this.process = false;
                    }
                }
                requesterName = "Put:" + channelPutRequester.getRequesterName();
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
                        if(!recordProcess.setActive(this)) {
                            message("could not process record",MessageType.warning);
                            channelPutRequester.putDone(RequestResult.failure);
                            return;
                        }
                    } else {
                        if(recordProcess.processSelfRequest(this)){
                            recordProcess.processSelfSetActive(this);
                        }  else {
                            message("could not process record",MessageType.warning);
                            channelPutRequester.putDone(RequestResult.failure);
                            return;
                        }
                    }
                    startPutData();
                    return;
                }               
                startPutData();
                channelPutRequester.putDone(requestResult);
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
                fieldIndex = 0;
                pvField = null;
                putData();                
            }
            
            private void putData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(fieldIndex>=channelFields.length) {
                            if(isRecordProcessRequester) {
                                recordProcess.process(this, false, null);
                            } else if(process) {
                                recordProcess.processSelfProcess(this, false);
                            }
                            return;
                        }
                        ChannelField field = channelFields[fieldIndex++];
                        pvField = field.getPVField();
                        dbRecord.lock();
                        try {
                            more = channelPutRequester.nextPutField(field,pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    } else {
                        dbRecord.lock();
                        try {
                            more = channelPutRequester.nextDelayedPutField(pvField);
                        } finally {
                            dbRecord.unlock();
                        }
                        if(more) return;
                        pvField = null;
                    }
                }
            }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,RecordProcessRequester
        {
            private boolean isDestroyed = false;
            private String requesterName;
            private ChannelPutGetRequester channelPutGetRequester = null;
            private boolean process;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private RequestResult requestResult = null;
            
            private ChannelField[] getChannelFields;
            private ChannelField[] putChannelFields;
            private PVField pvField;
            private int fieldIndex;
                 
            private ChannelPutGetImpl(
                ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
                ChannelPutGetRequester channelPutGetRequester,boolean process)
            {
                this.channelPutGetRequester = channelPutGetRequester;
                this.process = process;
                getChannelFields = getFieldGroup.getArray();
                putChannelFields = putFieldGroup.getArray();
                requesterName = "ChannelGetPut:" + channelPutGetRequester.getRequesterName();
                if(process) {
                    recordProcess = dbRecord.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester && !recordProcess.canProcessSelf()) {
                        channelPutGetRequester.message(
                                "already has process requester other than self",MessageType.warning);
                        this.process = false;
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
                requestResult = RequestResult.success;
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
                        if(!recordProcess.setActive(this)) 
                            message("could not process record",MessageType.warning);
                            channelPutGetRequester.putDone(RequestResult.failure);
                            channelPutGetRequester.getDone(RequestResult.failure);
                            
                            return;
                    } else {
                        if(recordProcess.processSelfRequest(this)){
                            recordProcess.processSelfSetActive(this);
                        }  else {
                            message("could not process record",MessageType.warning);
                            channelPutGetRequester.putDone(RequestResult.failure);
                            channelPutGetRequester.getDone(RequestResult.failure);
                            return;
                        }
                    }
                    startPutData();
                    return;
                }
                startPutData();
                startGetData();
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
                if(isRecordProcessRequester) {
                    recordProcess.setInactive(this);
                } else {
                    recordProcess.processSelfSetInactive(this);
                }
                channelPutGetRequester.getDone(requestResult);
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
                fieldIndex = 0;
                pvField = null;
                putData();
                channelPutGetRequester.putDone(requestResult);
            }
            
            private void putData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(fieldIndex>=putChannelFields.length) {
                            if(isRecordProcessRequester) {
                                recordProcess.process(this, true, null);
                            } else if(process) {
                                recordProcess.processSelfProcess(this, true);
                            }
                            return;
                        }
                        ChannelField field = putChannelFields[fieldIndex++];
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
                fieldIndex = 0;
                pvField = null;
                getData();
                if(process) {
                    if(isRecordProcessRequester) {
                        recordProcess.setInactive(this);
                    } else {
                        recordProcess.processSelfSetInactive(this);
                    }
                }
            }
           
            private void getData() {
                boolean more;
                while(true) {
                    if(pvField==null) {
                        if(fieldIndex>=getChannelFields.length) {
                            if(process) {
                                if(isRecordProcessRequester) {
                                    recordProcess.setInactive(this);
                                } else {
                                    recordProcess.processSelfSetInactive(this);
                                }
                            }
                            return;
                        }
                        ChannelField field = getChannelFields[fieldIndex++];
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
        
        private class MonitorImpl implements
        ChannelFieldGroupListener,Requester,ChannelMonitor,DBListener
        {
            private Channel channel;
            private ChannelMonitorRequester channelMonitorRequester;
            private boolean isStarted = false;
            private ChannelFieldGroup channelFieldGroup = null;
            private RecordListener recordListener = null;
            private boolean processActive = false;
            private boolean putStructureActive = false;
            
            
            private MonitorImpl(Channel channel,ChannelMonitorRequester channelMonitorRequester) {
                this.channel = channel;
                this.channelMonitorRequester = channelMonitorRequester;
            }
            private void destroy() {
                stop();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
             */
            public void accessRightsChange(Channel channel, ChannelField channelField) {
                // TODO Auto-generated method stub
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return channelMonitorRequester.getRequesterName();
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(String message, MessageType messageType) {
                channelMonitorRequester.message(message, messageType);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#getData(org.epics.ioc.ca.CD)
             */
            public void getData(CD cd) {
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                for(ChannelField cf : channelFieldList) {
                    ChannelFieldImpl channelField = (ChannelFieldImpl)cf;
                    DBField dbField = channelField.getDBField();
                    PVField pvField = dbField.getPVField();
                    cd.put(pvField);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#setFieldGroup(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void setFieldGroup(ChannelFieldGroup channelFieldGroup) {
                this.channelFieldGroup = channelFieldGroup;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#start()
             */
            public void start() {
                if(isStarted) {
                    message("illegal request. monitorOLD active",MessageType.error);
                    return;
                }
                if(channelFieldGroup==null) {
                    throw new IllegalStateException("setFieldGroup was not called"); 
                }
                isStarted = true;
                processActive = false;
                recordListener = dbRecord.createRecordListener(this);
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                for(ChannelField cf : channelFieldList) {
                    ChannelFieldImpl channelField = (ChannelFieldImpl)cf;
                    DBField dbField = channelField.getDBField();
                    dbField.addListener(recordListener);
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#stop()
             */
            public void stop() {
                if(!isStarted) return;
                isStarted = false;
                dbRecord.removeRecordListener(recordListener);
                recordListener = null;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginProcess()
             */
            public void beginProcess() {
                if(!isStarted) return;
                processActive = true;
                channelMonitorRequester.beginPut();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endProcess()
             */
            public void endProcess() {
                if(!isStarted) return;
                processActive = false;
                channelMonitorRequester.endPut();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.db.DBStructure)
             */
            public void beginPut(DBStructure dbStructure) {
                if(!isStarted) return;
                if(processActive) return;
                putStructureActive = true;
                channelMonitorRequester.beginPut();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.db.DBStructure)
             */
            public void endPut(DBStructure dbStructure) {
                if(!isStarted) return;
                if(processActive) return;
                if(!putStructureActive) return;
                putStructureActive = false;
                channelMonitorRequester.endPut();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
             */
            public void dataPut(DBField dbRequested, DBField dbField) {
                PVField pvRequested = getRequestedPVField(dbRequested);
                boolean beginEnd = (!processActive&&!putStructureActive) ? true : false;
                if(beginEnd) channelMonitorRequester.beginPut();
                channelMonitorRequester.dataPut(pvRequested, dbField.getPVField());                
                if(beginEnd) {
                    channelMonitorRequester.endPut();
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBField)
             */
            public void dataPut(DBField dbField) {
                PVField targetPVField = getRequestedPVField(dbField);
                boolean beginEnd = (!processActive&&!putStructureActive) ? true : false;
                if(beginEnd) channelMonitorRequester.beginPut();
                channelMonitorRequester.dataPut(targetPVField);
                if(beginEnd){
                    channelMonitorRequester.endPut();
                }
            }
            
            private PVField getRequestedPVField(DBField requestedDBField) {
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                for(ChannelField cf : channelFieldList) {
                    ChannelFieldImpl channelField = (ChannelFieldImpl)cf;
                    DBField dbField = channelField.getDBField();
                    if(requestedDBField!=dbField) continue;
                    return requestedDBField.getPVField();
                }
                throw new IllegalStateException("Logic error. Unexpected dataPut"); 
            }


            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField, org.epics.ioc.db.DBField)
             */
            public void supportNamePut(DBField requested, DBField dbField) {
                // nothing to do
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBField)
             */
            public void supportNamePut(DBField dbField) {
                // nothing to do
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
             */
            public void unlisten(RecordListener listener) {
                stop();
                channel.getChannelStateListener().disconnect(channel);
            }   
        }
    }
}
