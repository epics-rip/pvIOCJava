/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;

import org.epics.ioc.install.IOCDatabaseFactory;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.ProcessSelf;
import org.epics.ioc.support.ProcessSelfRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.channelAccess.AccessRights;
import org.epics.pvData.channelAccess.Channel;
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
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVByte;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pvCopy.*;
import org.epics.pvData.pvCopy.PVCopyFactory;
import org.epics.pvData.pvCopy.PVCopyMonitor;
import org.epics.pvData.pvCopy.PVCopyMonitorRequester;

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
    
    static public void registerMonitor(MonitorCreate monitorCreate) {
        synchronized(monitorCreateList) {
            monitorCreateList.add(monitorCreate);
        }
    }
    
    private static final String providerName = "local";
    
    private static final ArrayList<MonitorCreate> monitorCreateList = new ArrayList<MonitorCreate>();

    private static class ChannelProviderLocal implements ChannelProvider{
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#getProviderName()
         */
        @Override
        public String getProviderName() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#findChannel(java.lang.String, org.epics.pvData.channelAccess.ChannelRequester, double)
         */
        @Override
        public void findChannel(String channelName,
                ChannelRequester channelRequester, double timeOut)
        {
            PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                pvDatabase = PVDatabaseFactory.getBeingInstalled();
                if(pvDatabase!=null) pvRecord = pvDatabase.findRecord(channelName);
            }
            if(pvRecord==null) {
                channelRequester.channelNotCreated();
                return;
            }
            LocateSupport locateSupport = IOCDatabaseFactory.get(pvDatabase).getLocateSupport(pvRecord);
            RecordProcess recordProcess = locateSupport.getRecordProcess();
            Channel channel = new ChannelImpl(pvRecord,recordProcess,channelRequester);
            channelRequester.channelCreated(channel);
        }
    }
    
    private static class ChannelImpl implements Channel {
        private boolean isDestroyed = false;
        private PVRecord pvRecord;
        private RecordProcess recordProcess;
        private ChannelRequester channelRequester;
        private LinkedList<ChannelProcess> channelProcessList = new LinkedList<ChannelProcess>();
        private LinkedList<ChannelGet> channelGetList = new LinkedList<ChannelGet>();
        private LinkedList<ChannelPut> channelPutList = new LinkedList<ChannelPut>();
        private LinkedList<ChannelPutGet> channelPutGetList = new LinkedList<ChannelPutGet>();
        private LinkedList<ChannelMonitor> channelMonitorList = new LinkedList<ChannelMonitor>();
        
        
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
        public void connect() {
            channelRequester.channelStateChange(this, true);
        }

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
            if(isDestroyed) return;
            isDestroyed = true;
            while(true) {
                ChannelProcess channelProcess = null;
                synchronized(channelProcessList) {
                    if(channelProcessList.size()>0) {
                        channelProcess = channelProcessList.get(channelProcessList.size()-1);
                    } else {
                        break;
                    }
                }
                channelProcess.destroy();
            }
            while(true) {
                ChannelGet channelGet = null;
                synchronized(channelGetList) {
                    if(channelGetList.size()>0) {
                        channelGet = channelGetList.get(channelGetList.size()-1);
                    } else {
                        break;
                    }
                }
                channelGet.destroy();
            }
            while(true) {
                ChannelPut channelPut = null;
                synchronized(channelPutList) {
                    if(channelPutList.size()>0) {
                        channelPut = channelPutList.get(channelPutList.size()-1);
                    } else {
                        break;
                    }
                }
                channelPut.destroy();
            }
            while(true) {
                ChannelPutGet channelPutGet = null;
                synchronized(channelPutGetList) {
                    if(channelPutGetList.size()>0) {
                        channelPutGet = channelPutGetList.get(channelPutGetList.size()-1);
                    } else {
                        break;
                    }
                }
                channelPutGet.destroy();
            }
            while(true) {
                ChannelMonitor channelMonitor = null;
                synchronized(channelMonitorList) {
                    if(channelMonitorList.size()>0) {
                        channelMonitor = channelMonitorList.get(channelMonitorList.size()-1);
                    } else {
                        break;
                    }
                }
                channelMonitor.destroy();
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
            synchronized(channelProcessList) {
                channelProcessList.add(channelProcessImpl);
            }
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
            synchronized(channelGetList) {
                channelGetList.add(channelGetImpl);
            }
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
            synchronized(channelPutList) {
                channelPutList.add(channelPutImpl);
            }
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
            synchronized(channelPutGetList) {
                channelPutGetList.add(channelPutGetImpl);
            }
            channelPutGetRequester.channelPutGetConnect(channelPutGetImpl, pvPutStructure,pvGetStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelMonitor(org.epics.pvData.channelAccess.ChannelMonitorRequester, org.epics.pvData.pv.PVStructure, java.lang.String, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.Executor)
         */
        @Override
        public void createChannelMonitor(
                ChannelMonitorRequester channelMonitorRequester,
                PVStructure pvRequest, String structureName,
                PVStructure pvOption,
                Executor executor)
        {
            PVString pvAlgorithm = pvOption.getStringField("algorithm");
            if(pvAlgorithm==null) {
                channelMonitorRequester.message("pvOption does not have a string field named algorithm", MessageType.error);
                channelMonitorRequester.unlisten();
                return;
            }
            String algorithm = pvAlgorithm.get();
            PVByte pvQueueSize = pvOption.getByteField("queueSize");
            byte queueSize = pvQueueSize.get();
            MonitorCreate monitorCreate = null;
            for(int i=0; i<monitorCreateList.size(); i++) {
                monitorCreate = monitorCreateList.get(i);
                if(monitorCreate.getName().equals(algorithm))  break;
            }
            if(monitorCreate==null) {
                channelMonitorRequester.message("no support for algorithm", MessageType.error);
                channelMonitorRequester.unlisten();
                return;
            }
            PVCopy pvCopy = PVCopyFactory.create(pvRecord, pvRequest, structureName, ((queueSize==0) ? true : false));
            ChannelMonitor channelMonitor = monitorCreate.create(channelMonitorRequester, pvOption, pvCopy, queueSize, executor);
            synchronized(channelMonitorList) {
                channelMonitorList.add(channelMonitor);
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#disconnect()
         */
        @Override
        public void disconnect() {
            destroy();
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
            ChannelProcessImpl(ChannelImpl channelImpl,ChannelProcessRequester channelProcessRequester)
            {
                this.channelImpl = channelImpl;
                this.channelProcessRequester = channelProcessRequester;
                recordProcess = channelImpl.getRecordProcess();
                isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester) {
                    processSelf = recordProcess.canProcessSelf();
                    if(processSelf==null) {
                        channelProcessRequester.message(
                                "already has process requester other than self", MessageType.error);
                        return;
                    }
                }
                requesterName = "Process:" + channelProcessRequester.getRequesterName();
            }
            
            private ChannelImpl channelImpl;
            private ChannelProcessRequester channelProcessRequester = null;
            private String requesterName;
            private boolean isDestroyed = false;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            private RequestResult requestResult = null;
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelProcess#destroy()
             */
            @Override
            public void destroy() {
                if(isDestroyed) return;
                isDestroyed = true;
                if(isRecordProcessRequester) {
                    recordProcess.releaseRecordProcessRequester(this);
                }
                synchronized(channelImpl.channelProcessList) {
                    channelImpl.channelProcessList.remove(this);
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
             * @see org.epics.ioc.ca.ChannelProcess#process()
             */
            @Override
            public void process(boolean lastRequest) {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    message("channel is not connected",MessageType.info);
                    channelProcessRequester.processDone(false);
                    return;
                }
                requestResult = RequestResult.success;
                if(isRecordProcessRequester) {
                    becomeProcessor(recordProcess);
                } else {
                    processSelf.request(this);
                }
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
                if(processSelf!=null) processSelf.endRequest(this);
                if(requestResult!=RequestResult.success) {
                    channelProcessRequester.message("requestResult " + requestResult.toString(),MessageType.info);
                }
                channelProcessRequester.processDone(true);
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
        
        private class ChannelGetImpl implements ChannelGet,ProcessSelfRequester,PVCopyMonitorRequester
        {
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
                    if(!isRecordProcessRequester) {
                        processSelf = recordProcess.canProcessSelf();
                        if(processSelf==null) {
                            channelGetRequester.message(
                                    "already has process requester other than self", MessageType.error);
                            this.process = false;
                        }
                    }
                }
                if(pvCopy.isShared()) {
                    pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
                    monitorBitSet = new BitSet(pvStructure.getNumberFields());
                    overrunBitSet = new BitSet(pvStructure.getNumberFields());
                    pvCopyMonitor.startMonitoring(monitorBitSet, overrunBitSet);
                }
            }
            
            private boolean firstTime = true;
            private ChannelImpl channelImpl;
            private ChannelGetRequester channelGetRequester;
            private PVStructure pvStructure;
            private PVCopy pvCopy;
            private boolean process;
            private BitSet bitSet = null;
            // following for share
            private BitSet monitorBitSet = null;
            private BitSet overrunBitSet = null;
            private boolean isDestroyed = false;
            private String requesterName;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            private RequestResult requestResult = RequestResult.success;
            private PVCopyMonitor pvCopyMonitor = null;
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGet#destroy()
             */
            @Override
            public void destroy() {
                if(isDestroyed) return;
                if(pvCopyMonitor!=null) {
                    pvCopyMonitor.stopMonitoring();
                }
                isDestroyed = true;
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
                synchronized(channelImpl.channelGetList) {
                    channelImpl.channelGetList.remove(this);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelGet#getBitSet()
             */
            @Override
            public BitSet getBitSet() {
                return bitSet;
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
                bitSet.clear();
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
                try {
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelGetRequester.getDone(true);
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
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
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
             * @see org.epics.pvData.pvCopy.PVCopyMonitorRequester#dataChanged()
             */
            @Override
            public void dataChanged() {}
            /* (non-Javadoc)
             * @see org.epics.pvData.pvCopy.PVCopyMonitorRequester#unlisten()
             */
            @Override
            public void unlisten() {
                
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
            
            private void getData() {
                if(pvCopyMonitor==null) {
                    pvCopy.updateCopySetBitSet(pvStructure, bitSet, false);
                } else {
                    overrunBitSet.clear();
                    bitSet.clear();
                    int index = 0;
                    index = monitorBitSet.nextSetBit(0);
                    while(index>=0) {
                        bitSet.set(index);
                        monitorBitSet.clear(index);
                        index = monitorBitSet.nextSetBit(index);
                    }
                }
                if(firstTime) {
                    bitSet.clear();
                    bitSet.set(0);
                    firstTime = false;
                } 
            }
        }
        
        private class ChannelPutImpl implements ChannelPut,ProcessSelfRequester
        {
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
                    if(!isRecordProcessRequester) {
                        processSelf = recordProcess.canProcessSelf();
                        if(processSelf==null) {
                            channelPutRequester.message(
                                    "already has process requester other than self", MessageType.error);
                            this.process = false;
                        }
                    }
                }
            }
            
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
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#destroy()
             */
            public void destroy() {
                if(isDestroyed) return;
                isDestroyed = true;
                if(isRecordProcessRequester) {
                    recordProcess.releaseRecordProcessRequester(this);
                }
                synchronized(channelImpl.channelPutList) {
                    channelImpl.channelPutList.remove(this);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPut#getBitSet()
             */
            @Override
            public BitSet getBitSet() {
                return bitSet;
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
                try {
                    putData();
                } finally {
                    pvRecord.unlock();
                }
                return;
            }        
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPut#get()
             */
            @Override
            public void get() {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    message("channel is destroyed",MessageType.info);
                    channelPutRequester.getDone(false);
                    return;
                }
                pvRecord.lock();
                try {
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutRequester.getDone(true);
                return;
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
                    putData();
                    channelPutRequester.putDone(false);
                    return;
                }
                putData();
                recordProcess.process(this, false, null);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                if(processSelf!=null) processSelf.endRequest(this);
                if(requestResult!=RequestResult.success) {
                    channelPutRequester.message("requestResult " + requestResult.toString(),MessageType.info);
                }
                channelPutRequester.putDone(true);
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
channelPutRequester.message("putData " + bitSet.toString(), MessageType.info);
               pvCopy.updateRecord(pvStructure, bitSet, false);
            }
            
            private void getData() {
                bitSet.clear();
                bitSet.set(0);
                pvCopy.updateCopyFromBitSet(pvStructure, bitSet, false);
             }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,ProcessSelfRequester
        {
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
                    if(!isRecordProcessRequester) {
                        processSelf = recordProcess.canProcessSelf();
                        if(processSelf==null) {
                            channelPutGetRequester.message(
                                    "already has process requester other than self", MessageType.error);
                            this.process = false;
                        }
                    }
                }
            }
            
            private ChannelImpl channelImpl;
            private ChannelPutGetRequester channelPutGetRequester = null;
            private PVStructure pvPutStructure;
            private PVCopy pvPutCopy;
            private PVStructure pvGetStructure;
            private PVCopy pvGetCopy;
            private boolean process;
            private BitSet putBitSet = null;
            private BitSet getBitSet = null;
            private boolean isDestroyed = false;
            private String requesterName;
            private RecordProcess recordProcess = null;
            private boolean isRecordProcessRequester = false;
            private ProcessSelf processSelf = null;
            private boolean canProcess = false;
            private RequestResult requestResult = null;
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#destroy()
             */
            @Override
            public void destroy() {
                isDestroyed = true;
                if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
                if(isRecordProcessRequester) {
                    recordProcess.releaseRecordProcessRequester(this);
                }
                synchronized(channelImpl.channelPutGetList) {
                    channelImpl.channelPutGetList.remove(this);
                }
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
                pvRecord.lock();
                try {
                    putData();
                    getData();
                } finally {
                    pvRecord.unlock();
                }
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
                    putData();
                    channelPutGetRequester.putGetDone(false);
                    return;
                }
                putData();
                recordProcess.process(this, true, null);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                this.requestResult = requestResult;
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
                pvPutCopy.updateRecord(pvPutStructure, putBitSet, false);
            }
            
            private void getData() {
                pvGetCopy.updateCopySetBitSet(pvGetStructure, getBitSet, false);
            }   
        }
    }
}
