/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;



/**
 * Base class that implements ChannelGet for communicating with a V3 IOC.
 * @author mrk
 *
 */


public class BaseV3ChannelGet
implements ChannelGet,ChannelProcessRequester,GetListener,ConnectionListener
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
    
    private boolean isActive = false;
    /**
     * Constructor.
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
        try {
            jcaChannel.addConnectionListener(this);
        } catch (CAException e) {
            message(
                    "addConnectionListener failed " + e.getMessage(),
                    MessageType.error);
            jcaChannel = null;
            return false;
        };
        DBRType nativeDBRType = v3Channel.getV3ChannelRecord().getNativeDBRType();
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
        case graphic:
            if(nativeDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.GR_BYTE;
            } else if(nativeDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.GR_SHORT;
            } else if(nativeDBRType==DBRType.INT) {
                requestDBRType = DBRType.GR_INT;
            } else if(nativeDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.GR_FLOAT;
            } else if(nativeDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.GR_DOUBLE;
            } else if(nativeDBRType==DBRType.STRING) {
                requestDBRType = DBRType.GR_STRING;
            } else if(nativeDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.CTRL_ENUM;
            }
            break;
        case control:
            if(nativeDBRType==DBRType.BYTE) {
                requestDBRType = DBRType.CTRL_BYTE;
            } else if(nativeDBRType==DBRType.SHORT) {
                requestDBRType = DBRType.CTRL_SHORT;
            } else if(nativeDBRType==DBRType.INT) {
                requestDBRType = DBRType.CTRL_INT;
            } else if(nativeDBRType==DBRType.FLOAT) {
                requestDBRType = DBRType.CTRL_FLOAT;
            } else if(nativeDBRType==DBRType.DOUBLE) {
                requestDBRType = DBRType.CTRL_DOUBLE;
            } else if(nativeDBRType==DBRType.STRING) {
                requestDBRType = DBRType.CTRL_STRING;
            } else if(nativeDBRType==DBRType.ENUM) {
                requestDBRType = DBRType.CTRL_ENUM;
            }
            break;
        }
        return true;
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
            getDone(RequestResult.failure);
            return;
        }
        if(jcaChannel.getConnectionState()!=Channel.ConnectionState.CONNECTED) {
            getDone(RequestResult.failure);
            return;
        }
        isActive = true;
        if(process) {
            channelProcess.process();
            return;
        }
        // just call processDone directly
        processDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGet#getDelayed(org.epics.ioc.pv.PVField)
     */
    public void getDelayed(PVField pvField) {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
     */
    public void processDone(RequestResult requestResult) {
        String message = null;
        if(requestResult!=RequestResult.success) {
            message = "process returned " + requestResult.name();
        } else {
            try {
                jcaChannel.get(requestDBRType, elementCount, this);
            } catch (CAException e) {
                message = e.getMessage();
            } catch (IllegalStateException e) {
                message = e.getMessage();
            }
        }
        if(message!=null) {
            message(message,MessageType.error);
            getDone(RequestResult.failure);
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
     * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
     */
    public void getCompleted(GetEvent getEvent) {
        DBR fromDBR = getEvent.getDBR();
        if(fromDBR==null) {
            CAStatus caStatus = getEvent.getStatus();
            if(caStatus==null) {
                message(getEvent.toString(),MessageType.error);
            } else {
                message(caStatus.getMessage(),MessageType.error);
                getDone(RequestResult.failure);
            }
            return;
        }
        v3Channel.getV3ChannelRecord().toRecord(fromDBR,null);
        ChannelField[] channelFields = channelFieldGroup.getArray();
        for(ChannelField channelField : channelFields) {
            channelGetRequester.nextGetField(channelField, channelField.getPVField());
        }
        getDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
     */
    public void connectionChanged(ConnectionEvent arg0) {
        if(!arg0.isConnected()) {
            if(isActive) {
                message("disconnected while active",MessageType.error);
                getDone(RequestResult.failure);
            }
        }
    }
    
    private void getDone(RequestResult requestResult) {
        isActive = false;
        channelGetRequester.getDone(requestResult);
    }
}
