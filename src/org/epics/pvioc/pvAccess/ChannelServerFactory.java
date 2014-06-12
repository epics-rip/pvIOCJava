/**
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvAccess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.pvaccess.client.AccessRights;
import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelListRequester;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelArray;
import org.epics.pvaccess.client.ChannelArrayRequester;
import org.epics.pvaccess.client.ChannelFind;
import org.epics.pvaccess.client.ChannelFindRequester;
import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvaccess.client.ChannelProcess;
import org.epics.pvaccess.client.ChannelProcessRequester;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderFactory;
import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutGet;
import org.epics.pvaccess.client.ChannelPutGetRequester;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvaccess.client.ChannelRPC;
import org.epics.pvaccess.client.ChannelRPCRequester;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvaccess.client.GetFieldRequester;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVStructureArray;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Type;
import org.epics.pvdata.pv.*;
import org.epics.pvdata.copy.*;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordClient;

import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.util.RequestResult;
import org.epics.pvioc.monitor.*;

/**
 * Factory and implementation of local channel access, i.e. channel access that
 * accesses database records in the local pvDatabase..
 * User callbacks are called with the appropriate record locked except for
 * 1) all methods of ChannelRequester, 2) all methods of ChannelFieldGroupListener,
 * and 3) ChannelRequester.requestDone
 * @author mrk
 *
 */
public class ChannelServerFactory  {
     
    static public ChannelProvider getChannelServer() {
        return ChannelServerLocal.getChannelServer();
    }
    private static final String providerName = "local";
    private static final PVDatabase pvDatabase = PVDatabaseFactory.getMaster();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final Convert convert = ConvertFactory.getConvert();
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static final Status notFoundStatus = statusCreate.createStatus(StatusType.ERROR, "channel not found", null);
    private static final Status strideNotSupportedStatus = statusCreate.createStatus(StatusType.WARNING, "stride not supported", null);
    private static final Status capacityImmutableStatus = statusCreate.createStatus(StatusType.ERROR, "capacity is immutable", null);
    private static final Status subFieldDoesNotExistStatus = statusCreate.createStatus(StatusType.ERROR, "subField does not exist", null);
    private static final Status cannotProcessErrorStatus = statusCreate.createStatus(StatusType.ERROR, "can not process", null);
    private static final Status cannotProcessWarningStatus = statusCreate.createStatus(StatusType.WARNING, "can not process", null);
    private static final Status subFieldNotArrayStatus = statusCreate.createStatus(StatusType.ERROR, "subField is not an array", null);
    private static final Status channelDestroyedStatus = statusCreate.createStatus(StatusType.ERROR, "channel destroyed", null);
    private static final Status requestDestroyedStatus = statusCreate.createStatus(StatusType.ERROR, "request destroyed", null);
    private static final Status illegalRequestStatus = statusCreate.createStatus(StatusType.ERROR, "illegal pvRequest", null);
    private static final Status notImplementedStatus = statusCreate.createStatus(StatusType.ERROR, "not implemented", null);
    private static ChannelFind channelFind = null;
    private static LinkedList<Channel> channelList = new LinkedList<Channel>();
   
   private static class ChannelFindLocal implements ChannelFind {
        
        private ChannelFindLocal() {
        }   
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelFind#cancel()
         */
        @Override
        public void cancel() {}
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelFind#getChannelProvider()
         */
        @Override
        public ChannelProvider getChannelProvider() {
            return getChannelServer();
        } 
    }
   
    private static class ChannelServerLocal implements ChannelProvider{
    	private ChannelServerLocal(){} // don't allow creation except by getChannelServer. 
    	private static ChannelServerLocal singleImplementation = null;
    	private static synchronized ChannelServerLocal getChannelServer() {
            if (singleImplementation==null) {
                singleImplementation = new ChannelServerLocal();
                channelFind = new ChannelFindLocal();
                ChannelProviderRegistryFactory.registerChannelProviderFactory(
                		new ChannelProviderFactory() {
							
							@Override
							public ChannelProvider sharedInstance() {
								return singleImplementation;
							}
							
							@Override
							public ChannelProvider newInstance() {
								throw new RuntimeException("not supported");
							}
							
							@Override
							public String getFactoryName() {
								return providerName;
							}
						});
            }
            return singleImplementation;
    }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelProvider#destroy()
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
         * @see org.epics.pvaccess.client.ChannelProvider#getProviderName()
         */
        @Override
        public String getProviderName() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelProvider#channelFind(java.lang.String, org.epics.pvaccess.client.ChannelFindRequester)
         */
        @Override
        public ChannelFind channelFind(String channelName,ChannelFindRequester channelFindRequester) {
        	if (channelFindRequester == null)
        		throw new IllegalArgumentException("null channelFindRequester");
        	if (channelName == null)
        		throw new IllegalArgumentException("null channelName");
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                PVDatabase beingInstalled = PVDatabaseFactory.getBeingInstalled();
                if(beingInstalled!=null) pvRecord = beingInstalled.findRecord(channelName);
            }
            boolean wasFound = ((pvRecord==null) ? false : true);
            channelFindRequester.channelFindResult(okStatus, channelFind, wasFound);
            return channelFind;
        }
        @Override
        public ChannelFind channelList(ChannelListRequester channelListRequester) {
        	if (channelListRequester == null)
        		throw new IllegalArgumentException("null channelListRequester");
        	Set<String> channelNamesSet = new HashSet<String>(Arrays.asList(pvDatabase.getRecordNames()));
            channelListRequester.channelListResult(okStatus, channelFind, channelNamesSet, false);
            return channelFind;
        }
        @Override
		public Channel createChannel(String channelName,
				ChannelRequester channelRequester, short priority,
				String address) {
        	if (address != null)
        		throw new IllegalArgumentException("address not allowed for local implementation");
        	return createChannel(channelName, channelRequester, priority);
		}
		/* (non-Javadoc)
         * @see org.epics.pvaccess.client.ChannelProvider#createChannel(java.lang.String, org.epics.pvaccess.client.ChannelRequester, short)
         */
        @Override
        public Channel createChannel(String channelName,ChannelRequester channelRequester, short priority) {
        	if (channelRequester == null)
        		throw new IllegalArgumentException("null channelRequester");
        	if (channelName == null)
        		throw new IllegalArgumentException("null channelName");
        	if (priority < PRIORITY_MIN || priority > PRIORITY_MAX)
        		throw new IllegalArgumentException("priority out of bounds");
        	
            PVRecord pvRecord = pvDatabase.findRecord(channelName);
            if(pvRecord==null) {
                PVDatabase beingInstalled = PVDatabaseFactory.getBeingInstalled();
                if(beingInstalled!=null) pvRecord = beingInstalled.findRecord(channelName);
            }
            boolean wasFound = ((pvRecord==null) ? false : true);
            if(wasFound) {
                ChannelImpl channel = new ChannelImpl(this,pvRecord,channelRequester);
                channelRequester.channelCreated(okStatus, channel);
                synchronized(channelList) {
                    channelList.add(channel);
                }
                channelRequester.channelStateChange(channel, ConnectionState.CONNECTED);
               return channel;
            } else {
                channelRequester.channelCreated(notFoundStatus, null);
                return null;
            }
        }
    }
    
    private static class ChannelImpl implements Channel,PVRecordClient{
    	private final ChannelProvider provider;
        private final PVRecord pvRecord;
        private final ChannelRequester channelRequester;
        private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
        
        private ChannelImpl(ChannelProvider provider,PVRecord pvRecord,ChannelRequester channelRequester)
        {
        	this.provider = provider;
            this.pvRecord = pvRecord;
            this.channelRequester = channelRequester;
            pvRecord.registerClient(this);
        }       
        protected PVRecord getPVRecord() {
        	return pvRecord;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#getRequesterName()
         */
        @Override
        public String getRequesterName() {
            return channelRequester.getRequesterName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.Requester#message(java.lang.String, org.epics.pvdata.pv.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            channelRequester.message(message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#isConnected()
         */
        @Override
        public boolean isConnected() {
            return !isDestroyed.get();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#destroy()
         */
        @Override
        public void destroy() {
            if(!isDestroyed.compareAndSet(false, true)) return;
            pvRecord.unregisterClient(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVRecordClient#detach(org.epics.pvdata.pv.PVRecord)
         */
        @Override
		public void detach(PVRecord pvRecord) {
			destroy();
		}
		/* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getConnectionState()
         */
        @Override
        public ConnectionState getConnectionState() {
        	if (isDestroyed.get())
        		return ConnectionState.DESTROYED;
        	else
        		return ConnectionState.CONNECTED;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getRemoteAddress()
         */
        @Override
        public String getRemoteAddress() {
            return providerName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getField(org.epics.pvaccess.client.GetFieldRequester, java.lang.String)
         */
        @Override
        public void getField(GetFieldRequester requester,String subField) {
        	if (requester == null)
        		throw new IllegalArgumentException("null requester");
            if(isDestroyed.get()) {
            	requester.getDone(channelDestroyedStatus, null);
            	return;
            }
            if(subField==null || subField.length()<1) {
                requester.getDone(okStatus, pvRecord.getPVRecordStructure().getPVStructure().getStructure());
                return;
            }
            PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField(subField);
            if(pvField==null) {
                requester.getDone(subFieldDoesNotExistStatus, null);
            } else {
                requester.getDone(okStatus, pvField.getField());
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createChannelProcess(org.epics.pvaccess.client.ChannelProcessRequester, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public ChannelProcess createChannelProcess(
                ChannelProcessRequester channelProcessRequester,
                PVStructure pvRequest)
        {
        	if (channelProcessRequester == null)
        		throw new IllegalArgumentException("null channelProcessRequester");
            if(isDestroyed.get()) {
            	channelProcessRequester.channelProcessConnect(channelDestroyedStatus, null);
            	return null;
            }
            ChannelProcessImpl channelProcess = new ChannelProcessImpl(this,channelProcessRequester);
            if(channelProcess.canProcess()) {
                channelProcessRequester.channelProcessConnect(okStatus, channelProcess);
                return channelProcess;
            } else {
                channelProcessRequester.channelProcessConnect(cannotProcessErrorStatus, null);
                return null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createChannelGet(org.epics.pvaccess.client.ChannelGetRequester, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public ChannelGet createChannelGet(
                ChannelGetRequester channelGetRequester, PVStructure pvRequest)
        {
        	if (channelGetRequester == null)
        		throw new IllegalArgumentException("null channelGetRequester");
        	if (pvRequest == null)
        		throw new IllegalArgumentException("null pvRequest");
            if(isDestroyed.get()) {
            	channelGetRequester.channelGetConnect(channelDestroyedStatus, null, null);
            	return null;
            }
            PVCopy pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
            if(pvCopy==null) {
                channelGetRequester.channelGetConnect(illegalRequestStatus, null, null);
                return null;
            }
            PVStructure pvStructure = pvCopy.createPVStructure();
            return new ChannelGetImpl(this,channelGetRequester,pvStructure,pvCopy,getProcess(pvRequest,false));
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createChannelPut(org.epics.pvaccess.client.ChannelPutRequester, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public ChannelPut createChannelPut(ChannelPutRequester channelPutRequester, PVStructure pvRequest)
        {
        	if (channelPutRequester == null)
        		throw new IllegalArgumentException("null channelPutRequester");
        	if (pvRequest == null)
        		throw new IllegalArgumentException("null pvRequest");
            if(isDestroyed.get()) {
            	channelPutRequester.channelPutConnect(channelDestroyedStatus, null, null);
            	return null;
            }
        	PVCopy pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        	if(pvCopy==null) {
                channelPutRequester.channelPutConnect(illegalRequestStatus, null, null);
                return null;
        	}
            return new ChannelPutImpl(this,channelPutRequester,pvCopy,getProcess(pvRequest,true));
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createChannelPutGet(org.epics.pvaccess.client.ChannelPutGetRequester, org.epics.pvdata.pv.PVStructure, boolean, org.epics.pvdata.pv.PVStructure, boolean, boolean, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public ChannelPutGet createChannelPutGet(
                ChannelPutGetRequester channelPutGetRequester,
                PVStructure pvRequest)
        {
        	if (channelPutGetRequester == null)
        		throw new IllegalArgumentException("null channelPutRequester");
        	if (pvRequest == null)
        		throw new IllegalArgumentException("null pvRequest");
            if(isDestroyed.get()) {
            	channelPutGetRequester.channelPutGetConnect(channelDestroyedStatus, null, null, null);
            	return null;
            }
            boolean process = getProcess(pvRequest,true);
            PVField pvField = pvRequest.getSubField("putField");
            if(pvField==null || pvField.getField().getType()!=Type.structure) {
            	channelPutGetRequester.message("pvRequest does not have a putField request structure", MessageType.error);
            	channelPutGetRequester.message(pvRequest.toString(),MessageType.warning);
            	channelPutGetRequester.channelPutGetConnect(illegalRequestStatus, null, null, null);
            	return null;
            }
        	PVCopy pvPutCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest, "putField");
        	if(pvPutCopy==null) {
                channelPutGetRequester.channelPutGetConnect(illegalRequestStatus, null, null, null);
                return null;
            }
        	pvField = pvRequest.getSubField("getField");
            if(pvField==null || pvField.getField().getType()!=Type.structure) {
            	channelPutGetRequester.message("pvRequest does not have a getField request structure", MessageType.error);
            	channelPutGetRequester.message(pvRequest.toString(),MessageType.warning);
            	channelPutGetRequester.channelPutGetConnect(illegalRequestStatus, null, null, null);
            	return null;
            }
        	PVCopy pvGetCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest, "getField");
        	if(pvGetCopy==null) {
                channelPutGetRequester.channelPutGetConnect(illegalRequestStatus, null, null, null);
                return null;
            }
        	
            return new ChannelPutGetImpl(this,channelPutGetRequester,pvPutCopy,pvGetCopy,process);
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createChannelRPC(org.epics.pvaccess.client.ChannelRPCRequester, org.epics.pvdata.pv.PVStructure)
         */
        @Override
		public ChannelRPC createChannelRPC(
				ChannelRPCRequester channelRPCRequester, PVStructure pvRequest)
        {
            channelRPCRequester.channelRPCConnect(notImplementedStatus,null);
            return null;
        }
		/* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createMonitor(org.epics.pvdata.monitor.MonitorRequester, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public Monitor createMonitor(
                MonitorRequester monitorRequester,
                PVStructure pvRequest)
        {
        	if (monitorRequester == null)
        		throw new IllegalArgumentException("null channelPutRequester");
        	if (pvRequest == null)
        		throw new IllegalArgumentException("null pvRequest");
            if(isDestroyed.get()) {
            	monitorRequester.monitorConnect(channelDestroyedStatus, null, null);
            	return null;
            }
            return MonitorFactory.create(pvRecord,monitorRequester, pvRequest);
            
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#createChannelArray(org.epics.pvaccess.client.ChannelArrayRequester, java.lang.String, org.epics.pvdata.pv.PVStructure)
         */
        @Override
        public ChannelArray createChannelArray(
                ChannelArrayRequester channelArrayRequester, PVStructure pvRequest)
        {
        	if (channelArrayRequester == null)
        		throw new IllegalArgumentException("null channelArrayRequester");
            if(isDestroyed.get()) {
            	channelArrayRequester.channelArrayConnect(channelDestroyedStatus, null, null);
            	return null;
            }
            PVField[] pvFields = pvRequest.getPVFields();
            if(pvFields.length!=1) {
                channelArrayRequester.channelArrayConnect(illegalRequestStatus, null, null);
                return null;
            }
            PVField pvField = pvFields[0];
            String fieldName = "";
            while(pvField!=null) {
                String name = pvField.getFieldName();
                if(name!=null && name.length()>0) {
                    if(fieldName.length()>0) fieldName += '.';
                    fieldName += name;
                }
                PVStructure pvs = (PVStructure)pvField;
                pvFields = pvs.getPVFields();
                if(pvFields.length!=1) break;
                pvField = pvFields[0];
            }
            pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField(fieldName);
            if(pvField==null) {
                channelArrayRequester.channelArrayConnect(illegalRequestStatus, null, null);
                return null;
            }
            if(pvField.getField().getType()==Type.structureArray) {
            	PVStructureArray pvArray = (PVStructureArray)pvField;
            	PVStructureArray pvCopy = pvDataCreate.createPVStructureArray(pvArray.getStructureArray());
            	return new ChannelStructureArrayImpl(this,channelArrayRequester,pvArray,pvCopy);
            }
            if(pvField.getField().getType()==Type.scalarArray) {
                PVScalarArray pvArray = (PVScalarArray)pvField;
                PVScalarArray pvCopy = pvDataCreate.createPVScalarArray(pvArray.getScalarArray().getElementType());
                return new ChannelScalarArrayImpl(this,channelArrayRequester,pvArray,pvCopy);
            }
            if(pvField.getField().getType()==Type.unionArray) {
                PVUnionArray pvArray = (PVUnionArray)pvField;
                PVUnionArray pvCopy = pvDataCreate.createPVUnionArray(pvArray.getUnionArray());
                return new ChannelUnionArrayImpl(this,channelArrayRequester,pvArray,pvCopy);
            }
            channelArrayRequester.channelArrayConnect(subFieldNotArrayStatus, null, null);
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getAccessRights(org.epics.pvdata.pv.PVField)
         */
        @Override
        public AccessRights getAccessRights(PVField pvField) {
            // TODO Auto-generated method stub
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getChannelRequester()
         */
        @Override
        public ChannelRequester getChannelRequester() {
            return channelRequester;
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getChannelName()
         */
        @Override
        public String getChannelName() {
            return pvRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvaccess.client.Channel#getProvider()
         */
        @Override
        public ChannelProvider getProvider() {
            return provider;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "{ name = " + pvRecord.getRecordName() + (isDestroyed.get() ? " disconnected }" : " connected }" ); 
        }
        
        private boolean getProcess(PVStructure pvRequest,boolean processDefault) {
        	PVField pvField = pvRequest.getSubField("record._options.process");
        	if(pvField==null || pvField.getField().getType()!=Type.scalar) return processDefault;
        	Scalar scalar = (Scalar)pvField.getField();
        	if(scalar.getScalarType()==ScalarType.pvString) {
        		PVString pvString = (PVString)pvField;
        		return (pvString.get().equalsIgnoreCase("true")) ? true : false;
        	} else if(scalar.getScalarType()==ScalarType.pvBoolean) {
        		PVBoolean pvBoolean = (PVBoolean)pvField;
        		return pvBoolean.get();
        	}
        	return processDefault;
        }
        
        private static class ChannelProcessImpl implements ChannelProcess,RecordProcessRequester
        {
            ChannelProcessImpl(ChannelImpl channelImpl,ChannelProcessRequester channelProcessRequester)
            {
                this.channelImpl = channelImpl;
                this.channelProcessRequester = channelProcessRequester;
                recordProcess = channelImpl.getPVRecord().getRecordProcess();
                processToken = recordProcess.requestProcessToken(this);
            }
            
            boolean canProcess() {
                return (processToken==null) ? false : true;
            }
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            private final ChannelImpl channelImpl;
            private final ChannelProcessRequester channelProcessRequester;
            private final RecordProcess recordProcess;
            private final ProcessToken processToken;
            private Status status= null;
            
            @Override
            public Channel getChannel() {
                return channelImpl;
            }

            @Override
            public void cancel() {
                destroy();
            }

            @Override
            public void lastRequest() {}

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelProcess#destroy()
             */
            @Override
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true)) return;
                if(processToken!=null) {
                	recordProcess.releaseProcessToken(processToken);
                }
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelProcess#process(boolean)
             */
            @Override
            public void process() {
                if(isDestroyed.get()) {
                    channelProcessRequester.processDone(requestDestroyedStatus,this);
                    return;
                }
                recordProcess.queueProcessRequest(processToken);
            }
           
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
            	recordProcess.process(processToken, false);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
             */
            @Override
			public void canNotProcess(String reason) {
            	message(reason,MessageType.error);
            	channelProcessRequester.processDone(cannotProcessErrorStatus,this);
			}
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                channelProcessRequester.processDone(status,this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
             */
            @Override
			public void lostRightToProcess() {
            	message("lost ability to process",MessageType.fatalError);
				channelImpl.destroy();
			}
			@Override
			public void recordProcessResult(RequestResult requestResult) {
				if(requestResult!=RequestResult.success) {
	                status = statusCreate.createStatus(StatusType.ERROR, "requestResult " + requestResult.toString(), null);
	                return;
	            }
	            status = okStatus;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelProcessRequester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelProcessRequester.message(message, messageType);
            }
            
			@Override
			public void lock() {
			}
			@Override
			public void unlock() {
			}
        }
        
        private class ChannelGetImpl implements ChannelGet,RecordProcessRequester
        {
            private ChannelGetImpl(ChannelImpl channelImpl,ChannelGetRequester channelGetRequester,PVStructure pvStructure,PVCopy pvCopy,boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelGetRequester = channelGetRequester;
                this.pvStructure = pvStructure;
                this.pvCopy = pvCopy;
                bitSet = new BitSet(pvStructure.getNumberFields());
                Status status = okStatus;
                recordProcess = channelImpl.getPVRecord().getRecordProcess();
                if(process) {
                    processToken = recordProcess.requestProcessToken(this);
                    if(processToken==null) status = cannotProcessWarningStatus;
                } else {
                	processToken = null;
                }
                channelGetRequester.channelGetConnect(status, this, pvStructure.getStructure());
            }
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            private final ChannelImpl channelImpl;
            private final ChannelGetRequester channelGetRequester;
            private final PVStructure pvStructure;
            private final PVCopy pvCopy;
            private final BitSet bitSet;
            private final RecordProcess recordProcess;
            private final ProcessToken processToken;
            private boolean firstTime = true;
            private Status status = null;
            private boolean lastRequest = false;
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#getChannel()
             */
            @Override
            public Channel getChannel() {
                return channelImpl;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#cancel()
             */
            @Override
            public void cancel() {
                destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#lastRequest()
             */
            @Override
            public void lastRequest() {}
            
            /* (non-Javadoc)
             * @see org.epics.pvdata.misc.Destroyable#destroy()
             */
            @Override
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true)) return;
                if(processToken!=null) {
                	recordProcess.releaseProcessToken(processToken);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelGet#get()
             */
            @Override
            public void get() {
                if(isDestroyed.get()) {
                    channelGetRequester.getDone(requestDestroyedStatus,this,null,null);
                    return;
                }
                bitSet.clear();
                if(processToken!=null) {
                	recordProcess.queueProcessRequest(processToken);
                    return;
                }
                pvRecord.lock();
                try {
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelGetRequester.getDone(okStatus,this,pvStructure,bitSet);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
            	recordProcess.process(processToken,true);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
             */
            @Override
			public void canNotProcess(String reason) {
            	message(reason,MessageType.error);
            	channelGetRequester.getDone(cannotProcessErrorStatus,this,null,null);
                if(lastRequest) destroy();
			}
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
            	recordProcess.setInactive(processToken);
                channelGetRequester.getDone(status,this,pvStructure,bitSet);
                if(lastRequest) destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.server.ChannelProcessorRequester#recordProcessResult(org.epics.pvdata.pv.Status)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
                getData();
                if(requestResult!=RequestResult.success) {
	                status = statusCreate.createStatus(StatusType.ERROR, "requestResult " + requestResult.toString(), null);
	                return;
	            }
	            status = okStatus;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
             */
            @Override
			public void lostRightToProcess() {
            	message("lost ability to process",MessageType.fatalError);
				channelImpl.destroy();
			}
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelGetRequester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelGetRequester.message(message, messageType);
            }
            
            private void getData() {
                pvCopy.updateCopySetBitSet(pvStructure, bitSet);
                if(firstTime) {
                    bitSet.clear();
                    bitSet.set(0);
                    firstTime = false;
                } 
            }
			@Override
			public void lock() {
				pvRecord.lock();
			}
			@Override
			public void unlock() {
				pvRecord.unlock();
			}
        }
        
        private class ChannelPutImpl implements ChannelPut,RecordProcessRequester
        {
            private ChannelPutImpl(
                    ChannelImpl channelImpl,
                    ChannelPutRequester channelPutRequester,
                    PVCopy pvCopy,
                    boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelPutRequester = channelPutRequester;
                this.pvCopy = pvCopy;
                Status status = okStatus;
                recordProcess = channelImpl.getPVRecord().getRecordProcess();
                if(process) {
                    processToken = recordProcess.requestProcessToken(this);
                    if(processToken==null) status = cannotProcessWarningStatus;
                } else {
                	processToken = null;
                }
                channelPutRequester.channelPutConnect(status, this, pvCopy.getStructure());
            }
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            private final ChannelImpl channelImpl;
            private final ChannelPutRequester channelPutRequester;
            private final PVCopy pvCopy;
            private final RecordProcess recordProcess;
            private final ProcessToken processToken;
            private PVStructure pvStructure = null;
            private BitSet bitSet = null;
            private Status status = null;            
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#getChannel()
             */
            @Override
            public Channel getChannel() {
                return channelImpl;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#cancel()
             */
            @Override
            public void cancel() {
                destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#lastRequest()
             */
            @Override
            public void lastRequest() {}
            
            
            /* (non-Javadoc)
             * @see org.epics.pvdata.misc.Destroyable#destroy()
             */
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true))  return;
                if(processToken!=null) {
                	recordProcess.releaseProcessToken(processToken);
                }
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPut#put(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
             */
            @Override
            public void put(PVStructure pvPutStructure, BitSet bitSet) {
                if(isDestroyed.get()) {
                    channelPutRequester.putDone(requestDestroyedStatus,this);
                    return;
                }
                status = okStatus;
                this.pvStructure = pvPutStructure;
                this.bitSet = bitSet;
                if(processToken!=null) {
                    recordProcess.queueProcessRequest(processToken);
                    return;
                }
                pvRecord.lock();
                try {
                    putData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutRequester.putDone(status,this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
            	pvRecord.lock();
                try {
                    putData();
                } finally {
                    pvRecord.unlock();
                }
                recordProcess.process(processToken,false);
            	return;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
             */
            @Override
			public void canNotProcess(String reason) {
            	message(reason,MessageType.error);
            	channelPutRequester.putDone(cannotProcessErrorStatus,this);
			}
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                channelPutRequester.putDone(status,this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.RequestResult)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
            	if(requestResult!=RequestResult.success) {
	                status = statusCreate.createStatus(StatusType.ERROR, "requestResult " + requestResult.toString(), null);
	                return;
	            }
	            status = okStatus;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
             */
            @Override
			public void lostRightToProcess() {
            	message("lost ability to process",MessageType.fatalError);
				channelImpl.destroy();
			}
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPut#get()
             */
            @Override
            public void get() {
                if(isDestroyed.get()) {
                    channelPutRequester.getDone(requestDestroyedStatus,this,null,null);
                    return;
                }
                pvRecord.lock();
                try {
                    pvStructure = pvCopy.createPVStructure();
                    bitSet = new BitSet(pvStructure.getNumberFields());
                    pvCopy.initCopy(pvStructure, bitSet);
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutRequester.getDone(okStatus,this,pvStructure,bitSet);
                pvStructure = null;
                bitSet = null;
                return;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelPutRequester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelPutRequester.message(message, messageType);
            }
            
            private void putData() {
               pvCopy.updateMaster(pvStructure, bitSet);
            }
            
            private void getData() {
                bitSet.clear();
                bitSet.set(0);
                pvCopy.updateCopyFromBitSet(pvStructure, bitSet);
             }
			@Override
			public void lock() {
				pvRecord.lock();
			}
			@Override
			public void unlock() {
				pvRecord.unlock();
			}
        }
        
        private class ChannelPutGetImpl implements ChannelPutGet,RecordProcessRequester
        {
            private ChannelPutGetImpl(
                    ChannelImpl channelImpl,
                    ChannelPutGetRequester channelPutGetRequester,
                    PVCopy pvPutCopy,
                    PVCopy pvGetCopy,
                    boolean process)
            {
                this.channelImpl = channelImpl;
                this.channelPutGetRequester = channelPutGetRequester;
                this.pvPutCopy = pvPutCopy;
                this.pvGetCopy = pvGetCopy;
                pvGetStructure = pvGetCopy.createPVStructure();
                getBitSet = new BitSet(pvGetStructure.getNumberFields());
                Status status = okStatus;
                recordProcess = channelImpl.getPVRecord().getRecordProcess();
                if(process) {
                    processToken = recordProcess.requestProcessToken(this);
                    if(processToken==null) status = cannotProcessWarningStatus;
                } else {
                	processToken = null;
                }
                channelPutGetRequester.channelPutGetConnect(status, this,pvPutCopy.getStructure(),pvGetCopy.getStructure());
            }
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            private final ChannelImpl channelImpl;
            private ChannelPutGetRequester channelPutGetRequester;
            private final PVCopy pvPutCopy;
            private final PVCopy pvGetCopy;
            private final PVStructure pvGetStructure;
            private final BitSet getBitSet;
            private final RecordProcess recordProcess;
            private final ProcessToken processToken;
            private Status status;
            private PVStructure pvPutStructure = null;
            private BitSet putBitSet = null;
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#getChannel()
             */
            @Override
            public Channel getChannel() {
                return channelImpl;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#cancel()
             */
            @Override
            public void cancel() {
                destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#lastRequest()
             */
            @Override
            public void lastRequest() {}
            
            /* (non-Javadoc)
             * @see org.epics.pvdata.misc.Destroyable#destroy()
             */
            @Override
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true)) return;
                if(processToken!=null) {
                	recordProcess.releaseProcessToken(processToken);
                }
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGet#putGet(org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
             */
            @Override
            public void putGet(PVStructure pvPutStructure, BitSet bitSet)
            {
                if(isDestroyed.get()) {
                    channelPutGetRequester.putGetDone(requestDestroyedStatus,this,null,null);
                    return;
                }
                status = okStatus;
                this.pvPutStructure = pvPutStructure;
                this.putBitSet = bitSet;
                if(processToken!=null) {
                	recordProcess.queueProcessRequest(processToken);
                    return;
                }
                pvRecord.lock();
                try {
                    putData();
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutGetRequester.putGetDone(status,this,pvGetStructure,getBitSet);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
             */
            @Override
            public void becomeProcessor() {
            	pvRecord.lock();
            	try {
            		putData();
            	} finally {
            		pvRecord.unlock();
            	}
            	recordProcess.process(processToken,true);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
             */
            @Override
			public void canNotProcess(String reason) {
            	getData();
            	message(reason,MessageType.error);
            	channelPutGetRequester.putGetDone(cannotProcessErrorStatus,this,null,null);
			}
            /* (non-Javadoc)
             * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
             */
            @Override
            public void recordProcessComplete() {
                recordProcess.setInactive(processToken);
                channelPutGetRequester.putGetDone(status,this,pvGetStructure,getBitSet);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.server.ChannelProcessorRequester#recordProcessResult(org.epics.pvdata.pv.Status)
             */
            @Override
            public void recordProcessResult(RequestResult requestResult) {
            	 getData();
                 if(requestResult!=RequestResult.success) {
 	                status = statusCreate.createStatus(StatusType.ERROR, "requestResult " + requestResult.toString(), null);
 	                return;
 	            }
 	            status = okStatus;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.server.ChannelProcessorRequester#lostRightToProcess()
             */
            @Override
			public void lostRightToProcess() {
            	message("lost ability to process",MessageType.fatalError);
				channelImpl.destroy();
			}
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGet#getGet()
             */
            @Override
            public void getGet() {
                if(isDestroyed.get()) {
                    channelPutGetRequester.getGetDone(requestDestroyedStatus,this,null,null);
                    return;
                }
                pvRecord.lock();
                try {
                    getData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutGetRequester.getGetDone(okStatus,this,pvGetStructure,getBitSet);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelPutGet#getPut()
             */
            @Override
            public void getPut() {
                if(isDestroyed.get()) {
                    channelPutGetRequester.getPutDone(requestDestroyedStatus,this,null,null);
                    return;
                }
                pvRecord.lock();
                try {
                    getPutData();
                } finally {
                    pvRecord.unlock();
                }
                channelPutGetRequester.getPutDone(okStatus,this,pvPutStructure,putBitSet);
                
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.pv.Requester#getRequesterName()
             */
            @Override
            public String getRequesterName() {
                return channelPutGetRequester.getRequesterName();
            }     
            /* (non-Javadoc)
             * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
             */
            @Override
            public void message(String message, MessageType messageType) {
                channelPutGetRequester.message(message, messageType);
            }
            
            private void putData() {
                pvPutCopy.updateMaster(pvPutStructure, putBitSet);
                pvPutStructure = null;
                putBitSet = null;
            }
            
            private void getData() {
                getBitSet.clear();
                pvGetCopy.updateCopySetBitSet(pvGetStructure, getBitSet);
            }
            
            private void getPutData() {
                pvPutStructure = pvPutCopy.createPVStructure();
                putBitSet = new BitSet(pvPutStructure.getNumberFields());
                pvPutCopy.initCopy(pvPutStructure, putBitSet);
            }
            @Override
            public void lock() {
                pvRecord.lock();
            }
            @Override
            public void unlock() {
               pvRecord.unlock();
            }
        }
        
        private static class ChannelScalarArrayImpl implements ChannelArray {
            private ChannelScalarArrayImpl(ChannelImpl channelImpl,
                    ChannelArrayRequester channelArrayRequester,
                    PVScalarArray pvArray,PVScalarArray pvCopy)
            {
                this.channelImpl = channelImpl;
                this.channelArrayRequester = channelArrayRequester;
                this.pvArray = pvArray;
                this.pvCopy = pvCopy;
                pvRecord = channelImpl.pvRecord;
                channelArrayRequester.channelArrayConnect(okStatus, this, pvArray.getArray());
            }

            private ChannelImpl channelImpl;
            private ChannelArrayRequester channelArrayRequester;
            private PVScalarArray pvArray;
            private PVScalarArray pvCopy;
            private PVRecord pvRecord;
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#getChannel()
             */
            @Override
            public Channel getChannel() {
                return channelImpl;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#cancel()
             */
            @Override
            public void cancel() {
                destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#lastRequest()
             */
            @Override
            public void lastRequest() {}
            
            /* (non-Javadoc)
             * @see org.epics.pvdata.misc.Destroyable#destroy()
             */
            @Override
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true)) return;
            }

            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#getArray(int, int, int)
             */
            public void getArray(int offset, int count,int stride) {
                if(isDestroyed.get()) {
                	channelArrayRequester.getArrayDone(requestDestroyedStatus,this,null);
                	return;
                }
                if(count<=0) count = pvArray.getLength() - offset;
                pvRecord.lock();
                try {
                    int len = convert.copyScalarArray(pvArray, offset, pvCopy, 0, count);
                    if(len>0) pvCopy.setLength(len);
                } finally  {
                    pvRecord.unlock();
                }
                Status status = okStatus;
                if(stride!=1) status = strideNotSupportedStatus;
                channelArrayRequester.getArrayDone(status,this,pvCopy);
            }
           
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#putArray(org.epics.pvdata.pv.PVArray, int, int, int)
             */
            public void putArray(PVArray putArray,int offset, int count,int stride) {
                if(isDestroyed.get()) {
                	channelArrayRequester.putArrayDone(requestDestroyedStatus,this);
                	return;
                }
                if(count<=0) count = pvArray.getLength() - offset;
                pvRecord.lock();
                try {
                    convert.copyScalarArray((PVScalarArray)putArray, 0, pvArray, offset, count);
                } finally  {
                    pvRecord.unlock();
                }
                Status status = okStatus;
                if(stride!=1) status = strideNotSupportedStatus;
                channelArrayRequester.putArrayDone(status,this);
            }
			
			/* (non-Javadoc)
			 * @see org.epics.pvaccess.client.ChannelArray#getLength()
			 */
			@Override
			public void getLength() {
			    int length = 0;
			    int capacity = 0; 
			    pvRecord.lock();
			    try {
			        length = pvArray.getLength();
			        capacity = pvArray.getCapacity();
			    } finally {
			        pvRecord.unlock();
			    }
			    channelArrayRequester.getLengthDone(okStatus, this,length,capacity);
			}
            /* (non-Javadoc)
			 * @see org.epics.pvaccess.client.ChannelArray#setLength(int, int)
			 */
			public void setLength(int length, int capacity) {
				if(isDestroyed.get()) {
                	channelArrayRequester.setLengthDone(requestDestroyedStatus,this);
                	return;
                }
				if(capacity>=0 && !pvArray.isCapacityMutable()) {
					channelArrayRequester.setLengthDone(capacityImmutableStatus,this);
					return;
				}
				pvRecord.lock();
                try {
                    if(capacity>=0) {
                    	if(pvArray.getCapacity()!=capacity) pvArray.setCapacity(capacity);
                    }
                    if(length>=0) {
                    	if(pvArray.getLength()!=length) pvArray.setLength(length);
                    }
                } finally  {
                    pvRecord.unlock();
                }
                channelArrayRequester.setLengthDone(okStatus,this);
			}
			
			@Override
			public void lock() {
				pvRecord.lock();
			}
			@Override
			public void unlock() {
				pvRecord.unlock();
			}
        }
        
        private static class ChannelStructureArrayImpl implements ChannelArray {
            private ChannelStructureArrayImpl(ChannelImpl channelImpl,
                    ChannelArrayRequester channelArrayRequester,
                    PVStructureArray pvArray,PVStructureArray pvCopy)
            {
                this.channelImpl = channelImpl;
                this.channelArrayRequester = channelArrayRequester;
                this.pvArray = pvArray;
                this.pvCopy = pvCopy;
                pvRecord = channelImpl.pvRecord;
                channelArrayRequester.channelArrayConnect(okStatus, this, pvArray.getArray());
            }

            private ChannelImpl channelImpl;
            private ChannelArrayRequester channelArrayRequester;
            private PVStructureArray pvArray;
            private PVStructureArray pvCopy;
            private PVRecord pvRecord;
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#getChannel()
             */
            @Override
            public Channel getChannel() {
                return channelImpl;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#cancel()
             */
            @Override
            public void cancel() {
                destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#lastRequest()
             */
            @Override
            public void lastRequest() {}
            
            /* (non-Javadoc)
             * @see org.epics.pvdata.misc.Destroyable#destroy()
             */
            @Override
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true)) return;
            }
           
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#getArray(int, int, int)
             */
            public void getArray(int offset, int count,int stride) {
                if(isDestroyed.get()) {
                	channelArrayRequester.getArrayDone(requestDestroyedStatus,this,null);
                	return;
                }
                pvRecord.lock();
                try {
                    if(count==0) count = pvArray.getLength() - offset;
                    if(count>0) {
                        int len = convert.copyStructureArray(pvArray,offset, pvCopy,0,count);
                        if(len>0) pvCopy.setLength(len);
                    }

                } finally  {
                    pvRecord.unlock();
                }
                Status status = okStatus;
                if(stride!=1) status = strideNotSupportedStatus;
                channelArrayRequester.getArrayDone(status,this,pvCopy);
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#putArray(org.epics.pvdata.pv.PVArray, int, int, int)
             */
            public void putArray(PVArray pvCopy, int offset, int count, int stride) {
                if(isDestroyed.get()) {
                	channelArrayRequester.putArrayDone(requestDestroyedStatus,this);
                	return;
                }
                pvRecord.lock();
                try {
                	convert.copyStructureArray((PVStructureArray)pvCopy,0 ,pvArray,offset,count);
                } finally  {
                    pvRecord.unlock();
                }
                Status status = okStatus;
                if(stride!=1) status = strideNotSupportedStatus;
                channelArrayRequester.putArrayDone(status,this);
            }
			
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#getLength()
             */
            public void getLength()
            {
               int length = 0;
               int capacity = 0;
               pvRecord.lock();
               try {
                   length = pvArray.getLength();
                   capacity = pvArray.getCapacity();
               }finally  {
                   pvRecord.unlock();
               }
               channelArrayRequester.getLengthDone(okStatus, this,length,capacity);
            }
            
			/* (non-Javadoc)
			 * @see org.epics.pvaccess.client.ChannelArray#setLength(int, int)
			 */
			public void setLength(int length, int capacity) {
				if(isDestroyed.get()) {
                	channelArrayRequester.setLengthDone(requestDestroyedStatus,this);
                	return;
                }
				if(capacity>=0 && !pvArray.isCapacityMutable()) {
					channelArrayRequester.setLengthDone(capacityImmutableStatus,this);
					return;
				}
				pvRecord.lock();
                try {
                    if(length>=0) {
                    	if(pvArray.getLength()!=length) pvArray.setLength(length);
                    }
                    if(capacity>=0) {
                    	if(pvArray.getCapacity()!=capacity) pvArray.setCapacity(capacity);
                    }
                } finally  {
                    pvRecord.unlock();
                }
                channelArrayRequester.setLengthDone(okStatus,this);
			}
			/* (non-Javadoc)
			 * @see org.epics.pvaccess.client.Lockable#lock()
			 */
			@Override
			public void lock() {
				pvRecord.lock();
			}
			/* (non-Javadoc)
			 * @see org.epics.pvaccess.client.Lockable#unlock()
			 */
			@Override
			public void unlock() {
				pvRecord.unlock();
			}
        }
        
        private static class ChannelUnionArrayImpl implements ChannelArray {
            private ChannelUnionArrayImpl(ChannelImpl channelImpl,
                    ChannelArrayRequester channelArrayRequester,
                    PVUnionArray pvArray,PVUnionArray pvCopy)
            {
                this.channelImpl = channelImpl;
                this.channelArrayRequester = channelArrayRequester;
                this.pvArray = pvArray;
                this.pvCopy = pvCopy;
                pvRecord = channelImpl.pvRecord;
                channelArrayRequester.channelArrayConnect(okStatus, this, pvArray.getArray());
            }

            private ChannelImpl channelImpl;
            private ChannelArrayRequester channelArrayRequester;
            private PVUnionArray pvArray;
            private PVUnionArray pvCopy;
            private PVRecord pvRecord;
            private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#getChannel()
             */
            @Override
            public Channel getChannel() {
                return channelImpl;
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#cancel()
             */
            @Override
            public void cancel() {
                destroy();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelRequest#lastRequest()
             */
            @Override
            public void lastRequest() {}
            
            /* (non-Javadoc)
             * @see org.epics.pvdata.misc.Destroyable#destroy()
             */
            @Override
            public void destroy() {
                if(!isDestroyed.compareAndSet(false, true)) return;
            }
           
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#getArray(int, int, int)
             */
            public void getArray(int offset, int count,int stride) {
                if(isDestroyed.get()) {
                    channelArrayRequester.getArrayDone(requestDestroyedStatus,this,null);
                    return;
                }
                pvRecord.lock();
                try {
                    if(count==0) count = pvArray.getLength() - offset;
                    if(count>0) {
                        int len = convert.copyUnionArray(pvArray,offset, pvCopy,0,count);
                        if(len>0) pvCopy.setLength(len);
                    }
                } finally  {
                    pvRecord.unlock();
                }
                Status status = okStatus;
                if(stride!=1) status = strideNotSupportedStatus;
                channelArrayRequester.getArrayDone(status,this,pvCopy);
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#putArray(org.epics.pvdata.pv.PVArray, int, int, int)
             */
            public void putArray(PVArray pvCopy, int offset, int count, int stride) {
                if(isDestroyed.get()) {
                    channelArrayRequester.putArrayDone(requestDestroyedStatus,this);
                    return;
                }
                pvRecord.lock();
                try {
                    convert.copyUnionArray((PVUnionArray)pvCopy,0, pvArray,offset,count);
                } finally  {
                    pvRecord.unlock();
                }
                Status status = okStatus;
                if(stride!=1) status = strideNotSupportedStatus;
                channelArrayRequester.putArrayDone(status,this);
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#getLength()
             */
            public void getLength()
            {
               int length = 0;
               int capacity = 0;
               pvRecord.lock();
               try {
                   length = pvArray.getLength();
                   capacity = pvArray.getCapacity();
               }finally  {
                   pvRecord.unlock();
               }
               channelArrayRequester.getLengthDone(okStatus, this,length,capacity);
            }
            
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.ChannelArray#setLength(int, int)
             */
            public void setLength(int length, int capacity) {
                if(isDestroyed.get()) {
                    channelArrayRequester.setLengthDone(requestDestroyedStatus,this);
                    return;
                }
                if(capacity>=0 && !pvArray.isCapacityMutable()) {
                    channelArrayRequester.setLengthDone(capacityImmutableStatus,this);
                    return;
                }
                pvRecord.lock();
                try {
                    if(length>=0) {
                        if(pvArray.getLength()!=length) pvArray.setLength(length);
                    }
                    if(capacity>=0) {
                        if(pvArray.getCapacity()!=capacity) pvArray.setCapacity(capacity);
                    }
                } finally  {
                    pvRecord.unlock();
                }
                channelArrayRequester.setLengthDone(okStatus,this);
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.Lockable#lock()
             */
            @Override
            public void lock() {
                pvRecord.lock();
            }
            /* (non-Javadoc)
             * @see org.epics.pvaccess.client.Lockable#unlock()
             */
            @Override
            public void unlock() {
                pvRecord.unlock();
            }
        }

    }
}
