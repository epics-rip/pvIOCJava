/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.LinkedList;

import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Abstract class for code that implements Channel.
 * @author mrk
 *
 */
public abstract class AbstractChannel implements Channel{
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty();
    private boolean isDestroyed = false;
    private boolean isConnected = false;
    private ChannelListener channelListener;
    private String fieldName;
    private String options;
    
    private PVRecord pvRecord = null;
    private String channelName = null;
    
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
    
    /**
     * Constructor.
     * @param channelListener The channelListener.
     * @param options The options.
     */
    protected AbstractChannel(ChannelListener channelListener,String options)
    {
        this.channelListener = channelListener;
        
        this.options = options;
        
    }
    /**
     * Set the PVRecord for this channel.
     * @param pvRecord The pvRecord interface.
     * @param fieldName The fieldName.
     */
    protected synchronized void setPVRecord(PVRecord pvRecord,String fieldName) {
        this.fieldName = fieldName;
        this.pvRecord = pvRecord;
        if(fieldName==null) {
            channelName = pvRecord.getRecordName();
        } else {
            channelName =  pvRecord.getRecordName() + "." + fieldName;
        }
    }
    /**
     * Add a channelProcess
     * @param channelProcess The channelProcess to add.
     * @return (false,true) if the channelProcess (was not, was) added.
     */
    public synchronized boolean add(ChannelProcess channelProcess)
    {
        return channelProcessList.add(channelProcess);
    }
    /**
     * Add a channelGet
     * @param channelGet The channelGet to add.
     * @return (false,true) if the channelGet (was not, was) added.
     */
    public synchronized boolean add(ChannelGet channelGet)
    {
        return channelGetList.add(channelGet);
    }
    /**
     * Add a channelPut
     * @param channelPut The channelPut to add.
     * @return (false,true) if the channelPut (was not, was) added.
     */
    public synchronized boolean add(ChannelPut channelPut)
    {
        return channelPutList.add(channelPut);
    }
    /**
     * Add a channelPutGet
     * @param channelPutGet The channelPutGet to add.
     * @return (false,true) if the channelPutGet (was not, was) added.
     */
    public synchronized boolean add(ChannelPutGet channelPutGet)
    {
        return channelPutGetList.add(channelPutGet);
    }
    /**
     * Add a channelMonitor
     * @param channelMonitor The channelMonitor to add.
     * @return (false,true) if the channelMonitor (was not, was) added.
     */
    public synchronized boolean add(ChannelMonitor channelMonitor)
    {
        return channelMonitorList.add(channelMonitor);
    }
    
    /**
     * Remove a ChannelProcess 
     * @param channelProcess The channelProcess to remove.
     * @return (false,true) if the channelProcess (was not, was) removed.
     */
    public synchronized boolean remove(ChannelProcess channelProcess) {
        return channelProcessList.remove(channelProcess);
    }
    /**
     * Remove a ChannelGet 
     * @param channelGet The channelGet to remove.
     * @return (false,true) if the channelGet (was not, was) removed.
     */
    public synchronized boolean remove(ChannelGet channelGet) {
        return channelGetList.remove(channelGet);
    }
    /**
     * Remove a ChannelPut 
     * @param channelPut The channelPut to remove.
     * @return (false,true) if the channelPut (was not, was) removed.
     */
    public synchronized boolean remove(ChannelPut channelPut) {
        return channelPutList.remove(channelPut);
    }
    /**
     * Remove a ChannelPutGet 
     * @param channelPutGet The channelPutGet to remove.
     * @return (false,true) if the channelPutGet (was not, was) removed.
     */
    public synchronized boolean remove(ChannelPutGet channelPutGet) {
        return channelPutGetList.remove(channelPutGet);
    }
    /**
     * Remove a ChannelMonitor 
     * @param channelMonitor The channelMonitor to remove.
     * @return (false,true) if the channelMonitor (was not, was) removed.
     */
    public synchronized boolean remove(ChannelMonitor channelMonitor) {
        return channelMonitorList.remove(channelMonitor);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getChannelName()
     */
    public synchronized String getChannelName() {
        return channelName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getPVRecord()
     */
    public synchronized PVRecord getPVRecord() {
        return pvRecord;
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
        channelListener.message(message, messageType);   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#connect()
     */
    public void connect() {
        if(isDestroyed) {
            message("connect request but is destroyed",MessageType.warning);
            return;
        }
        isConnected = true;
        channelListener.channelStateChange(this, true);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#disconnect()
     */
    public void disconnect() {
        if(isDestroyed) return;
        isConnected = false;
        channelListener.channelStateChange(this, false);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#isConnected()
     */
    public synchronized boolean isConnected() {
        return isConnected;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#destroy()
     */
    public void destroy() {
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
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getChannelStateListener()
     */
    public ChannelListener getChannelListener() {
        return channelListener;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getFieldName()
     */
    public String getFieldName() {
        return fieldName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getOptions()
     */
    public String getOptions() {
        return options;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getPrimaryFieldName()
     */
    public synchronized String getPrimaryFieldName() {
        if(fieldName==null||fieldName.length()<=0) return "value";
        PVField pvField = pvProperty.findProperty(pvRecord,fieldName);
        if(pvField!=null && pvField.getField().getType()==Type.structure) {
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
    public synchronized ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener) {
        if(!isConnected) {
            message("createFieldGroup called while not connected",MessageType.warning);
            return null;
        }
        return new BaseChannelFieldGroup(this,listener);
    }
}
