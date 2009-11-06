/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.epics.ca.client.AccessRights;
import org.epics.ca.client.ChannelArray;
import org.epics.ca.client.ChannelArrayRequester;
import org.epics.ca.client.ChannelFind;
import org.epics.ca.client.ChannelFindRequester;
import org.epics.ca.client.ChannelGet;
import org.epics.ca.client.ChannelGetRequester;
import org.epics.ca.client.ChannelProcess;
import org.epics.ca.client.ChannelProcessRequester;
import org.epics.ca.client.ChannelProvider;
import org.epics.ca.client.ChannelPut;
import org.epics.ca.client.ChannelPutGet;
import org.epics.ca.client.ChannelPutGetRequester;
import org.epics.ca.client.ChannelPutRequester;
import org.epics.ca.client.ChannelRequester;
import org.epics.ca.client.GetFieldRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.StatusFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Status.StatusType;



/**
 * Base class that implements V3Channel.
 * @author mrk
 *
 */
public class BaseV3Channel implements
ChannelFind,org.epics.ca.client.Channel,
V3Channel,ConnectionListener,Runnable,V3ChannelStructureRequester
{
    private static Executor executor = ExecutorFactory.create("caV3Connect", ThreadPriority.low);
    private boolean isDestroyed = false;
    private final ChannelProvider channelProvider;
    private final ChannelFindRequester channelFindRequester;
    private final ChannelRequester channelRequester;
    private final Context context;
    private final String pvName;
    private final String recordName;
    private final String valueFieldName;
    private final String[] propertyNames;
    private final ScalarType enumRequestType;
    
    private final ExecutorNode executorNode;
    
    private AtomicBoolean synchCreateChannel = new AtomicBoolean(false);
    private AtomicBoolean gotFirstConnection = new AtomicBoolean(false);
    
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static Status notSupportedStatus = statusCreate.createStatus(StatusType.ERROR, "not supported", null);
    private static Status channelNotConnectedStatus = statusCreate.createStatus(StatusType.ERROR, "channel not connected", null);
   
    private V3ChannelStructure v3ChannelStructure = null;

    
    private gov.aps.jca.Channel jcaChannel = null;
    
    private LinkedList<ChannelGet> channelGetList =
        new LinkedList<ChannelGet>();
    private LinkedList<ChannelPut> channelPutList =
        new LinkedList<ChannelPut>();
    private LinkedList<Monitor> monitorList = 
        new LinkedList<Monitor>();
    /**
     * The constructor.
     * @param listener The ChannelListener.
     * @param enumRequestType Request type for ENUM native type.
     */
    BaseV3Channel(
    		ChannelProvider channelProvider,
            ChannelFindRequester channelFindRequester,
            ChannelRequester channelRequester,
            Context context,
            String pvName,
            String recordName,
            String valueFieldName,
            ScalarType enumRequestType,
            String[] propertyNames)
    {
    	this.channelProvider = channelProvider;
        this.channelFindRequester = channelFindRequester;
        this.channelRequester = channelRequester;
        this.context = context;
        this.pvName = pvName;
        this.recordName = recordName;
        this.valueFieldName = valueFieldName;
        this.enumRequestType = enumRequestType;
        this.propertyNames = propertyNames;
        executorNode = executor.createNode(this);
    }
    
    public void connectCaV3() {
        try {
            jcaChannel = context.createChannel(pvName,this);
            if(synchCreateChannel.getAndSet(false)) { // connectionChanged was called synchronously
                executor.execute(executorNode);
            }
        } catch (CAException e) {
            if(channelFindRequester!=null)
            	channelFindRequester.channelFindResult(
            		statusCreate.createStatus(StatusType.FATAL, "failed to create channel", e),
            		this, false);
            jcaChannel = null;
        };
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.channelAccess.ChannelFind#cancelChannelFind()
     */
    @Override
    public void cancelChannelFind() {
        jcaChannel.dispose();
        jcaChannel = null;
        v3ChannelStructure = null;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.channelAccess.ChannelFind#getChannelProvider()
     */
    @Override
    public ChannelProvider getChannelProvider() {
        return ClientFactory.channelProvider;
    }

    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getConnectionState()
     */
    @Override
    public ConnectionState getConnectionState() {
        gov.aps.jca.Channel.ConnectionState connectionState = jcaChannel.getConnectionState();
        if(connectionState==gov.aps.jca.Channel.ConnectionState.DISCONNECTED) return ConnectionState.DISCONNECTED;
        if(connectionState==gov.aps.jca.Channel.ConnectionState.CONNECTED) return ConnectionState.CONNECTED;
        if(connectionState==gov.aps.jca.Channel.ConnectionState.NEVER_CONNECTED) return ConnectionState.NEVER_CONNECTED;
        if(connectionState==gov.aps.jca.Channel.ConnectionState.CLOSED) return ConnectionState.DISCONNECTED;
        throw new RuntimeException("unknown connection state");
    }

    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getRemoteAddress()
     */
    @Override
    public String getRemoteAddress() {
        return jcaChannel.getHostName();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.ca.client.ChannelGet)
     */
    @Override
    public boolean add(ChannelGet channelGet)
    {
        boolean result = false;
        synchronized(channelGetList) {
            result = channelGetList.add(channelGet);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.ca.client.ChannelPut)
     */
    @Override
    public boolean add(ChannelPut channelPut)
    {
        boolean result = false;
        synchronized(channelPutList) {
            result = channelPutList.add(channelPut);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.monitor.Monitor)
     */
    @Override
    public boolean add(Monitor monitor)
    {
        boolean result = false;
        synchronized(monitorList) {
            result = monitorList.add(monitor);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.ca.client.ChannelGet)
     */
    @Override
    public boolean remove(ChannelGet channelGet) {
        boolean result = false;
        synchronized(channelGetList) {
            result = channelGetList.remove(channelGet);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.ca.client.ChannelPut)
     */
    @Override
    public boolean remove(ChannelPut channelPut) {
        boolean result = false;
        synchronized(channelPutList) {
            result = channelPutList.remove(channelPut);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.monitor.Monitor)
     */
    @Override
    public boolean remove(Monitor monitor) {
        boolean result = false;
        synchronized(monitorList) {
            result = monitorList.remove(monitor);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelArray(org.epics.ca.client.ChannelArrayRequester, java.lang.String, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelArray createChannelArray(
            ChannelArrayRequester channelArrayRequester, String subField,
            PVStructure pvOption)
    {
        channelArrayRequester.channelArrayConnect(notSupportedStatus, null, null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelGet(org.epics.ca.client.ChannelGetRequester, org.epics.pvData.pv.PVStructure, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelGet createChannelGet(ChannelGetRequester channelGetRequester,
            PVStructure pvRequest,boolean shareData,
            boolean process, PVStructure pvOption)
    {
        if(v3ChannelStructure==null) {
            channelGetRequester.channelGetConnect(channelNotConnectedStatus, null, null, null);
            return null;
        }
        BaseV3ChannelGet channelGet = new BaseV3ChannelGet(channelGetRequester,process);
        channelGet.init(this);
        return channelGet;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createMonitor(org.epics.pvData.monitor.MonitorRequester, org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public Monitor createMonitor(
            MonitorRequester monitorRequester,
            PVStructure pvRequest,
            PVStructure pvOption)
    {
        if(v3ChannelStructure==null) {
            monitorRequester.monitorConnect(channelNotConnectedStatus,null,null);
            return null;
        }
        BaseV3Monitor monitor = new BaseV3Monitor(monitorRequester);
        monitor.init(this);
        return monitor;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelProcess(org.epics.ca.client.ChannelProcessRequester, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelProcess createChannelProcess(
            ChannelProcessRequester channelProcessRequester,
            PVStructure pvOption)
    {
        channelProcessRequester.channelProcessConnect(notSupportedStatus,null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelPut(org.epics.ca.client.ChannelPutRequester, org.epics.pvData.pv.PVStructure, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelPut createChannelPut(ChannelPutRequester channelPutRequester,
            PVStructure pvRequest,boolean shareData,
            boolean process, PVStructure pvOption)
    {
        if(v3ChannelStructure==null) {
            channelPutRequester.channelPutConnect(channelNotConnectedStatus, null, null, null);
            return null;
        }
        BaseV3ChannelPut channelPut = new BaseV3ChannelPut(channelPutRequester,process);
        channelPut.init(this);
        return channelPut;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelPutGet(org.epics.ca.client.ChannelPutGetRequester, org.epics.pvData.pv.PVStructure, boolean, org.epics.pvData.pv.PVStructure, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelPutGet createChannelPutGet(
            ChannelPutGetRequester channelPutGetRequester,
            PVStructure pvPutRequest,
            boolean sharePutData, PVStructure pvGetRequest,
            boolean shareGetData, boolean process,
            PVStructure pvOption)
    {
        channelPutGetRequester.channelPutGetConnect(notSupportedStatus, null, null, null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#destroy()
     */
    @Override
    public void destroy() {
        if(isDestroyed) return;
        isDestroyed = true;
        while(!channelGetList.isEmpty()) {
            ChannelGet channelGet = channelGetList.getFirst();
            channelGet.destroy();
        }
        while(!channelPutList.isEmpty()) {
            ChannelPut channelPut = channelPutList.getFirst();
            channelPut.destroy();
        }
        while(!monitorList.isEmpty()) {
            Monitor monitor = monitorList.getFirst();
            monitor.destroy();
        }
        try {
            jcaChannel.destroy();
        } catch (CAException e) {
            if(channelRequester!=null) channelRequester.message("destroy caused CAException " + e.getMessage(), MessageType.error);
            jcaChannel = null;
        }
        jcaChannel = null;
        v3ChannelStructure = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getAccessRights(org.epics.pvData.pv.PVField)
     */
    @Override
    public AccessRights getAccessRights(PVField pvField) {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getChannelName()
     */
    @Override
    public String getChannelName() {
        return pvName;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getChannelRequester()
     */
    @Override
    public ChannelRequester getChannelRequester() {
        return channelRequester;
    }
    private static final Status subFieldDoesNotExistStatus = statusCreate.createStatus(StatusType.ERROR, "subField does not exist", null);
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getField(org.epics.ca.client.GetFieldRequester, java.lang.String)
     */
    @Override
    public void getField(GetFieldRequester requester, String subField) {
        if(v3ChannelStructure==null) {
            requester.getDone(channelNotConnectedStatus,null);
            return;
        }
        PVStructure pvStructure = v3ChannelStructure.getPVStructure();
        if(subField==null) subField = "value";
        PVField pvField = pvStructure.getSubField(subField);
        if(pvField==null) {
            requester.getDone(subFieldDoesNotExistStatus,null);
        } else {
            requester.getDone(okStatus,pvField.getField());
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getProvider()
     */
    @Override
    public ChannelProvider getProvider() {
        return channelProvider;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#isConnected()
     */
    @Override
    public boolean isConnected() {
        return (jcaChannel.getConnectionState()==Channel.ConnectionState.CONNECTED ? true : false);
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
     * @see org.epics.ioc.caV3.V3Channel#getJcaChannel()
     */
    public Channel getJCAChannel() {
        return jcaChannel;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getPVName()
     */
    public String getPVName() {
        return pvName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getV3ChannelRecord()
     */
    public V3ChannelStructure getV3ChannelStructure() {
        return v3ChannelStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getPropertyNames()
     */
    public String[] getPropertyNames() {
        return propertyNames;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getValueFieldName()
     */
    public String getValueFieldName() {
        return valueFieldName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getExecutor()
     */
    public Executor getExecutor() {
        return executor;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getEnumRequestType()
     */
    public ScalarType getEnumRequestScalarType() {
        return enumRequestType;
    }
    
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        boolean isConnected = arg0.isConnected();
        if(isConnected) {
            if(gotFirstConnection.getAndSet(true)) {
                channelRequester.channelStateChange(this, ConnectionState.CONNECTED);
                return;
            }
            if(!synchCreateChannel.getAndSet(true)) {
                executor.execute(executorNode);
            }
        } else {
            channelRequester.channelStateChange(this, ConnectionState.DISCONNECTED);
            message("connection lost", MessageType.warning);
        }
    }
    private static final Status createStructureFailedStatus = statusCreate.createStatus(StatusType.ERROR, "createPVStructure failed", null);
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if(channelFindRequester!=null) {
            channelFindRequester.channelFindResult(okStatus,this, true);
            destroy();
            return;
        }
        v3ChannelStructure = new BaseV3ChannelStructure(this);
        if(v3ChannelStructure.createPVStructure(this,recordName)) {
            channelRequester.channelCreated(okStatus,this);
            channelRequester.channelStateChange(this, ConnectionState.CONNECTED);
            return;
        } else {
            channelRequester.channelCreated(createStructureFailedStatus,null);
        }
        destroy();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelStructureRequester#createPVStructureDone(org.epics.ioc.util.RequestResult)
     */
    public void createPVStructureDone(RequestResult requestResult) {
        if(requestResult==RequestResult.success) {
            channelRequester.channelCreated(okStatus,this);
        } else {
            channelRequester.channelCreated(createStructureFailedStatus,null);
            destroy();
        }
    }
}