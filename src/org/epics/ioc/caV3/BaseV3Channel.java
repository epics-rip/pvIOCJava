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

import org.epics.ioc.util.RequestResult;
import org.epics.pvData.channelAccess.AccessRights;
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
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.misc.ThreadPriority;
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
ChannelFind,org.epics.pvData.channelAccess.Channel,
V3Channel,ConnectionListener,Runnable,V3ChannelStructureRequester
{
    private static Executor executor = ExecutorFactory.create("caV3Connect", ThreadPriority.low);
    private boolean isDestroyed = true;
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
    
    private LinkedList<ChannelProcess> channelProcessList =
        new LinkedList<ChannelProcess>();
    private LinkedList<ChannelGet> channelGetList =
        new LinkedList<ChannelGet>();
    private LinkedList<ChannelPut> channelPutList =
        new LinkedList<ChannelPut>();
    private LinkedList<ChannelPutGet> channelPutGetList =
        new LinkedList<ChannelPutGet>();
    private LinkedList<ChannelMonitor> channelMonitorList = 
        new LinkedList<ChannelMonitor>();
    private LinkedList<ChannelArray> channelArrayList = 
        new LinkedList<ChannelArray>();

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
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.channelAccess.ChannelProcess)
     */
    @Override
    public boolean add(ChannelProcess channelProcess)
    {
        boolean result = false;
        synchronized(channelProcessList) {
            result = channelProcessList.add(channelProcess);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.channelAccess.ChannelGet)
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
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.channelAccess.ChannelPut)
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
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.channelAccess.ChannelPutGet)
     */
    @Override
    public boolean add(ChannelPutGet channelPutGet)
    {
        boolean result = false;
        synchronized(channelPutGetList) {
            result = channelPutGetList.add(channelPutGet);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.channelAccess.ChannelMonitor)
     */
    @Override
    public boolean add(ChannelMonitor channelMonitor)
    {
        boolean result = false;
        synchronized(channelMonitorList) {
            result = channelMonitorList.add(channelMonitor);
        }
        return result;
    }
    /* (non-Javadoc)if(success) {
            super.add(channelPutGet);
            return channelPutGet;
        }
        return null;
     * @see org.epics.ioc.caV3.V3Channel#add(org.epics.pvData.channelAccess.ChannelArray)
     */
    @Override
    public boolean add(ChannelArray channelArray)
    {
        boolean result = false;
        synchronized(channelArrayList) {
            result = channelArrayList.add(channelArray);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.channelAccess.ChannelProcess)
     */
    @Override
    public boolean remove(ChannelProcess channelProcess) {
        boolean result = false;
        synchronized(channelProcessList) {
            result = channelProcessList.remove(channelProcess);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.channelAccess.ChannelGet)
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
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.channelAccess.ChannelPut)
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
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.channelAccess.ChannelPutGet)
     */
    @Override
    public boolean remove(ChannelPutGet channelPutGet) {
        boolean result = false;
        synchronized(channelPutGetList) {
            result = channelPutGetList.remove(channelPutGet);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.channelAccess.ChannelMonitor)
     */
    @Override
    public boolean remove(ChannelMonitor channelMonitor) {
        boolean result = false;
        synchronized(channelMonitorList) {
            result = channelMonitorList.remove(channelMonitor);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#remove(org.epics.pvData.channelAccess.ChannelArray)
     */
    @Override
    public boolean remove(ChannelArray channelArray) {
        boolean result = false;
        synchronized(channelArrayList) {
            result = channelArrayList.remove(channelArray);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createChannelArray(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelArrayRequester, java.lang.String)
     */
    @Override
    public void createChannelArray(
            org.epics.pvData.channelAccess.Channel channel,
            ChannelArrayRequester channelArrayRequester, String subField)
    {
        channelArrayRequester.channelArrayConnect(null, null);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createChannelGet(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelGetRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean)
     */
    @Override
    public void createChannelGet(
            org.epics.pvData.channelAccess.Channel channel,
            ChannelGetRequester channelGetRequester, PVStructure pvRequest,
            String structureName, boolean shareData, boolean process)
    {
        if(v3ChannelStructure==null) {
            channelGetRequester.message(
                    "createChannelGet but not connected",MessageType.warning);
            channelGetRequester.channelGetConnect(null, null, null);
            return;
        }
        BaseV3ChannelGet channelGet = new BaseV3ChannelGet(channelGetRequester,process);
        channelGet.init(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createChannelMonitor(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelMonitorRequester, org.epics.pvData.pv.PVStructure, java.lang.String, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.Executor)
     */
    @Override
    public void createChannelMonitor(
            org.epics.pvData.channelAccess.Channel channel,
            ChannelMonitorRequester channelMonitorRequester,
            PVStructure pvRequest, String structureName, PVStructure pvOption,
            Executor executor)
    {
        if(v3ChannelStructure==null) {
            channelMonitorRequester.message(
                    "createChannelMonitor but not connected",MessageType.warning);
            channelMonitorRequester.channelMonitorConnect(null);
            return;
        }
        BaseV3ChannelMonitor channelMonitor = new BaseV3ChannelMonitor(channelMonitorRequester);
        channelMonitor.init(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createChannelProcess(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelProcessRequester)
     */
    @Override
    public void createChannelProcess(
            org.epics.pvData.channelAccess.Channel channel,
            ChannelProcessRequester channelProcessRequester)
    {
        channelProcessRequester.message(
                "createChannelProcess not supported. Issue a put to .PROC",MessageType.warning);
        channelProcessRequester.channelProcessConnect(null);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createChannelPut(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelPutRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean)
     */
    @Override
    public void createChannelPut(
            org.epics.pvData.channelAccess.Channel channel,
            ChannelPutRequester channelPutRequester, PVStructure pvRequest,
            String structureName, boolean shareData, boolean process)
    {
        if(v3ChannelStructure==null) {
            channelPutRequester.message(
                    "createChannelPut but not connected",MessageType.warning);
            channelPutRequester.channelPutConnect(null, null, null);
            return;
        }
        BaseV3ChannelPut channelPut = new BaseV3ChannelPut(channelPutRequester,process);
        channelPut.init(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createChannelPutGet(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.ChannelPutGetRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, org.epics.pvData.pv.PVStructure, java.lang.String, boolean, boolean)
     */
    @Override
    public void createChannelPutGet(
            org.epics.pvData.channelAccess.Channel channel,
            ChannelPutGetRequester channelPutGetRequester,
            PVStructure pvPutRequest, String putStructureName,
            boolean sharePutData, PVStructure pvGetRequest,
            String getStructureName, boolean shareGetData, boolean process)
    {
        channelPutGetRequester.channelPutGetConnect(null, null, null, null, null);
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#createPVStructure(org.epics.pvData.channelAccess.Channel, org.epics.pvData.channelAccess.CreatePVStructureRequester, org.epics.pvData.pv.PVStructure, java.lang.String, boolean)
     */
    @Override
    public void createPVStructure(
            org.epics.pvData.channelAccess.Channel channel,
            CreatePVStructureRequester requester, PVStructure pvRequest,
            String structureName, boolean shareData)
    {
        if(v3ChannelStructure==null) {
            requester.message(
                    "createPVStructure but not connected",MessageType.warning);
            requester.createDone(null);
            return;
        }
        requester.createDone(v3ChannelStructure.getPVStructure());
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#destroy()
     */
    @Override
    public void destroy() {
        jcaChannel.dispose();
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
     * @see org.epics.pvData.channelAccess.Channel#getChannelName()
     */
    @Override
    public String getChannelName() {
        return pvName;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#getChannelRequester()
     */
    @Override
    public ChannelRequester getChannelRequester() {
        return channelRequester;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#getField(org.epics.pvData.channelAccess.GetFieldRequester, java.lang.String)
     */
    @Override
    public void getField(GetFieldRequester requester, String subField) {
        if(v3ChannelStructure==null) {
            requester.message(
                    "not connected",MessageType.warning);
            requester.getDone(null);
        }
        PVStructure pvStructure = v3ChannelStructure.getPVStructure();
        PVField pvField = pvStructure.getSubField(subField);
        requester.getDone(pvField.getField());
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#getProviderName()
     */
    @Override
    public String getProviderName() {
        return jcaChannel.getHostName();
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.channelAccess.Channel#isConnected()
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
     * @see org.epics.pvData.channelAccess.Channel#connect()
     */
    @Override
    public void connect() {
        channelRequester.channelStateChange(this, true);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.AbstractChannel#disconnect()
     */
    public void disconnect() {
        if(isDestroyed) return;
        this.disconnect();
        isDestroyed = true;
        while(!channelProcessList.isEmpty()) {
            ChannelProcess channelProcess = channelProcessList.getFirst();
            remove(channelProcess);
            channelProcess.destroy();
        }
        while(!channelGetList.isEmpty()) {
            ChannelGet channelGet = channelGetList.getFirst();
            remove(channelGet);
            channelGet.destroy();
        }
        while(!channelPutList.isEmpty()) {
            ChannelPut channelPut = channelPutList.getFirst();
            remove(channelPut);
            channelPut.destroy();
        }
        while(!channelPutGetList.isEmpty()) {
            ChannelPutGet channelPutGet = channelPutGetList.getFirst();
            remove(channelPutGet);
            channelPutGet.destroy();
        }
        while(!channelMonitorList.isEmpty()) {
            ChannelMonitor channelMonitor = channelMonitorList.getFirst();
            remove(channelMonitor);
            channelMonitor.destroy();
        }
        while(!channelArrayList.isEmpty()) {
            ChannelArray channelArray = channelArrayList.getFirst();
            remove(channelArray);
            channelArray.destroy();
        }
        jcaChannel.dispose();
        jcaChannel = null;
        v3ChannelStructure = null;
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