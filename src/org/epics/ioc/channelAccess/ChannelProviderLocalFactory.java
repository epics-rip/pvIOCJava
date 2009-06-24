/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;
import java.util.LinkedList;

import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.ProcessSelf;
import org.epics.ioc.support.ProcessSelfRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.channelAccess.AccessRights;
import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelAccess;
import org.epics.pvData.channelAccess.ChannelGet;
import org.epics.pvData.channelAccess.ChannelGetRequester;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.channelAccess.ChannelProcess;
import org.epics.pvData.channelAccess.ChannelProcessRequester;
import org.epics.pvData.channelAccess.ChannelPut;
import org.epics.pvData.channelAccess.ChannelPutGet;
import org.epics.pvData.channelAccess.ChannelPutGetRequester;
import org.epics.pvData.channelAccess.ChannelPutRequester;
import org.epics.pvData.channelAccess.ChannelRequester;
import org.epics.pvData.channelAccess.CreatePVStructureRequester;
import org.epics.pvData.channelAccess.GetFieldRequester;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVListener;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.PVCopy;
import org.epics.pvData.pvCopy.PVCopyFactory;

/**
 * Factory and implementation of local channel access, i.e. channel access that
 * accesses database records in the local IOC.
 * All user callbacks will be called with the appropriate records locked except for
 * 1) all methods of ChannelRequester, 2) all methods of ChannelFieldGroupListener,
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
        ChannelAccessFactory.registerChannelProvider(channelProvider);
    }
    
    private static final String providerName = "local";

    private static class ChannelProviderLocal implements ChannelProvider,ChannelAccess{
        
        
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#findChannel(java.lang.String, double, org.epics.ioc.channelAccess.ChannelProviderRequester)
         */
        @Override
        public void findChannel(String channelName, double timeOut,ChannelProviderRequester channelProviderRequester) {
            PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                pvDatabase = PVDatabaseFactory.getBeingInstalled();
                if(pvDatabase!=null) pvRecord = pvDatabase.findRecord(channelName);
            }
            if(pvRecord==null) {
                channelProviderRequester.timeout(channelName, this);
            } else {
                channelProviderRequester.foundChannel(channelName, this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#getChannelAccess()
         */
        @Override
        public ChannelAccess getChannelAccess() {
            return this;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelAccess#cancelCreateChannel(java.lang.String, org.epics.pvData.channelAccess.ChannelRequester)
         */
        @Override
        public void cancelCreateChannel(String channelName,ChannelRequester channelRequester) {}
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelAccess#createChannel(java.lang.String, org.epics.pvData.channelAccess.ChannelRequester)
         */
        @Override
        public void createChannel(String channelName,ChannelRequester channelRequester) {
            PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                pvDatabase = PVDatabaseFactory.getBeingInstalled();
                if(pvDatabase!=null) {
                    pvRecord = pvDatabase.findRecord(channelName);
                }
            }
            if(pvRecord==null) {
                channelRequester.message("channel not found", MessageType.error);
                return;
            }
            LocateSupport locateSupport = IOCDatabaseFactory.get(pvDatabase).getLocateSupport(pvRecord);
            RecordProcess recordProcess = locateSupport.getRecordProcess();
            Channel channel = new ChannelImpl(pvRecord,recordProcess,channelRequester);
            channelRequester.channelCreated(channel);
        }
    }
    
    private static class ChannelImpl implements Channel {
        private boolean isDestroyed = true;
        private PVRecord pvRecord;
        private RecordProcess recordProcess;
        private ChannelRequester channelRequester;
        private RecordProcessRequester recordProcessRequester = null;
        private LinkedList<PVListener> pvListenerList = new LinkedList<PVListener>();
        
        
        private ChannelImpl(PVRecord pvRecord,RecordProcess recordProcess,ChannelRequester channelRequester)
        {
            this.pvRecord = pvRecord;
            this.recordProcess = recordProcess;
            this.channelRequester = channelRequester;
        }       
        
        private boolean isDestroyed() {
            return isDestroyed;
        }
        
        private RecordProcess getRecordProcess() {
            return recordProcess;
        }
        
        private void setRecordProcessRequester(RecordProcessRequester recordProcessRequester) {
            if(isDestroyed) return;
            this.recordProcessRequester = recordProcessRequester;
        }
        private void releaseRecordProcessRequester() {
            if(isDestroyed) return;
            recordProcess.releaseRecordProcessRequester(recordProcessRequester);
            recordProcessRequester = null;
        }
        private void addPVListener(PVListener pvListener) {
            if(isDestroyed) return;
            synchronized(pvListenerList) {
                pvListenerList.add(pvListener);
            }
        }
        private void removePVListener(PVListener pvListener) {
            if(isDestroyed) return;
            synchronized(pvListenerList) {
                pvListenerList.remove(pvListener);
            }
        }
        /* (non-Javadoc)         * @see org.epics.pvData.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return channelRequester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.Requester#message(java.lang.String, org.epics.pvData.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            channelRequester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#connect()
         */
        @Override
        public void connect() {}

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#isConnected()
         */
        @Override
        public boolean isConnected() {
            return !isDestroyed;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#destroy()
         */
        @Override
        public void destroy() {
            isDestroyed = true;
            RecordProcessRequester recordProcessRequester = this.recordProcessRequester;
            if(recordProcessRequester!=null) recordProcess.releaseRecordProcessRequester(recordProcessRequester);
            while(true) {
                PVListener pvListener = null;
                synchronized(pvListenerList) {
                    if(pvListenerList.size()>0) {
                        pvListener = pvListenerList.get(0);
                    } else {
                        break;
                    }
                }
                pvRecord.unregisterListener(pvListener);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createPVStructure(org.epics.pvData.channelAccess.CreatePVStructureRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean)
         */
        @Override
        public void createPVStructure(CreatePVStructureRequester requester,
                PVStructure pvRequest, String structureName, boolean shareData)
        {
            PVCopy pvCopy = PVCopyFactory.create(pvRecord, pvRequest, structureName, shareData);
            requester.createDone(pvCopy.createPVStructure());
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#getField(org.epics.pvData.channelAccess.GetFieldRequester, java.lang.String)
         */
        @Override
        public void getField(GetFieldRequester requester,String subField) {
            if(subField==null || subField.length()<1) {
                requester.getDone(pvRecord.getStructure());
            }
            PVField pvField = pvRecord.getSubField(subField);
            if(pvField==null) {
                requester.getDone(null);
            } else {
                requester.getDone(pvField.getField());
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelProcess(org.epics.pvData.channelAccess.ChannelProcessRequester)
         */
        @Override
        public void createChannelProcess(ChannelProcessRequester channelProcessRequester) {
            ChannelProcessImpl channelProcessImpl = new ChannelProcessImpl(this,channelProcessRequester);
            channelProcessRequester.channelProcessConnect(channelProcessImpl);
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelGet(org.epics.pvData.channelAccess.ChannelGetRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean)
         */
        @Override
        public void createChannelGet(ChannelGetRequester channelGetRequester,
                PVStructure pvRequest, String structureName, boolean shareData,
                boolean process)
        {
            PVCopy pvCopy = PVCopyFactory.create(pvRecord, pvRequest, structureName, shareData);
            PVStructure pvStructure = pvCopy.createPVStructure();
            ChannelGetImpl channelGetImpl = new ChannelGetImpl(this,channelGetRequester,pvStructure,pvCopy,process);
            channelGetRequester.channelGetConnect(channelGetImpl, pvStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelPut(org.epics.pvData.channelAccess.ChannelPutRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean)
         */
        @Override
        public void createChannelPut(ChannelPutRequester channelPutRequester,
                PVStructure pvRequest, String structureName, boolean shareData,
                boolean process)
        {
            PVCopy pvCopy = PVCopyFactory.create(pvRecord, pvRequest, structureName, shareData);
            PVStructure pvStructure = pvCopy.createPVStructure();
            ChannelPutImpl channelPutImpl = new ChannelPutImpl(this,channelPutRequester,pvStructure,pvCopy,process);
            channelPutRequester.channelPutConnect(channelPutImpl, pvStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelPutGet(org.epics.pvData.channelAccess.ChannelPutGetRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean)
         */
        @Override
        public void createChannelPutGet(
                ChannelPutGetRequester channelPutGetRequester,
                PVStructure pvPutRequest, String putStructureName,
                boolean sharePutData, PVStructure pvGetRequest,
                String getStructureName, boolean shareGetData, boolean process)
        {
            PVCopy pvPutCopy = PVCopyFactory.create(pvRecord, pvPutRequest, putStructureName, sharePutData);
            PVStructure pvPutStructure = pvPutCopy.createPVStructure();
            PVCopy pvGetCopy = PVCopyFactory.create(pvRecord, pvGetRequest, getStructureName, shareGetData);
            PVStructure pvGetStructure = pvGetCopy.createPVStructure();
            ChannelPutGetImpl channelPutGetImpl = new ChannelPutGetImpl(
                    this,channelPutGetRequester,pvPutStructure,pvPutCopy,
                    pvGetStructure,pvGetCopy,
                    process);
            channelPutGetRequester.channelPutGetConnect(channelPutGetImpl, pvPutStructure,pvGetStructure);
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelMonitor(org.epics.pvData.channelAccess.ChannelMonitorRequester, org.epics.pvData.pv.PVStructure)
         */
        @Override
        public void createChannelMonitor(
                ChannelMonitorRequester channelMonitorRequester,
                PVStructure pvStructure, boolean shareData, byte queueSize) {
            // TODO Auto-generated method stub
            
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#disconnect()
         */
        @Override
        public void disconnect() {
            // TODO Auto-generated method stub
            
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#getAccessRights(org.epics.pvData.pv.PVField)
         */
        @Override
        public AccessRights getAccessRights(PVField pvField) {
            // TODO Auto-generated method stub
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#getChannelListener()
         */
        @Override
        public ChannelRequester getChannelRequester() {
            return channelRequester;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#getChannelName()
         */
        @Override
        public String getChannelName() {
            return pvRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#getProviderName()
         */
        @Override
        public String getProviderName() {
            return channelRequester.getRequesterName();
        }
        
        private static class ChannelProcessImpl implements ChannelProcess,ProcessSelfRequester
        {
            private ChannelImpl channelImpl;
            private ChannelProcessRequester channelProcessRequester = null;
            private boolean isDestroyed = false;
            private String requesterName;
            
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            
            private RequestResult requestResult = null;
                 
            private ChannelProcessImpl(ChannelImpl channelImpl,ChannelProcessRequester channelProcessRequester)
            {
                this.channelImpl = channelImpl;
                this.channelProcessRequester = channelProcessRequester;
                
            }
            
            private boolean init() {
                recordProcess = channelImpl.getRecordProcess();
                isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                if(isRecordProcessRequester) {
                    channelImpl.setRecordProcessRequester(this);
                } else {
                    processSelf = recordProcess.canProcessSelf();
                    if(processSelf==null) {
                        channelProcessRequester.message(
                                "already has process requester other than self", MessageType.error);
                        return false;
                    }
                }
                requesterName = "Process:" + channelProcessRequester.getRequesterName();
                return true;
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelProcess#destroy()
             */
            @Override
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) channelImpl.releaseRecordProcessRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelProcess#process()
             */
            @Override
            public void process(boolean lastRequest) {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    channelProcessRequester.message(
                            "channel is not connected",MessageType.info);
                    channelProcessRequester.processDone(false);
                    return;
                }
                if(isRecordProcessRequester) {
                    becomeProcessor(recordProcess);
                } else {
                    processSelf.request(this);
                }
            }    
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelProcessRequester.message(message, messageType);
            }    
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
             */
            @Override
            public void recordProcessComplete() {
                channelProcessRequester.processDone(true);
                if(processSelf!=null) processSelf.endRequest(this);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
             */
            @Override
            public void becomeProcessor(RecordProcess recordProcess) {
                if(recordProcess.process(this, false, null)) return;
                channelProcessRequester.message(
                        "could not process record",MessageType.error);
                channelProcessRequester.processDone(false);
            }
        }
        
        private class ChannelGetImpl implements ChannelGet,ProcessSelfRequester
        {
            private ChannelImpl channelImpl;
            private ChannelGetRequester channelGetRequester;
            private PVStructure pvStructure;
            private PVCopy pvCopy;
            private boolean process;
            private BitSet bitSet = null;
            private boolean isDestroyed = false;
            private String requesterName;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            
            private RequestResult requestResult = RequestResult.success;
            
            private ChannelGetImpl(ChannelImpl channelImpl,ChannelGetRequester channelGetRequester,PVStructure pvStructure,PVCopy pvCopy,boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelGetRequester = channelGetRequester;
                this.pvStructure = pvStructure;
                this.pvCopy = pvCopy;
                this.process = process;
                bitSet = new BitSet(pvStructure.getNumberFields());
                requesterName = "Get:" + channelGetRequester.getRequesterName();
                if(process) {
                    recordProcess = channelImpl.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(isRecordProcessRequester) {
                        channelImpl.setRecordProcessRequester(this);
                    } else {
                        processSelf = recordProcess.canProcessSelf();
                        if(processSelf==null) {
                            channelGetRequester.message(
                                    "already has process requester other than self", MessageType.error);
                            this.process = false;
                        }
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGet#destroy()
             */
            @Override
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) channelImpl.releaseRecordProcessRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGet#get()
             */
            @Override
            public void get(boolean lastRequest) {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    channelGetRequester.message(
                        "channel is destroyed",MessageType.info);
                    channelGetRequester.getDone(false);
                }
                requestResult = RequestResult.success;
                if(process) {
                    if(isRecordProcessRequester) {
                        becomeProcessor(recordProcess);
                    } else {
                        processSelf.request(this);
                    }
                    return;
                }
                getData();
                channelGetRequester.getDone(true);
            }                
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGet#getBitSet()
             */
            @Override
            public BitSet getBitSet() {
                return bitSet;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelGetRequester.message(message, messageType);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                getData();
                recordProcess.setInactive(this);
                if(processSelf!=null) processSelf.endRequest(this);
                if(requestResult!=RequestResult.success) {
                    channelGetRequester.message("requestResult " + requestResult.toString(),MessageType.info);
                }
                channelGetRequester.getDone(true);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
             */
            @Override
            public void becomeProcessor(RecordProcess recordProcess) {
                if(recordProcess.process(this, true, null)) return;
                channelGetRequester.message("process failed", MessageType.warning);
                requestResult = RequestResult.failure;
                getData();
                if(processSelf!=null) processSelf.endRequest(this);
                channelGetRequester.getDone(true);
            }
            
            private void getData() {
                bitSet.clear();
                pvCopy.switchBitSets(pvStructure, bitSet, null);
            }
        }
        
        private class ChannelPutImpl implements ChannelPut,ProcessSelfRequester
        {
            private ChannelImpl channelImpl;
            private ChannelPutRequester channelPutRequester = null;
            private PVStructure pvStructure;
            private PVCopy pvCopy;
            private boolean process;
            private BitSet bitSet = null;
            private boolean isDestroyed = false;
            private String requesterName;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            private boolean canProcess = false;
            private RequestResult requestResult = null;
                        
            private ChannelPutImpl(
                    ChannelImpl channelImpl,
                    ChannelPutRequester channelPutRequester,
                    PVStructure pvStructure,
                    PVCopy pvCopy,
                    boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelPutRequester = channelPutRequester;
                this.pvStructure = pvStructure;
                this.pvCopy = pvCopy;
                this.process = process;
                bitSet = new BitSet(pvStructure.getNumberFields());
                requesterName = "Put:" + channelPutRequester.getRequesterName();
                if(process) {
                    recordProcess = channelImpl.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(isRecordProcessRequester) {
                        channelImpl.setRecordProcessRequester(this);
                    } else {
                        processSelf = recordProcess.canProcessSelf();
                        if(processSelf==null) {
                            channelPutRequester.message(
                                    "already has process requester other than self", MessageType.error);
                            this.process = false;
                        }
                    }
                }
            } 
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#destroy()
             */
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) channelImpl.releaseRecordProcessRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#put(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void put(boolean lastRequest) {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    message("channel is destroyed",MessageType.info);
                    channelPutRequester.putDone(false);
                    return;
                }
                requestResult = RequestResult.success;
                if(process) {
                    if(isRecordProcessRequester) {
                        becomeProcessor(recordProcess);
                    } else {
                        processSelf.request(this);
                    }
                    return;
                }
                pvRecord.lock();
                putData();
                return;
            }        
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPut#getBitSet()
             */
            @Override
            public BitSet getBitSet() {
                return bitSet;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                if(processSelf!=null) processSelf.endRequest(this);
                if(requestResult!=RequestResult.success) {
                    channelPutRequester.message("requestResult " + requestResult.toString(),MessageType.info);
                }
                channelPutRequester.putDone(true);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
             */
            @Override
            public void becomeProcessor(RecordProcess recordProcess) {
                canProcess = recordProcess.setActive(this);
                if(!canProcess) {
                    requestResult = RequestResult.failure;
                    message("setActive failed",MessageType.error);
                }
                putData();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requesterName;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelPutRequester.message(message, messageType);
            }
            
            private void putData() {
               bitSet.clear();
               pvCopy.updateRecord(pvStructure, bitSet);
            }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,ProcessSelfRequester
        {
            private ChannelImpl channelImpl;
            private ChannelPutGetRequester channelPutGetRequester = null;
            private PVStructure pvPutStructure;
            private PVCopy pvPutCopy;
            private PVStructure pvGetStructure;
            private PVCopy pvGetCopy;
            private boolean process;
            private BitSet getBitSet = null;
            private BitSet putBitSet = null;
            private boolean isDestroyed = false;
            private String requesterName;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            private boolean canProcess = false;
            private RequestResult requestResult = null;
                 
            private ChannelPutGetImpl(
                    ChannelImpl channelImpl,
                    ChannelPutGetRequester channelPutGetRequester,
                    PVStructure pvPutStructure,
                    PVCopy pvPutCopy,
                    PVStructure pvGetStructure,
                    PVCopy pvGetCopy,
                    boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelPutGetRequester = channelPutGetRequester;
                this.pvPutStructure = pvPutStructure;
                this.pvPutCopy = pvPutCopy;
                this.pvGetStructure = pvGetStructure;
                this.pvGetCopy = pvGetCopy;
                this.process = process;
                putBitSet = new BitSet(pvPutStructure.getNumberFields());
                getBitSet = new BitSet(pvGetStructure.getNumberFields());
                requesterName = "PutGet:" + channelPutGetRequester.getRequesterName();
                if(process) {
                    recordProcess = channelImpl.getRecordProcess();
                    isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                    if(isRecordProcessRequester) {
                        channelImpl.setRecordProcessRequester(this);
                    } else {
                        processSelf = recordProcess.canProcessSelf();
                        if(processSelf==null) {
                            channelPutGetRequester.message(
                                    "already has process requester other than self", MessageType.error);
                            this.process = false;
                        }
                    }
                }
            }
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#destroy()
             */
            @Override
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) channelImpl.releaseRecordProcessRequester();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup)
             */
            @Override
            public void putGet(boolean lastRequest)
            {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    channelPutGetRequester.message(
                        "channel is not connected",MessageType.info);
                    channelPutGetRequester.putGetDone(false);
                    return;
                }
                requestResult = RequestResult.success;
                canProcess = false;
                if(process) {
                    if(isRecordProcessRequester) {
                        becomeProcessor(recordProcess);
                    } else {
                        processSelf.request(this);
                    }
                    return;
                }
                putData();
                getData();
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutGet#getGetBitSet()
             */
            @Override
            public BitSet getGetBitSet() {
                return getBitSet;
            }

            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutGet#getPutBitSet()
             */
            @Override
            public BitSet getPutBitSet() {
                return putBitSet;
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                getData();                
                if(canProcess) recordProcess.setInactive(this);
                if(processSelf!=null) processSelf.endRequest(this);
                if(requestResult!=RequestResult.success) {
                    channelPutGetRequester.message("requestResult " + requestResult.toString(),MessageType.info);
                }
                channelPutGetRequester.putGetDone(true);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.ProcessSelfRequester#becomeProcessor(org.epics.ioc.support.RecordProcess)
             */
            @Override
            public void becomeProcessor(RecordProcess recordProcess) {
                canProcess = recordProcess.setActive(this);
                if(!canProcess) {
                    requestResult = RequestResult.failure;
                    message("setActive failed",MessageType.error);
                }
                putData();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return requesterName;
            }     
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelPutGetRequester.message(message, messageType);
            }
            
            private void putData() {
                putBitSet.clear();
                pvPutCopy.updateRecord(pvPutStructure, putBitSet);
            }
            
            private void getData() {
                getBitSet.clear();
                pvGetCopy.updateRecord(pvGetStructure, getBitSet);
            }
            
        }
        
        private class MonitorImpl implements ChannelMonitor,PVListener
        {
            private ChannelMonitorRequester channelMonitorRequester;
            
            private MonitorImpl(Channel channel,ChannelMonitorRequester channelMonitorRequester) {
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
             * @see org.epics.ioc.ca.ChannelMonitor#setFieldGroup(org.epics.ioc.ca.ChannelFieldGroup)
             */
            public void setFieldGroup(ChannelFieldGroup channelFieldGroup) {}
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#start()
             */
            public void start() {
                channelMonitorRequester.initialDataAvailable();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelMonitor#stop()
             */
            public void stop() {}
        }
    }
}
