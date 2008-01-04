/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.Iterator;
import java.util.LinkedList;


import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * @author mrk
 *
 */
public abstract class AbstractChannel implements Channel,Requester{
    protected boolean isDestroyed = false;
    protected ChannelListener channelListener = null;
    protected PVRecord pvRecord;
    protected String channelName;
    protected String fieldName;
    protected String options;
    
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
    
    protected AbstractChannel(ChannelListener channelListener,
        String fieldName, String options)
    {
        this.channelListener = channelListener;
        this.fieldName = fieldName;
        this.options = options;
        
    }
    
    protected synchronized void SetPVRecord(PVRecord pvRecord) {
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
    protected synchronized boolean add(ChannelProcess channelProcess)
    {
        return channelProcessList.add(channelProcess);
    }
    /**
     * Add a channelGet
     * @param channelGet The channelGet to add.
     * @return (false,true) if the channelGet (was not, was) added.
     */
    protected synchronized boolean add(ChannelGet channelGet)
    {
        return channelGetList.add(channelGet);
    }
    /**
     * Add a channelPut
     * @param channelPut The channelPut to add.
     * @return (false,true) if the channelPut (was not, was) added.
     */
    protected synchronized boolean add(ChannelPut channelPut)
    {
        return channelPutList.add(channelPut);
    }
    /**
     * Add a channelPutGet
     * @param channelPutGet The channelPutGet to add.
     * @return (false,true) if the channelPutGet (was not, was) added.
     */
    protected synchronized boolean add(ChannelPutGet channelPutGet)
    {
        return channelPutGetList.add(channelPutGet);
    }
    /**
     * Add a channelMonitor
     * @param channelMonitor The channelMonitor to add.
     * @return (false,true) if the channelMonitor (was not, was) added.
     */
    protected synchronized boolean add(ChannelMonitor channelMonitor)
    {
        return channelMonitorList.add(channelMonitor);
    }
    
    /**
     * Remove a ChannelProcess 
     * @param channelProcess The channelProcess to remove.
     * @return (false,true) if the channelProcess (was not, was) removed.
     */
    protected synchronized boolean remove(ChannelProcess channelProcess) {
        return channelProcessList.remove(channelProcess);
    }
    /**
     * Remove a ChannelGet 
     * @param channelGet The channelGet to remove.
     * @return (false,true) if the channelGet (was not, was) removed.
     */
    protected synchronized boolean remove(ChannelGet channelGet) {
        return channelGetList.remove(channelGet);
    }
    /**
     * Remove a ChannelPut 
     * @param channelPut The channelPut to remove.
     * @return (false,true) if the channelPut (was not, was) removed.
     */
    protected synchronized boolean remove(ChannelPut channelPut) {
        return channelPutList.remove(channelPut);
    }
    /**
     * Remove a ChannelPutGet 
     * @param channelPutGet The channelPutGet to remove.
     * @return (false,true) if the channelPutGet (was not, was) removed.
     */
    protected synchronized boolean remove(ChannelPutGet channelPutGet) {
        return channelPutGetList.remove(channelPutGet);
    }
    /**
     * Remove a ChannelMonitor 
     * @param channelMonitor The channelMonitor to remove.
     * @return (false,true) if the channelMonitor (was not, was) removed.
     */
    protected synchronized boolean remove(ChannelMonitor channelMonitor) {
        return channelMonitorList.remove(channelMonitor);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getChannelName()
     */
    public synchronized String getChannelName() {
        return channelName;
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
     * @see org.epics.ioc.ca.Channel#getChannelStateListener()
     */
    public ChannelListener getChannelListener() {
        return channelListener;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#destroy()
     */
    public void destroy() {
        ChannelListener channelListener = destroyPvt();
        if(channelListener!=null) channelListener.disconnect(this);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getFieldName()
     */
    public synchronized String getFieldName() {
        return fieldName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getOptions()
     */
    public synchronized String getOptions() {
        return options;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.Channel#getPropertyName()
     */
    public synchronized String getPropertyName() {
        if(fieldName==null||fieldName.length()<=0) return "value";
        PVField pvField = pvRecord.findProperty(fieldName);
        if(pvField!=null && pvField.getField().getType()==Type.pvStructure) {
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
        if(isDestroyed) return null;
        return new BaseChannelFieldGroup(this,listener);
    }
    
    private synchronized ChannelListener destroyPvt() {
        if(isDestroyed) return null;
        int number = channelProcessList.size();
        while(number>0) {
            channelProcessList.getLast().destroy();
            --number;
        }
        if(number!=0) {
            message("logic error destroying channelProcessList",MessageType.error);
        }
        number = channelGetList.size();
        while(number>0) {
            channelGetList.getLast().destroy();
            --number;
        }
        if(number!=0) {
            message("logic error destroying channelGetList",MessageType.error);
        }
        number = channelPutList.size();
        while(number>0) {
            channelPutList.getLast().destroy();
            --number;
        }
        if(number!=0) {
            message("logic error destroying channelPutList",MessageType.error);
        }
        number = channelPutGetList.size();
        while(number>0) {
            channelPutGetList.getLast().destroy();
            --number;
        }
        if(number!=0) {
            message("logic error destroying channelPutGetList",MessageType.error);
        }
        number = channelMonitorList.size();
        while(number>0) {
            channelMonitorList.getLast().destroy();
            --number;
        }
        if(number!=0) {
            message("logic error destroying channelMonitorList",MessageType.error);
        }
        isDestroyed = true;
        return channelListener;
    }  
}
