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

import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelMonitorRequester;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVStructure;


/**
 * Base class that implements ChannelMonitor for communicating with a V3 IOC.
 * @author mrk
 *
 */
public class BaseV3ChannelMonitor implements ChannelMonitor,MonitorListener,ConnectionListener
{
    private static enum DBRProperty {none,status,time};

    private ChannelMonitorRequester channelMonitorRequester;
    
    private V3Channel v3Channel = null;
    private gov.aps.jca.Channel jcaChannel = null;
    private int elementCount = 0;
    
    private String[] propertyNames = null;
    private DBRProperty dbrProperty = DBRProperty.none;
    private DBRType requestDBRType = null;

    private Monitor monitor = null;
    private boolean isDestroyed = false;
    
    private  PVStructure pvStructure = null;
    private BitSet changeBitSet = null;
    private BitSet overrunBitSet = null;
    /**
     * Constructor.
     * @param channelMonitorRequester The channelMonitorRequester.
     */
    public BaseV3ChannelMonitor(ChannelMonitorRequester channelMonitorRequester) {
        this.channelMonitorRequester = channelMonitorRequester;
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
        propertyNames = v3Channel.getPropertyNames();
        jcaChannel = v3Channel.getJCAChannel();
        try {
            jcaChannel.addConnectionListener(this);
        } catch (CAException e) {
            
            channelMonitorRequester.message(
                    "addConnectionListener failed " + e.getMessage(),
                    MessageType.error);
            channelMonitorRequester.channelMonitorConnect(null);
            
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
        channelMonitorRequester.channelMonitorConnect(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#destroy()
     */
    public void destroy() {
        isDestroyed = true;
        if(monitor!=null) stop();
        v3Channel.remove(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#start()
     */
    public void start() {
        if(isDestroyed) {
            channelMonitorRequester.message("isDestroyed", MessageType.warning);
        }
        try {
            monitor = jcaChannel.addMonitor(requestDBRType, elementCount, 0x0ff, this);
        } catch (CAException e) {
            channelMonitorRequester.message(e.getMessage(),MessageType.error);
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
            channelMonitorRequester.message(e.getMessage(),MessageType.error);
        }
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
     */
    public void monitorChanged(MonitorEvent monitorEvent) {
        CAStatus caStatus = monitorEvent.getStatus();
        if(!caStatus.isSuccessful()) {
            channelMonitorRequester.message(caStatus.getMessage(),MessageType.error);
            return;
        }
        DBR fromDBR = monitorEvent.getDBR();
        if(fromDBR==null) {
            channelMonitorRequester.message("fromDBR is null", MessageType.error);
        } else {
            v3Channel.getV3ChannelStructure().toStructure(fromDBR);
            channelMonitorRequester.monitorEvent(pvStructure, changeBitSet, overrunBitSet);
        }
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        if(!arg0.isConnected()) {
            if(monitor!=null) stop();
        }
    }
}