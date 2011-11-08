/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.dbr.DBRType;
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
import org.epics.ca.client.ChannelRPC;
import org.epics.ca.client.ChannelRPCRequester;
import org.epics.ca.client.ChannelRequester;
import org.epics.ca.client.GetFieldRequester;
import org.epics.ioc.database.PVDatabase;
import org.epics.ioc.database.PVDatabaseFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.StatusFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.Status.StatusType;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Type;

/**
 * Base class that implements V3Channel.
 * @author mrk
 *
 */
public class BaseV3Channel implements
ChannelFind,org.epics.ca.client.Channel,
V3Channel,ConnectionListener
{
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static Executor executor = ExecutorFactory.create("caV3", ThreadPriority.low);
    
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static final Status notSupportedStatus = statusCreate.createStatus(StatusType.ERROR, "not supported", null);
    private static final Status channelNotConnectedStatus = statusCreate.createStatus(StatusType.ERROR, "channel not connected", null);
    private static final Status subFieldDoesNotExistStatus = statusCreate.createStatus(StatusType.ERROR, "subField does not exist", null);
    
    private final ChannelProvider channelProvider;
    private final ChannelFindRequester channelFindRequester;
    private final ChannelRequester channelRequester;
    private final Context context;
    private final String channelName;
    
    private final AtomicBoolean synchCreateChannel = new AtomicBoolean(false);
    private final AtomicBoolean gotFirstConnection = new AtomicBoolean(false);
    
    private final LinkedList<ChannelGet> channelGetList = new LinkedList<ChannelGet>();
    private final LinkedList<ChannelPut> channelPutList = new LinkedList<ChannelPut>();
    private final LinkedList<Monitor> monitorList =  new LinkedList<Monitor>();
    
    private gov.aps.jca.Channel jcaChannel = null;
    private volatile boolean isDestroyed = false;
    /**
     * The constructor.
     * @param channelProvider The channelProvider.
     * @param channelFindRequester The channelFind requester.
     * @param channelRequester The channel requester.
     * @param context The context.
     * @param channelName The channelName.
     */
    BaseV3Channel(
    		ChannelProvider channelProvider,
            ChannelFindRequester channelFindRequester,
            ChannelRequester channelRequester,
            Context context,
            String channelName)
    {
    	this.channelProvider = channelProvider;
        this.channelFindRequester = channelFindRequester;
        this.channelRequester = channelRequester;
        this.context = context;
        this.channelName = channelName;
    }
    
    public void connectCaV3() {
        try {
            jcaChannel = context.createChannel(channelName,this);
            if(synchCreateChannel.getAndSet(false)) { // connectionChanged was called synchronously
                if(channelFindRequester!=null) {
                    channelFindRequester.channelFindResult(okStatus,this, true);
                    destroy();
                    return;
                }
                channelRequester.channelCreated(okStatus,this);
                channelRequester.channelStateChange(this, ConnectionState.CONNECTED);
            }
        } catch (CAException e) {
            if(channelFindRequester!=null)
            	channelFindRequester.channelFindResult(
            		statusCreate.createStatus(StatusType.FATAL, "failed to create channel", e),
            		this, false);
            channelRequester.channelCreated(channelNotConnectedStatus, null);
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
            ChannelArrayRequester channelArrayRequester, PVStructure pvRequest)
    {
        channelArrayRequester.channelArrayConnect(notSupportedStatus, null, null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelGet(org.epics.ca.client.ChannelGetRequester, org.epics.pvData.pv.PVStructure, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelGet createChannelGet(ChannelGetRequester channelGetRequester,
            PVStructure pvRequest)
    {
        BaseV3ChannelGet channelGet = new BaseV3ChannelGet(channelGetRequester);
        channelGet.init(this,pvRequest);
        return channelGet;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createMonitor(org.epics.pvData.monitor.MonitorRequester, org.epics.pvData.pv.PVStructure, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public Monitor createMonitor(
            MonitorRequester monitorRequester,
            PVStructure pvRequest)
    {
        BaseV3Monitor monitor = new BaseV3Monitor(monitorRequester);
        monitor.init(this,pvRequest);
        return monitor;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelProcess(org.epics.ca.client.ChannelProcessRequester, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelProcess createChannelProcess(
            ChannelProcessRequester channelProcessRequester,
            PVStructure pvRequest)
    {
        channelProcessRequester.channelProcessConnect(notSupportedStatus,null);
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelPut(org.epics.ca.client.ChannelPutRequester, org.epics.pvData.pv.PVStructure, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelPut createChannelPut(ChannelPutRequester channelPutRequester,
            PVStructure pvRequest)
    {
        BaseV3ChannelPut channelPut = new BaseV3ChannelPut(channelPutRequester);
        channelPut.init(this,pvRequest);
        return channelPut;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#createChannelPutGet(org.epics.ca.client.ChannelPutGetRequester, org.epics.pvData.pv.PVStructure, boolean, org.epics.pvData.pv.PVStructure, boolean, boolean, org.epics.pvData.pv.PVStructure)
     */
    @Override
    public ChannelPutGet createChannelPutGet(
            ChannelPutGetRequester channelPutGetRequester,
            PVStructure pvRequest)
    {
        channelPutGetRequester.channelPutGetConnect(notSupportedStatus, null, null, null);
        return null;
    }
    @Override
	public ChannelRPC createChannelRPC(ChannelRPCRequester channelRPCRequester,
			PVStructure pvRequest)
    {
    	channelRPCRequester.channelRPCConnect(notSupportedStatus,null);
		return null;
	}

	/* (non-Javadoc)
     * @see org.epics.ca.client.Channel#destroy()
     */
    @Override
    public void destroy() {
        synchronized(this) {
            if(isDestroyed) return;
            isDestroyed = true;
        }
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
        return channelName;
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getChannelRequester()
     */
    @Override
    public ChannelRequester getChannelRequester() {
        return channelRequester;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ca.client.Channel#getField(org.epics.ca.client.GetFieldRequester, java.lang.String)
     */
    @Override
    public void getField(GetFieldRequester requester, String subField) {
        if(subField==null || subField.equals("")) subField = "value";
        if(!subField.equals("value")) {
            requester.getDone(subFieldDoesNotExistStatus, null);
            return;
        }
        DBRType nativeDBRType = jcaChannel.getFieldType();
        boolean extraProperties = true;
        Type valueType = null;
        ScalarType valueScalarType = null;
        if(nativeDBRType==DBRType.ENUM) {
            valueType = Type.structure;
            extraProperties = false;
        } else if(nativeDBRType==DBRType.STRING) {
            valueScalarType = ScalarType.pvString;
            extraProperties = false;
        } else if(nativeDBRType==DBRType.BYTE) {
            valueScalarType = ScalarType.pvByte;
        } else if(nativeDBRType==DBRType.SHORT) {
            valueScalarType = ScalarType.pvShort;
        } else if(nativeDBRType==DBRType.INT) {
            valueScalarType = ScalarType.pvInt;
        } else if(nativeDBRType==DBRType.FLOAT) {
            valueScalarType = ScalarType.pvFloat;
        } else if(nativeDBRType==DBRType.DOUBLE) {
            valueScalarType = ScalarType.pvDouble;
        }
        if(valueType==null) {
            if(jcaChannel.getElementCount()>1) {
                valueType = Type.scalarArray;
            } else {
                valueType = Type.scalar;
            }
        }
        int numFields = extraProperties ? 5 : 3 ;
        Field[] fields = new Field[numFields];
        switch(valueType) {
        case scalar:
            fields[0] = fieldCreate.createScalar("value", valueScalarType);
            break;
        case scalarArray:
            fields[0] = fieldCreate.createScalarArray("value", valueScalarType);
            break;
        case structure:
            Field[] enumFields = new Field[2];
            enumFields[0] = fieldCreate.createScalar("index", ScalarType.pvInt);;
            enumFields[1] = fieldCreate.createScalarArray("choices",ScalarType.pvString);
            fields[0] = fieldCreate.createStructure("value", enumFields);
        }
        PVStructure pvStructure = masterPVDatabase.findStructure("org.epics.pvData.timeStamp");
        if(pvStructure==null) {
            requester.getDone(
                statusCreate.createStatus(StatusType.ERROR, "structure org.epics.pvData.timeStamp not found", null),
                null);
            return;
        }
        fields[1] = fieldCreate.createStructure("timeStamp", pvStructure.getStructure().getFields());
        pvStructure = masterPVDatabase.findStructure("org.epics.pvData.alarm");
        if(pvStructure==null) {
            requester.getDone(
                statusCreate.createStatus(StatusType.ERROR, "structure org.epics.pvData.alarm not found", null),
                null);
            return;
        }
        fields[2] = fieldCreate.createStructure("alarm", pvStructure.getStructure().getFields());
        if(extraProperties) {
            pvStructure = masterPVDatabase.findStructure("org.epics.pvData.display");
            if(pvStructure==null) {
                requester.getDone(
                    statusCreate.createStatus(StatusType.ERROR, "structure org.epics.pvData.display not found", null),
                    null);
                return;
            }
            fields[3] = fieldCreate.createStructure("display", pvStructure.getStructure().getFields());
            pvStructure = masterPVDatabase.findStructure("org.epics.pvData.control");
            if(pvStructure==null) {
                requester.getDone(
                    statusCreate.createStatus(StatusType.ERROR, "structure org.epics.pvData.control not found", null),
                    null);
                return;
            }
            fields[4] = fieldCreate.createStructure("control", pvStructure.getStructure().getFields());
        }
        requester.getDone(okStatus, fieldCreate.createStructure(null, fields));
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
     * @see org.epics.ioc.caV3.V3Channel#getExecutor()
     */
    public Executor getExecutor() {
        return executor;
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
                if(channelFindRequester!=null) {
                    channelFindRequester.channelFindResult(okStatus,this, true);
                    destroy();
                    return;
                }
                channelRequester.channelCreated(okStatus,this);
                channelRequester.channelStateChange(this, ConnectionState.CONNECTED);
            }
        } else {
            channelRequester.channelStateChange(this, ConnectionState.DISCONNECTED);
            message("connection lost", MessageType.warning);
        }
    }
}