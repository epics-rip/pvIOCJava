/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;


import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import org.epics.pvData.factory.StatusFactory;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.monitor.MonitorElement;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;
import org.epics.pvData.pv.StatusCreate;
import org.epics.pvData.pv.Status.StatusType;


/**
 * Base class that implements ChannelMonitor for communicating with a V3 IOC.
 * @author mrk
 *
 */
public class BaseV3Monitor implements org.epics.pvData.monitor.Monitor,MonitorListener,ConnectionListener
{
    private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
   
    private static enum DBRProperty {none,status,time};

    private MonitorRequester monitorRequester;
    
    private V3Channel v3Channel = null;
    private gov.aps.jca.Channel jcaChannel = null;
    private int elementCount = 0;
    
    private String[] propertyNames = null;
    private DBRProperty dbrProperty = DBRProperty.none;
    private DBRType requestDBRType = null;

    private Monitor monitor = null;
    private boolean isDestroyed = false;
    
    private PVStructure pvStructure = null;
    private BitSet changeBitSet = null;
    private BitSet overrunBitSet = null;
    private MonitorElement monitorElement = null;
    /**
     * Constructor.
     * @param monitorRequester The monitorRequester.
     */
    public BaseV3Monitor(MonitorRequester monitorRequester) {
        this.monitorRequester = monitorRequester;
    }
    /**
     * Initialize the channelMonitor.
     * @param v3Channel The V3Channel
     * @return (false,true) if the channelMonitor (did not, did) properly initialize.
     */
    /**
     * @param v3Channel
     * @return
     */
    public void init(V3Channel v3Channel)
    {
        this.v3Channel = v3Channel;
        v3Channel.add(this);
        propertyNames = v3Channel.getPropertyNames();
        jcaChannel = v3Channel.getJCAChannel();
        try {
            jcaChannel.addConnectionListener(this);
        } catch (CAException e) {
            monitorRequester.monitorConnect(statusCreate.createStatus(StatusType.ERROR, "addConnectionListener failed", e), null,null);
            jcaChannel = null;
            return;
        };
        DBRType nativeDBRType = v3Channel.getV3ChannelStructure().getNativeDBRType();
        requestDBRType = null;
        elementCount = jcaChannel.getElementCount();
        dbrProperty = DBRProperty.none;
        for(String property : propertyNames) {
            if(property.equals("alarm")&& (dbrProperty.compareTo(DBRProperty.status)<0)) {
                dbrProperty = DBRProperty.status;
                continue;
            }
            if(property.equals("timeStamp")&& (dbrProperty.compareTo(DBRProperty.time)<0)) {
                dbrProperty = DBRProperty.time;
                continue;
            }
        }
        switch(dbrProperty) {
        case none:
            if(nativeDBRType.isENUM()) {
                requestDBRType = DBRType.INT;
            } else {
                requestDBRType = nativeDBRType;
            }
            break;
        case status:
            if(nativeDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.STS_BYTE;
            } else if(nativeDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.STS_SHORT;
            } else if(nativeDBRType==DBRType.INT) {
                requestDBRType = DBRType.STS_INT;
            } else if(nativeDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.STS_FLOAT;
            } else if(nativeDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.STS_DOUBLE;
            } else if(nativeDBRType==DBRType.STRING) {
                requestDBRType = DBRType.STS_STRING;
            } else if(nativeDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.STS_INT;
            }
            break;
        case time:
            if(nativeDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.TIME_BYTE;
            } else if(nativeDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.TIME_SHORT;
            } else if(nativeDBRType==DBRType.INT) {
                requestDBRType = DBRType.TIME_INT;
            } else if(nativeDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.TIME_FLOAT;
            } else if(nativeDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.TIME_DOUBLE;
            } else if(nativeDBRType==DBRType.STRING) {
                requestDBRType = DBRType.TIME_STRING;
            } else if(nativeDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.TIME_INT;
            }
            break;
        }
        V3ChannelStructure v3ChannelStructure = v3Channel.getV3ChannelStructure();
        pvStructure = v3ChannelStructure.getPVStructure();
        changeBitSet = v3ChannelStructure.getBitSet();
        overrunBitSet = new BitSet(pvStructure.getNumberFields());
        monitorElement = new MonitorElementImpl(pvStructure,changeBitSet,overrunBitSet);
        monitorRequester.monitorConnect(okStatus,this,pvStructure.getStructure());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#destroy()
     */
    public void destroy() {
        if(monitor!=null) stop();
        isDestroyed = true;
        v3Channel.remove(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#start()
     */
    public void start() {
        if(isDestroyed) {
        	// TODO report
            monitorRequester.message("isDestroyed", MessageType.warning);
        }
        try {
            monitor = jcaChannel.addMonitor(requestDBRType, elementCount, 0x0ff, this);
        } catch (CAException e) {
        	// TODO report
            monitorRequester.message("start caused CAExecption " + e.getMessage(),MessageType.error);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#stop()
     */
    public void stop() {
        if(isDestroyed) return;
        try {
            monitor.clear();
        } catch (CAException e) {
        	// TODO report
            monitorRequester.message("stop caused CAExecption " + e.getMessage(),MessageType.error);
        }
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
     */
    public void monitorChanged(MonitorEvent monitorEvent) {
        CAStatus caStatus = monitorEvent.getStatus();
        if(!caStatus.isSuccessful()) {
            monitorRequester.message(caStatus.getMessage(),MessageType.error);
            return;
        }
        DBR fromDBR = monitorEvent.getDBR();
        if(fromDBR==null) {
            monitorRequester.message("fromDBR is null", MessageType.error);
        } else {
            v3Channel.getV3ChannelStructure().toStructure(fromDBR);
            monitorRequester.monitorEvent(this);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.Monitor#poll()
     */
    @Override
    public MonitorElement poll() {
        if(changeBitSet.nextSetBit(0)<0) return null;
        return monitorElement;
    }
    /* (non-Javadoc)
     * @see org.epics.pvData.monitor.Monitor#release(org.epics.pvData.monitor.MonitorElement)
     */
    @Override
    public void release(MonitorElement monitorElement) {
        changeBitSet.clear();
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        if(!arg0.isConnected()) {
            if(monitor!=null) stop();
        }
    }
    
    private static class MonitorElementImpl implements MonitorElement {
        private PVStructure pvStructure;
        private BitSet changedBitSet;
        private BitSet overrunBitSet;
        

        MonitorElementImpl(PVStructure pvStructure,BitSet changedBitSet, BitSet overrunBitSet) {
            super();
            this.pvStructure = pvStructure;
            this.changedBitSet = changedBitSet;
            this.overrunBitSet = overrunBitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.monitor.MonitorElement#getChangedBitSet()
         */
        @Override
        public BitSet getChangedBitSet() {
            return changedBitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.monitor.MonitorElement#getOverrunBitSet()
         */
        @Override
        public BitSet getOverrunBitSet() {
            return overrunBitSet;
        }
        /* (non-Javadoc)
         * @see org.epics.pvData.monitor.MonitorElement#getPVStructure()
         */
        @Override
        public PVStructure getPVStructure() {
            return pvStructure;
        }
        
    }
}