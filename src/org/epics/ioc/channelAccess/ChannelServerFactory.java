/**
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.ArrayList;
import java.util.LinkedList;

import org.epics.pvData.channelAccess.AccessRights;
import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelArray;
import org.epics.pvData.channelAccess.ChannelArrayRequester;
import org.epics.pvData.channelAccess.ChannelFind;
import org.epics.pvData.channelAccess.ChannelFindRequester;
import org.epics.pvData.channelAccess.ChannelGet;
import org.epics.pvData.channelAccess.ChannelGetRequester;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.channelAccess.ChannelProcess;
import org.epics.pvData.channelAccess.ChannelProcessRequester;
import org.epics.pvData.channelAccess.ChannelProvider;
import org.epics.pvData.channelAccess.ChannelPut;
import org.epics.pvData.channelAccess.ChannelPutGet;
import org.epics.pvData.channelAccess.ChannelPutGetRequester;
import org.epics.pvData.channelAccess.ChannelPutRequester;
import org.epics.pvData.channelAccess.ChannelRequester;
import org.epics.pvData.channelAccess.CreatePVStructureRequester;
import org.epics.pvData.channelAccess.GetFieldRequester;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;
import org.epics.pvData.pvCopy.PVCopy;
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
public class ChannelServerFactory  {
    private static ChannelServerLocal channelServer = new ChannelServerLocal();
    /**
     * Register. This is called by ChannelAccessFactory.
     */
    static public void register() {
        ChannelAccessFactory.registerChannelProvider(channelServer);
        // start standard algorithms
        MonitorOnPutFactory.start();
        MonitorOnChangeFactory.start();
        MonitorOnAbsoluteChangeFactory.start();
        MonitorOnPercentChangeFactory.start();
    }
    
    static public ChannelServer getChannelServer() {
        return channelServer;
    }
    
    private static final String providerName = "local";
    private static final ArrayList<MonitorCreate> monitorCreateList = new ArrayList<MonitorCreate>();
    private static final PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static ChannelProcessorProvider channelProcessorProvider = null;
   
    private static class ChannelFindImpl implements ChannelFind {
        
        private ChannelFindImpl(PVRecord pvRecord) {
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFind#cancelChannelFind()
         */
        @Override
        public void cancelChannelFind() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFind#geChannelProvider()
         */
        @Override
        public ChannelProvider getChannelProvider() {
            return channelServer;
        }
        
    }
    
    private static class ChannelServerLocal implements ChannelServer{
        private LinkedList<Channel> channelList = new LinkedList<Channel>();
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#destroy()
         */
        @Override
        public void destroy() {
            Channel channel = null;
            while(true) {
                synchronized(channelList) {
                    if(channelList.size()<1) return;
                    channel = channelList.pop();
                }
                channel.destroy();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#getProviderName()
         */
        @Override
        public String getProviderName() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelProvider#channelFind(java.lang.String, org.epics.pvData.channelAccess.ChannelFindRequester)
         */
        @Override
        public ChannelFind channelFind(String channelName,ChannelFindRequester channelFindRequester) {
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                PVDatabase beingInstalled = PVDatabaseFactory.getBeingInstalled();
                if(beingInstalled!=null) pvRecord = beingInstalled.findRecord(channelName);
            }
            ChannelFindImpl channelFind = new ChannelFindImpl(pvRecord);
            boolean wasFound = ((pvRecord==null) ? false : true);
            channelFindRequester.channelFindResult(channelFind, wasFound);
            return channelFind;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.ChannelProvider#createChannel(java.lang.String, org.epics.pvData.channelAccess.ChannelRequester)
         */
        @Override
        public void createChannel(String channelName,ChannelRequester channelRequester) {
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                PVDatabase beingInstalled = PVDatabaseFactory.getBeingInstalled();
                if(beingInstalled!=null) pvRecord = beingInstalled.findRecord(channelName);
            }
            boolean wasFound = ((pvRecord==null) ? false : true);
            if(wasFound) {
                ChannelImpl channel = new ChannelImpl(pvRecord,channelRequester);
                channelRequester.channelCreated(channel);
                synchronized(channelList) {
                    channelList.add(channel);
                }
            } else {
                channelRequester.message("channel " + channelName + " nor found", MessageType.fatalError);
                channelRequester.channelNotCreated();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#registerMonitor(org.epics.ioc.channelAccess.MonitorCreate)
         */
        @Override
        public void registerMonitor(MonitorCreate monitorCreate) {
            synchronized(monitorCreateList) {
                monitorCreateList.add(monitorCreate);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#destroyMonitor(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelMonitor)
         */
        @Override
        public void destroyMonitor(Channel channel,ChannelMonitor channelMonitor) {
            ChannelImpl channelImpl = (ChannelImpl)channel;
            synchronized(channelImpl.channelMonitorList) {
                channelImpl.channelMonitorList.remove(channelMonitor);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProvider#registerChannelProcessProvider(org.epics.ioc.channelAccess.ChannelProcessorProvider)
         */
        @Override
        public boolean registerChannelProcessProvider(ChannelProcessorProvider channelProcessorProvider) {
            if(ChannelServerFactory.channelProcessorProvider!=null) return false;
            ChannelServerFactory.channelProcessorProvider = channelProcessorProvider;
            return true;
        }
    }
    
    private static class ChannelImpl implements Channel {
        private boolean isDestroyed = false;
        private PVRecord pvRecord;
        private ChannelRequester channelRequester;
        private LinkedList<ChannelProcess> channelProcessList = new LinkedList<ChannelProcess>();
        private LinkedList<ChannelGet> channelGetList = new LinkedList<ChannelGet>();
        private LinkedList<ChannelPut> channelPutList = new LinkedList<ChannelPut>();
        private LinkedList<ChannelPutGet> channelPutGetList = new LinkedList<ChannelPutGet>();
        private LinkedList<ChannelMonitor> channelMonitorList = new LinkedList<ChannelMonitor>();
        private LinkedList<ChannelArray> channelArrayList = new LinkedList<ChannelArray>();
        
        
        private ChannelImpl(PVRecord pvRecord,ChannelRequester channelRequester)
        {
            this.pvRecord = pvRecord;
            this.channelRequester = channelRequester;
        }       
        
        private boolean isDestroyed() {
            return isDestroyed;
        }
        
        private ChannelProcessor requestChannelProcessor(PVRecord pvRecord,ChannelProcessorRequester channelProcessorRequester)  {
            ChannelProcessorProvider channelProcessorProvider = ChannelServerFactory.channelProcessorProvider;
            if(channelProcessorProvider==null) return null;
            return channelProcessorProvider.requestChannelProcessor(pvRecord, channelProcessorRequester);
        }
    
        /* (non-Javadoc)
         * @see org.epics.pvData.pv.Requester#getRequesterName()
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
                return;
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
            new ChannelProcessImpl(this,channelProcessRequester);
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
            new ChannelGetImpl(this,channelGetRequester,pvStructure,pvCopy,process);
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
            new ChannelPutImpl(this,channelPutRequester,pvStructure,pvCopy,process);
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
            new ChannelPutGetImpl(this,channelPutGetRequester,pvPutStructure,pvPutCopy,pvGetStructure,pvGetCopy,process);
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
            PVInt pvQueueSize = pvOption.getIntField("queueSize");
            int queueSize = pvQueueSize.get();
            MonitorCreate monitorCreate = null;
            for(int i=0; i<monitorCreateList.size(); i++) {
                MonitorCreate create = monitorCreateList.get(i);
                if(create.getName().equals(algorithm))  {
                    monitorCreate = create;
                    break;
                }
            }
            if(monitorCreate==null) {
                channelMonitorRequester.message("no support for algorithm", MessageType.error);
                channelMonitorRequester.unlisten();
                return;
            }
            PVCopy pvCopy = PVCopyFactory.create(pvRecord, pvRequest, structureName, ((queueSize==0) ? true : false));
            ChannelMonitor channelMonitor = monitorCreate.create(this,channelMonitorRequester, pvOption, pvCopy, queueSize, executor);
            if(channelMonitor==null) {
                channelMonitorRequester.channelMonitorConnect(null);
                return;
            }
            synchronized(channelMonitorList) {
                channelMonitorList.add(channelMonitor);
            }
            channelMonitorRequester.channelMonitorConnect(channelMonitor);
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.channelAccess.Channel#createChannelArray(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelArrayRequester, java.lang.String)
         */
        @Override
        public void createChannelArray(ChannelArrayRequester channelArrayRequester,
                String subField)
        {
            PVField pvField = pvRecord.getSubField(subField);
            if(pvField==null) {
                channelArrayRequester.message("subField does not exist", MessageType.error);
                channelArrayRequester.channelArrayConnect(null, null);
                return;
            }
            if(pvField.getField().getType()!=Type.scalarArray) {
                channelArrayRequester.message("subField is not an array", MessageType.error);
                channelArrayRequester.channelArrayConnect(null, null);
                return;
            }
            PVArray pvArray = (PVArray)pvField;
            PVArray pvCopy = pvDataCreate.createPVArray(null, "", pvArray.getArray().getElementType());
            new ChannelArrayImpl(this,channelArrayRequester,pvArray,pvCopy);
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
        
        private static class ChannelProcessImpl implements ChannelProcess,ChannelProcessorRequester
        {
            ChannelProcessImpl(ChannelImpl channelImpl,ChannelProcessRequester channelProcessRequester)
            {
                this.channelImpl = channelImpl;
                this.channelProcessRequester = channelProcessRequester;
                PVRecord pvRecord = channelImpl.pvRecord;
                channelProcessor = channelImpl.requestChannelProcessor(pvRecord, this);
                if(channelProcessor!=null) {
                    synchronized(channelImpl.channelProcessList) {
                        channelImpl.channelProcessList.add(this);
                        channelProcessRequester.channelProcessConnect(this);
                    }
                } else {
                    channelProcessRequester.channelProcessConnect(null);
                }
            }

            private ChannelImpl channelImpl;
            private ChannelProcessRequester channelProcessRequester;
            private ChannelProcessor channelProcessor = null;
            private boolean isDestroyed = false;
            private boolean success = false;
            private boolean lastRequest = false;
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelProcess#destroy()
             */
            @Override
            public void destroy() {
                if(isDestroyed) return;
                isDestroyed = true;
                channelProcessor.detach();
                synchronized(channelImpl.channelProcessList) {
                    channelImpl.channelProcessList.remove(this);
                }
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelProcess#process(boolean)
             */
            @Override
            public void process(boolean lastRequest) {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    message("channel is destroyed",MessageType.info);
                    channelProcessRequester.processDone(false);
                    return;
                }
                this.lastRequest = lastRequest;
                channelProcessor.requestProcess();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
                channelProcessor.process(false, null);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                channelProcessRequester.processDone(success);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessResult(boolean)
             */
            @Override
            public void recordProcessResult(boolean success) {
                this.success = success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelProcessRequester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelProcessRequester.message(message, messageType);
            }
            
        }
        
        private class ChannelGetImpl implements ChannelGet,ChannelProcessorRequester,PVCopyMonitorRequester
        {
            private ChannelGetImpl(ChannelImpl channelImpl,ChannelGetRequester channelGetRequester,PVStructure pvStructure,PVCopy pvCopy,boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelGetRequester = channelGetRequester;
                this.pvStructure = pvStructure;
                this.pvCopy = pvCopy;
                this.process = process;
                bitSet = new BitSet(pvStructure.getNumberFields());
                if(process) {
                    this.process = true;
                    PVRecord pvRecord = channelImpl.pvRecord;
                    channelProcessor = channelImpl.requestChannelProcessor(pvRecord, this);
                    if(channelProcessor==null) {
                        channelGetRequester.message(
                                "can not process", MessageType.error);
                        this.process = false;
                    }
                }
                if(pvCopy.isShared()) {
                    pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
                    monitorBitSet = new BitSet(pvStructure.getNumberFields());
                    overrunBitSet = new BitSet(pvStructure.getNumberFields());
                    pvCopyMonitor.startMonitoring(monitorBitSet, overrunBitSet);
                }
                synchronized(channelImpl.channelGetList) {
                    channelImpl.channelGetList.add(this);
                }
                channelGetRequester.channelGetConnect(this, pvStructure,bitSet);
            }
            
            private boolean firstTime = true;
            private ChannelImpl channelImpl;
            private ChannelGetRequester channelGetRequester;
            private PVStructure pvStructure;
            private PVCopy pvCopy;
            private boolean process;
            private boolean success = false;
            private ChannelProcessor channelProcessor = null;
            private BitSet bitSet = null;
            private boolean isDestroyed = false;
            private boolean lastRequest = false;
            // following for share
            private BitSet monitorBitSet = null;
            private BitSet overrunBitSet = null;
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
                if(process) channelProcessor.detach();
                synchronized(channelImpl.channelGetList) {
                    channelImpl.channelGetList.remove(this);
                }
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
                this.lastRequest = lastRequest;
                bitSet.clear();
                if(process) {
                    channelProcessor.requestProcess();
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
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
                if(channelProcessor.process(true, null)) return;
                message("process request failed",MessageType.error);
                success = false;
                channelGetRequester.getDone(success);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                getData();
                channelProcessor.setInactive();
                channelGetRequester.getDone(success);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessResult(boolean)
             */
            @Override
            public void recordProcessResult(boolean success) {
                this.success = success;
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
                return channelGetRequester.getRequesterName();
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
        
        private class ChannelPutImpl implements ChannelPut,ChannelProcessorRequester
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
                if(process) {
                    this.process = true;
                    PVRecord pvRecord = channelImpl.pvRecord;
                    channelProcessor = channelImpl.requestChannelProcessor(pvRecord, this);
                    if(channelProcessor==null) {
                        channelPutRequester.message(
                                "can not process", MessageType.error);
                        this.process = false;
                    }
                }
                synchronized(channelImpl.channelPutList) {
                    channelImpl.channelPutList.add(this);
                }
                channelPutRequester.channelPutConnect(this, pvStructure,bitSet);
            }
            
            private ChannelImpl channelImpl;
            private ChannelPutRequester channelPutRequester = null;
            private PVStructure pvStructure;
            private PVCopy pvCopy;
            private boolean process;
            private boolean success = false;
            private ChannelProcessor channelProcessor = null;
            private BitSet bitSet = null;
            private boolean isDestroyed = false;
            private boolean lastRequest = false;
            
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPut#destroy()
             */
            public void destroy() {
                if(isDestroyed) return;
                isDestroyed = true;
                if(process) channelProcessor.detach();
                synchronized(channelImpl.channelPutList) {
                    channelImpl.channelPutList.remove(this);
                }
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
                success = true;
                this.lastRequest = lastRequest;
                if(process) {
                    channelProcessor.requestProcess();
                    return;
                }
                pvRecord.lock();
                try {
                    putData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutRequester.putDone(success);
                return;
            } 
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
                boolean isActive = channelProcessor.setActive();
                if(isActive) {
                    putData();
                    channelProcessor.process(false, null);
                    return;
                } else {
                    success = false;
                    message("process request failed",MessageType.error);
                    channelPutRequester.putDone(success);
                    if(lastRequest) destroy();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                channelPutRequester.putDone(success);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessResult(boolean)
             */
            @Override
            public void recordProcessResult(boolean success) {
                this.success = success;
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
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelPutRequester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelPutRequester.message(message, messageType);
            }
            
            private void putData() {
               pvCopy.updateRecord(pvStructure, bitSet, false);
            }
            
            private void getData() {
                bitSet.clear();
                bitSet.set(0);
                pvCopy.updateCopyFromBitSet(pvStructure, bitSet, false);
             }
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,ChannelProcessorRequester
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
                if(process) {
                    this.process = true;
                    PVRecord pvRecord = channelImpl.pvRecord;
                    channelProcessor = channelImpl.requestChannelProcessor(pvRecord, this);
                    if(channelProcessor==null) {
                        channelPutGetRequester.message(
                                "can not process", MessageType.error);
                        this.process = false;
                    }
                }
                synchronized(channelImpl.channelPutGetList) {
                    channelImpl.channelPutGetList.add(this);
                }
                channelPutGetRequester.channelPutGetConnect(this, pvPutStructure,pvGetStructure);
            }
            
            private ChannelImpl channelImpl;
            private ChannelPutGetRequester channelPutGetRequester = null;
            private PVStructure pvPutStructure;
            private PVCopy pvPutCopy;
            private PVStructure pvGetStructure;
            private PVCopy pvGetCopy;
            private boolean process;
            private boolean success = false;
            private ChannelProcessor channelProcessor = null;
            private BitSet putBitSet = null;
            private BitSet getBitSet = null;
            private boolean isDestroyed = false;
            private boolean lastRequest = false;
           
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#destroy()
             */
            @Override
            public void destroy() {
                isDestroyed = true;
                if(process) channelProcessor.detach();
                synchronized(channelImpl.channelPutGetList) {
                    channelImpl.channelPutGetList.remove(this);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.ca.ChannelPutGet#putGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup)
             */
            @Override
            public void putGet(boolean lastRequest)
            {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    channelPutGetRequester.message(
                        "channel is destroyed",MessageType.info);
                    channelPutGetRequester.putGetDone(false);
                    return;
                }
                success = true;
                this.lastRequest = lastRequest;
                if(process) {
                    channelProcessor.requestProcess();
                    return;
                }
                pvRecord.lock();
                try {
                    putData();
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutGetRequester.putGetDone(success);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
                boolean isActive = channelProcessor.setActive();
                if(isActive) {
                    putData();
                    channelProcessor.process(true, null);
                    return;
                } else {
                    success = false;
                    channelPutGetRequester.putGetDone(success);
                    if(lastRequest) destroy();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                getData();
                channelProcessor.setInactive();
                channelPutGetRequester.putGetDone(success);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.channelAccess.ChannelProcessorRequester#recordProcessResult(boolean)
             */
            @Override
            public void recordProcessResult(boolean success) {
                this.success = success;
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutGet#getGet()
             */
            @Override
            public void getGet() {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    message("channel is destroyed",MessageType.info);
                    channelPutGetRequester.getGetDone(false);
                    return;
                }
                pvRecord.lock();
                try {
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutGetRequester.getGetDone(true);
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelPutGet#getPut()
             */
            @Override
            public void getPut() {
                if(isDestroyed || channelImpl.isDestroyed()) {
                    message("channel is destroyed",MessageType.info);
                    channelPutGetRequester.getPutDone(false);
                    return;
                }
                pvRecord.lock();
                try {
                    getPutData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutGetRequester.getPutDone(true);
                
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelPutGetRequester.getRequesterName();
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
                putBitSet.set(0);
                pvPutCopy.updateRecord(pvPutStructure, putBitSet, false);
            }
            
            private void getData() {
                pvGetCopy.updateCopySetBitSet(pvGetStructure, getBitSet, false);
                getBitSet.clear();
                getBitSet.set(0);
            }
            
            private void getPutData() {
                pvPutCopy.updateCopySetBitSet(pvPutStructure, putBitSet, false);
                putBitSet.clear();
                putBitSet.set(0);
            }
        }
        
        private class ChannelArrayImpl implements ChannelArray {

            private ChannelArrayImpl(ChannelImpl channelImpl,
                    ChannelArrayRequester channelArrayRequester,
                    PVArray pvArray,PVArray pvCopy)
            {
                this.channelImpl = channelImpl;
                this.channelArrayRequester = channelArrayRequester;
                this.pvArray = pvArray;
                this.pvCopy = pvCopy;
                pvRecord = channelImpl.pvRecord;

                synchronized(channelImpl.channelArrayList) {
                    channelImpl.channelArrayList.add(this);
                }
                channelArrayRequester.channelArrayConnect(this, pvCopy);
            }

            private ChannelImpl channelImpl;
            private ChannelArrayRequester channelArrayRequester;
            private PVArray pvArray;
            private PVArray pvCopy;
            private PVRecord pvRecord;
            private boolean isDestroyed = false;

            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelRequest#destroy()
             */
            @Override
            public void destroy() {
                if(isDestroyed) return;
                isDestroyed = true;
                synchronized(channelImpl.channelArrayList) {
                    channelImpl.channelArrayList.remove(this);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelArray#getArray(boolean, int, int)
             */
            @Override
            public void getArray(boolean lastRequest, int offset, int count) {
                if(isDestroyed) return;
                if(count<=0) count = pvArray.getLength();
                pvRecord.lock();
                try {
                    int len = convert.copyArray(pvArray, offset, pvCopy, 0, count);
                    if(!pvCopy.isImmutable()) pvCopy.setLength(len);
                } finally  {
                    pvRecord.unlock();
                }
                channelArrayRequester.getArrayDone(true);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvData.channelAccess.ChannelArray#putArray(boolean, int, int)
             */
            @Override
            public void putArray(boolean lastRequest, int offset, int count) {
                if(isDestroyed) return;
                if(count<=0) count = pvCopy.getLength();
                pvRecord.lock();
                try {
                    convert.copyArray(pvCopy, 0, pvArray, offset, count);
                } finally  {
                    pvRecord.unlock();
                }
                channelArrayRequester.putArrayDone(true);
                if(lastRequest) destroy();
            }
        }
    }
}
