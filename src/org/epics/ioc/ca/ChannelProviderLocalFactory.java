/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBListener;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.RecordListener;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.RecordProcessRequester;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Factory and implementation of local channel access, i.e. channel access that
 * accesses database records in the local IOC.
 * All user callbacks will be called with the appropriate records locked except for
 * 1) all methods of ChannelListener, 2) all methods of ChannelFieldGroupListener,
 * and 3) ChannelRequester.requestDone
 * @author mrk
 *
 */
public class ChannelProviderLocalFactory  {
    private static ChannelProviderLocal channelProvider = new ChannelProviderLocal();
    /**
     * Register. This is called by ChannelFactory.
     */
    static public void register() {
        channelProvider.register();
    }
    
    private static class ChannelProviderLocal implements ChannelProvider{
        private static boolean isRegistered = false; 
        private static final IOCDB iocdb = IOCDBFactory.getMaster();
        private static final String providerName = "local";

        private void register() {
            if(registerPvt()) ChannelAccessFactory.getChannelAccess().registerChannelProvider(this);
        }
        private synchronized boolean registerPvt() {
            if(isRegistered) return false;
            isRegistered = true;
            return true;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#createChannel(java.lang.String, org.epics.ioc.ca.ChannelListener)
         */
        public Channel createChannel(String pvName,String[] propertys,ChannelListener listener) {
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
            Channel channel = new ChannelImpl(dbRecord,listener,fieldName,options);
            return channel;
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
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProvider#destroy()
         */
        public void destroy() {
            // nothing to do
        }
    }
    
    static private Pattern periodPattern = Pattern.compile("[.]");
    static private Pattern leftBracePattern = Pattern.compile("[{]");
    static private Pattern rightBracePattern = Pattern.compile("[}]");
    
    private static class ChannelImpl extends AbstractChannel {
        private DBRecord dbRecord;
        private String fieldName;
        
        private ChannelImpl(DBRecord record,ChannelListener listener,
                String fieldName, String options)
        {
            super(listener,options);
            dbRecord = record;
            this.fieldName = fieldName;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#connect()
         */
        public void connect() {
            super.SetPVRecord(dbRecord.getPVRecord(),fieldName);
            super.connect();
        }
                
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelField(java.lang.String)
         */
        public ChannelField createChannelField(String name) {
            if(!super.isConnected()) {
                message("createChannelField but not connected",MessageType.warning);
                return null;
            }
            if(name==null || name.length()<=0) {
                return new BaseChannelField(dbRecord,dbRecord.getDBStructure().getPVField());
            }
            PVField pvField = super.getPVRecord().findProperty(name);
            if(pvField==null) return null;
            return new BaseChannelField(dbRecord,pvField);               
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelProcess(org.epics.ioc.ca.ChannelProcessRequester)
         */
        public ChannelProcess createChannelProcess(ChannelProcessRequester channelProcessRequester)
        {
            if(!super.isConnected()) {
                channelProcessRequester.message(
                    "createChannelProcess but not connected",MessageType.warning);
                return null;
            }
            ChannelProcessImpl channelProcess;
            try {
                channelProcess = new ChannelProcessImpl(channelProcessRequester);
                super.add(channelProcess);
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
        public ChannelGet createChannelGet(ChannelFieldGroup channelFieldGroup,
                ChannelGetRequester channelGetRequester, boolean process)
        {
            if(!super.isConnected()) {
                channelGetRequester.message(
                    "createChannelGet but not connected",MessageType.warning);
                return null;
            }
            ChannelGetImpl channelGet = 
                new ChannelGetImpl(channelFieldGroup,channelGetRequester,process);
            super.add(channelGet);
            return channelGet;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester, boolean)
         */
        public ChannelPut createChannelPut(ChannelFieldGroup channelFieldGroup,
                ChannelPutRequester channelPutRequester, boolean process)
        {
            if(!super.isConnected()) {
                channelPutRequester.message(
                    "createChannelPut but not connected",MessageType.warning);
                return null;
            }
            ChannelPutImpl channelPut = 
                new ChannelPutImpl(channelFieldGroup,channelPutRequester,process);
            super.add(channelPut);
            return channelPut;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester, boolean)
         */
        public ChannelPutGet createChannelPutGet(ChannelFieldGroup putFieldGroup,
            ChannelFieldGroup getFieldGroup, ChannelPutGetRequester channelPutGetRequester,
            boolean process)
        {
            if(!super.isConnected()) {
                channelPutGetRequester.message(
                    "createChannelPutGet but not connected",MessageType.warning);
                return null;
            }
            ChannelPutGetImpl channelPutGet = 
                new ChannelPutGetImpl(putFieldGroup,getFieldGroup,
                        channelPutGetRequester,process);
            super.add(channelPutGet);
            return channelPutGet;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequester, boolean)
         */
        public ChannelMonitor createChannelMonitor(ChannelMonitorRequester channelMonitorRequester)
        {
            if(!super.isConnected()) {
                channelMonitorRequester.message(
                        "createChannelMonitor but not connected",MessageType.warning);
                return null;
            }
            MonitorImpl impl = new MonitorImpl(this,channelMonitorRequester);
            super.add(impl);
            return impl;
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
                    channelProcessRequester.message(
                        "already has process requester other than self", MessageType.error);
                }
                requesterName = "Process:" + channelProcessRequester.getRequesterName();
            }           
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcess#destroy()
             */
            public void destroy() {
                isDestroyed = true;
                recordProcess.releaseRecordProcessRequester(this);
                ChannelImpl.this.remove(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcess#process()
             */
            public void process() {
                if(isDestroyed || !isConnected()) {
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
            private ChannelFieldGroup fieldGroup = null;
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
                this.fieldGroup = channelFieldGroup;
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
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#destroy()
             */
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
                ChannelImpl.this.remove(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelGet#get(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void get() {
                if(isDestroyed || !isConnected()) {
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
                if(isRecordProcessRequester) {
                    recordProcess.setInactive(this);
                } else {
                    recordProcess.processSelfSetInactive(this);
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
                        ChannelField field = channelFieldListIter.next();
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
            private boolean isDestroyed = false;
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
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#destroy()
             */
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
                ChannelImpl.this.remove(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void put() {
                if(isDestroyed || !isConnected()) {
                    message("channel is not connected",MessageType.info);
                    channelPutRequester.putDone(RequestResult.failure);
                    return;
                }
                requestResult = RequestResult.success;
                while(process) {
                    if(isRecordProcessRequester) {
                        if(!recordProcess.setActive(this)) {
                            message("could not process record",MessageType.warning);
                            break;
                        }
                    } else {
                        if(recordProcess.processSelfRequest(this)){
                            recordProcess.processSelfSetActive(this);
                        }  else {
                            message("could not process record",MessageType.warning);
                            break;
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
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#destroy()
             */
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester)recordProcess.releaseRecordProcessRequester(this);
                ChannelImpl.this.remove(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void putGet()
            {
                if(isDestroyed || !isConnected()) {
                    channelPutGetRequester.message(
                        "channel is not connected",MessageType.info);
                    channelPutGetRequester.putDone(RequestResult.failure);
                    channelPutGetRequester.getDone(RequestResult.failure);
                    return;
                }
                requestResult = RequestResult.success;
                while(process) {
                    if(isRecordProcessRequester) {
                        if(!recordProcess.setActive(this)) {
                            message("could not process record",MessageType.warning);
                            break;
                        }
                    } else {
                        if(recordProcess.processSelfRequest(this)){
                            recordProcess.processSelfSetActive(this);
                        }  else {
                            message("could not process record",MessageType.warning);
                           break;
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
        ChannelMonitor,DBListener
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
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#destroy()
             */
            public void destroy() {
                stop();
                ChannelImpl.this.remove(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#getData(org.epics.ioc.ca.CD)
             */
            public void getData(CD cd) {
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                for(ChannelField cf : channelFieldList) {
                    ChannelField channelField = (ChannelField)cf;
                    PVField pvField = channelField.getPVField();
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
                    message("illegal request. monitor active",MessageType.error);
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
                    ChannelField channelField = (ChannelField)cf;
                    DBField dbField = dbRecord.findDBField(channelField.getPVField());
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
                    PVField pvField = requestedDBField.getPVField();
                    if(cf.getPVField()==pvField) return pvField;
                }
                throw new IllegalStateException("Logic error. Unexpected dataPut"); 
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
             */
            public void unlisten(RecordListener listener) {
                stop();
                channel.getChannelListener().destroy(channel);
            }   
        }
    }
}
