/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Base class that implements ChannelGet for communicating with a V3 IOC.
 * @author mrk
 *
 */


public class BaseV3ChannelGet implements ChannelProcessRequester,ChannelGet,GetListener
{
    private static enum DBRProperty {none,status,time,graphic,control};
    
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelGetRequester channelGetRequester = null;
    private boolean process;
    
    private gov.aps.jca.Channel jcaChannel = null;
    
    private V3Channel v3Channel = null;
    private DBRType requestDBRType = null;
    private int elementCount = 0;
    
    private boolean isDestroyed = false;
    
    private ChannelProcess channelProcess = null;
    private DBRProperty dbrProperty = DBRProperty.none;
    /**
     * Constructer.
     * @param channelFieldGroup The channelFieldGroup.
     * @param channelGetRequester The channelGetRequester.
     * @param process Should the record be processed before get.
     */
    public BaseV3ChannelGet(ChannelFieldGroup channelFieldGroup,
            ChannelGetRequester channelGetRequester,boolean process)
    {
        this.channelFieldGroup = channelFieldGroup;
        this.channelGetRequester = channelGetRequester;
        this.process = process;
    }
    /**
     * Initialize the channelGet.
     * @param v3Channel The V3Channel
     * @return (false,true) if the channelGet (did not, did) properly initialize.
     */
    public boolean init(V3Channel v3Channel)
    {
        this.v3Channel = v3Channel;
        jcaChannel = v3Channel.getJCAChannel();
        DBRType valueDBRType = jcaChannel.getFieldType();
        requestDBRType = null;
        ChannelField[] channelFields = channelFieldGroup.getArray();
        elementCount = jcaChannel.getElementCount();
        dbrProperty = DBRProperty.none;
        if(process) {
            channelProcess = v3Channel.createChannelProcess(this);
            if(channelProcess==null) return false;
        }
        for(ChannelField channelField : channelFields) {
            String fieldName = channelField.getPVField().getField().getFieldName();
            if(fieldName.equals("alarm")&& (dbrProperty.compareTo(DBRProperty.status)<0)) {
                dbrProperty = DBRProperty.status;
                continue;
            }
            if(fieldName.equals("timeStamp")&& (dbrProperty.compareTo(DBRProperty.time)<0)) {
                dbrProperty = DBRProperty.time;
                continue;
            }
            if(fieldName.equals("display")&& (dbrProperty.compareTo(DBRProperty.graphic)<0)) {
                dbrProperty = DBRProperty.graphic;
                continue;
            }
            if(fieldName.equals("control")&& (dbrProperty.compareTo(DBRProperty.control)<0)) {
                dbrProperty = DBRProperty.control;
                continue;
            }
        }
        switch(dbrProperty) {
        case none:
            if(valueDBRType.isENUM()) {
                requestDBRType = DBRType.INT;
            } else {
                requestDBRType = valueDBRType;
            }
            break;
        case status:
            if(valueDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.STS_BYTE;
            } else if(valueDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.STS_SHORT;
            } else if(valueDBRType==DBRType.INT) {
                requestDBRType = DBRType.STS_INT;
            } else if(valueDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.STS_FLOAT;
            } else if(valueDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.STS_DOUBLE;
            } else if(valueDBRType==DBRType.STRING) {
                requestDBRType = DBRType.STS_STRING;
            } else if(valueDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.STS_INT;
            }
            break;
        case time:
            if(valueDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.TIME_BYTE;
            } else if(valueDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.TIME_SHORT;
            } else if(valueDBRType==DBRType.INT) {
                requestDBRType = DBRType.TIME_INT;
            } else if(valueDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.TIME_FLOAT;
            } else if(valueDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.TIME_DOUBLE;
            } else if(valueDBRType==DBRType.STRING) {
                requestDBRType = DBRType.TIME_STRING;
            } else if(valueDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.TIME_INT;
            }
            break;
        case graphic:
            if(valueDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.GR_BYTE;
            } else if(valueDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.GR_SHORT;
            } else if(valueDBRType==DBRType.INT) {
                requestDBRType = DBRType.GR_INT;
            } else if(valueDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.GR_FLOAT;
            } else if(valueDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.GR_DOUBLE;
            } else if(valueDBRType==DBRType.STRING) {
                requestDBRType = DBRType.GR_STRING;
            } else if(valueDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.CTRL_ENUM;
            }
            break;
        case control:
            if(valueDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.CTRL_BYTE;
            } else if(valueDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.CTRL_SHORT;
            } else if(valueDBRType==DBRType.INT) {
                requestDBRType = DBRType.CTRL_INT;
            } else if(valueDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.CTRL_FLOAT;
            } else if(valueDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.CTRL_DOUBLE;
            } else if(valueDBRType==DBRType.STRING) {
                requestDBRType = DBRType.CTRL_STRING;
            } else if(valueDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.CTRL_ENUM;
            }
            break;
        }
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
     */
    public void processDone(RequestResult requestResult) {
        try {
            jcaChannel.get(requestDBRType, elementCount, this);
        } catch (CAException e) {
            message(e.getMessage(),MessageType.error);
            channelGetRequester.getDone(RequestResult.failure);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return v3Channel.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        v3Channel.message(message, messageType);   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGet#destroy()
     */
    public void destroy() {
        isDestroyed = true;
        if(channelProcess!=null) channelProcess.destroy();
        v3Channel.remove(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGet#get()
     */
    public void get() {
        if(isDestroyed) {
            message("isDestroyed",MessageType.error);
            channelGetRequester.getDone(RequestResult.failure);
        }
        if(process) {
            channelProcess.process();
        }
        try {
            jcaChannel.get(requestDBRType, elementCount, this);
        } catch (CAException e) {
            message(e.getMessage(),MessageType.error);
            channelGetRequester.getDone(RequestResult.failure);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGet#getDelayed(org.epics.ioc.pv.PVField)
     */
    public void getDelayed(PVField pvField) {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
     */
    public void getCompleted(GetEvent getEvent) {
        DBR fromDBR = getEvent.getDBR();
        if(fromDBR==null) {
            CAStatus caStatus = getEvent.getStatus();
            if(caStatus==null) {
                channelGetRequester.message(getEvent.toString(),MessageType.error);
            } else {
                channelGetRequester.message(caStatus.getMessage(),MessageType.error);
                channelGetRequester.getDone(RequestResult.failure);
            }
            return;
        }
        v3Channel.getV3ChannelRecord().toRecord(fromDBR,null);
        ChannelField[] channelFields = channelFieldGroup.getArray();
        for(ChannelField channelField : channelFields) {
            channelGetRequester.nextGetField(channelField, channelField.getPVField());
        }
        channelGetRequester.getDone(RequestResult.success);
    }
}
