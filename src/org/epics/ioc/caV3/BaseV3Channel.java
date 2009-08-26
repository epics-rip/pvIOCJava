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

import org.epics.ca.channelAccess.client.AccessRights;
import org.epics.ca.channelAccess.client.ChannelArray;
import org.epics.ca.channelAccess.client.ChannelArrayRequester;
import org.epics.ca.channelAccess.client.ChannelFind;
import org.epics.ca.channelAccess.client.ChannelFindRequester;
import org.epics.ca.channelAccess.client.ChannelGet;
import org.epics.ca.channelAccess.client.ChannelGetRequester;
import org.epics.ca.channelAccess.client.ChannelProcess;
import org.epics.ca.channelAccess.client.ChannelProcessRequester;
import org.epics.ca.channelAccess.client.ChannelProvider;
import org.epics.ca.channelAccess.client.ChannelPut;
import org.epics.ca.channelAccess.client.ChannelPutGet;
import org.epics.ca.channelAccess.client.ChannelPutGetRequester;
import org.epics.ca.channelAccess.client.ChannelPutRequester;
import org.epics.ca.channelAccess.client.ChannelRequester;
import org.epics.ca.channelAccess.client.GetFieldRequester;
import org.epics.ioc.util.RequestResult;
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



/**
 * Base class that implements V3Channel.
 * @author mrk
 *
 */
public class BaseV3Channel implements
ChannelFind,org.epics.ca.channelAccess.client.Channel,
V3Channel,ConnectionListener,Runnable,V3ChannelStructureRequester
{
    private static final String providerName = "caV3";
    private static Executor executor = ExecutorFactory.create("caV3Connect", ThreadPriority.low);
    private boolean isDestroyed = false;
    private ChannelFindRequester channelFindRequester = null;
    private Context context = null;
    private String pvName = null;
    private String recordName = null;
    private String valueFieldName = null;
    private String[] propertyNames = null;
    private ScalarType enumRequestType = null;
    
    private ChannelRequester channelRequester = null;
    private ExecutorNode executorNode = null;
    private boolean isCreatingChannel = false;
    private boolean synchCreateChannel = false;
    private boolean gotFirstConnection = false;
    
    
   
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
            ChannelFindRequester channelFindRequester,
            ChannelRequester channelRequester,
            Context context,
            String pvName,
            String recordName,
            String valueFieldName,
            ScalarType enumRequestType,
            String[] propertyNames)
    {
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
            isCreatingChannel = true;
            synchCreateChannel = false;
            jcaChannel = context.createChannel(pvName,this);
            isCreatingChannel = false;
            if(synchCreateChannel) { // connectionChanged was called synchronously
                run();
                synchCreateChannel = false;
            }
        } catch (CAException e) {
            if(channelFindRequester!=null) channelFindRequester.channelFindResult(this, false);
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
     * @see org.epics.ca.channelAccess.client.Channel#getConnectionState()
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
     * @see org.epics.ca.channelAccess.client.Channel#getRemoteAddress()
     */
    @Override
    public String getRemoteAddress() {
        return jcaChannel.getHostName();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.ca.channelAccess.client.ChannelGet)
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
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.ca.channelAccess.client.ChannelPut)
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
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.ca.channelAccess.client.ChannelGet)
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
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.ca.channelAccess.client.ChannelPut)
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
     * @see org.epics.ca.channelAccess.client.Channel#createChannelArray(org.epics.ca.channelAccess.client.ChannelArrayRequester, java.lang.String, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelArray createChannelArray(
            ChannelArrayRequester channelArrayRequester, String subField,
            PVStructure pvOption)
    {
        channelArrayRequester.channelArrayConnect(null, null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#createChannelGet(org.epics.ca.channelAccess.client.ChannelGetRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelGet createChannelGet(ChannelGetRequester channelGetRequester,
            PVStructure pvRequest, String structureName, boolean shareData,
            boolean process, PVStructure pvOption)
    {
        if(v3ChannelStructure==null) {
            channelGetRequester.message(
                    "createChannelGet but not connected",MessageType.warning);
            channelGetRequester.channelGetConnect(null, null, null);
            return null;
        }
        BaseV3ChannelGet channelGet = new BaseV3ChannelGet(channelGetRequester,process);
        channelGet.init(this);
        return channelGet;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#createMonitor(org.epics.pvData.monitor.MonitorRequester, org.epics.pvData.pv.PVStructure, java.lang.String, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public Monitor createMonitor(
            MonitorRequester monitorRequester,
            PVStructure pvRequest,
            String structureName, PVStructure pvOption)
    {
        if(v3ChannelStructure==null) {
            monitorRequester.message(
                    "createMonitor but not connected",MessageType.warning);
            monitorRequester.monitorConnect(null,null);
            return null;
        }
        BaseV3Monitor monitor = new BaseV3Monitor(monitorRequester);
        monitor.init(this);
        return monitor;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#createChannelProcess(org.epics.ca.channelAccess.client.ChannelProcessRequester, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelProcess createChannelProcess(
            ChannelProcessRequester channelProcessRequester,
            PVStructure pvOption)
    {
        channelProcessRequester.message(
                "createChannelProcess not supported. Issue a put to .PROC",MessageType.warning);
        channelProcessRequester.channelProcessConnect(null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#createChannelPut(org.epics.ca.channelAccess.client.ChannelPutRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelPut createChannelPut(ChannelPutRequester channelPutRequester,
            PVStructure pvRequest, String structureName, boolean shareData,
            boolean process, PVStructure pvOption)
    {
        if(v3ChannelStructure==null) {
            channelPutRequester.message(
                    "createChannelPut but not connected",MessageType.warning);
            channelPutRequester.channelPutConnect(null, null, null);
            return null;
        }
        BaseV3ChannelPut channelPut = new BaseV3ChannelPut(channelPutRequester,process);
        channelPut.init(this);
        return channelPut;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#createChannelPutGet(org.epics.ca.channelAccess.client.ChannelPutGetRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelPutGet createChannelPutGet(
            ChannelPutGetRequester channelPutGetRequester,
            PVStructure pvPutRequest, String putStructureName,
            boolean sharePutData, PVStructure pvGetRequest,
            String getStructureName, boolean shareGetData, boolean process,
            PVStructure pvOption)
    {
        channelPutGetRequester.channelPutGetConnect(null, null, null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#destroy()
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
     * @see org.epics.ca.channelAccess.client.Channel#disconnect()
     */
    public void disconnect() {
        if(isDestroyed) return;
        if(channelRequester!=null) channelRequester.message("do not know how to disconnect. ", MessageType.error);
    }                      
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#getAccessRights(org.epics.pvData.pv.PVField)
     */
    @Override
    public AccessRights getAccessRights(PVField pvField) {
        // TODO Auto-generated method stub
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#getChannelName()
     */
    @Override
    public String getChannelName() {
        return pvName;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#getChannelRequester()
     */
    @Override
    public ChannelRequester getChannelRequester() {
        return channelRequester;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#getField(org.epics.ca.channelAccess.client.GetFieldRequester, java.lang.String)
     */
    @Override
    public void getField(GetFieldRequester requester, String subField) {
        if(v3ChannelStructure==null) {
            requester.message(
                    "not connected",MessageType.warning);
            requester.getDone(null);
        }
        PVStructure pvStructure = v3ChannelStructure.getPVStructure();
        if(subField==null) subField = "value";
        PVField pvField = pvStructure.getSubField(subField);
        if(pvField==null) {
            message("subField " + subField + " not found",MessageType.error);
            requester.getDone(null);
        } else {
            requester.getDone(pvField.getField());
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#getProviderName()
     */
    @Override
    public String getProviderName() {
        return providerName;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.Channel#isConnected()
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
     * @see org.epics.ca.channelAccess.client.Channel#connect()
     */
    @Override
    public void connect() {
        channelRequester.channelStateChange(this, true);
    }
    
    
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        boolean isConnected = arg0.isConnected();
        if(isConnected) {
            if(gotFirstConnection) return;
            gotFirstConnection = true;
            if(isCreatingChannel) {
                synchCreateChannel = true;
                return;
            }
            executor.execute(executorNode);
        } else {
            message("connection lost", MessageType.warning);
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if(channelFindRequester!=null) {
            channelFindRequester.channelFindResult(this, true);
            disconnect();
            return;
        }
        v3ChannelStructure = new BaseV3ChannelStructure(this);
        if(v3ChannelStructure.createPVStructure(this,recordName)) {
            channelRequester.channelCreated(this);
            return;
        } else {
            channelRequester.message("createPVStructure failed", MessageType.error);
            channelRequester.channelNotCreated();
        }
        disconnect();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelStructureRequester#createPVStructureDone(org.epics.ioc.util.RequestResult)
     */
    public void createPVStructureDone(RequestResult requestResult) {
        if(requestResult==RequestResult.success) {
            channelRequester.channelCreated(this);
        } else {
            channelRequester.message("createPVStructure failed", MessageType.error);
            channelRequester.channelNotCreated();
            disconnect();
        }
    }
}