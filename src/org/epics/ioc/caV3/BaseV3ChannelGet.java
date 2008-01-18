/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;

import java.util.Iterator;
import java.util.List;

import org.epics.ioc.ca.*;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.PVByteArray;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVDoubleArray;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVFloat;
import org.epics.ioc.pv.PVFloatArray;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVIntArray;
import org.epics.ioc.pv.PVLong;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.PVShortArray;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
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
    
    private V3Channel channel = null;
    private PVRecord pvRecord = null;
    private String valueFieldName = null;
    private int elementCount = 0;
    
    
    private boolean isDestroyed = false;
    private DBRType requestDBRType = null;
    
    private PVInt pvIndex = null;
    private DBField dbIndex = null;
    
    private List<ChannelField> channelFieldList;
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
        if(channelFieldGroup==null) {
            throw new IllegalStateException("no field group");
        }

        this.channelFieldGroup = channelFieldGroup;
        this.channelGetRequester = channelGetRequester;
        this.process = process;
    }
    /**
     * Initialize the channelGet.
     * @param channel The V3Channel
     * @return (false,true) if the channelGet (did not, did) properly initialize.
     */
    public boolean init(V3Channel channel)
    {
        this.channel = channel;
        jcaChannel = channel.getJcaChannel();
        DBRecord dbRecord = channel.getDBRecord();
        pvRecord = dbRecord.getPVRecord();
        DBRType valueDBRType = channel.getValueDBRType();
        String[] propertyNames = channel.getPropertyNames();
        elementCount = jcaChannel.getElementCount();
        valueFieldName = channel.getValueFieldName();
        channelFieldList = channelFieldGroup.getList();
        PVField valuePVField = pvRecord.getPVFields()[0];
        if(valueDBRType.isENUM()) {
            if(process) {
                channelGetRequester.message(
                    "process not supported for enumerated", MessageType.error);
                return false;
            }
            if(elementCount!=1) {
                channelGetRequester.message(
                        "array of enumerated not supported", MessageType.error);
                    return false;
            }
            PVEnumerated pvEnumerated = valuePVField.getPVEnumerated();
            pvIndex = pvEnumerated.getIndexField();
            dbIndex = dbRecord.findDBField(pvIndex);
            requestDBRType = DBRType.SHORT;
            return true;
        }
        if(process) {
            channelProcess = channel.createChannelProcess(this);
            if(channelProcess==null) return false;
        }
        
        if(propertyNames.length>0) {
            for(String propertyName : propertyNames) {
                if(propertyName.equals("alarm")&& (dbrProperty.compareTo(DBRProperty.status)<0)) {
                    dbrProperty = DBRProperty.status;
                    continue;
                }
                if(propertyName.equals("timeStamp")&& (dbrProperty.compareTo(DBRProperty.time)<0)) {
                    dbrProperty = DBRProperty.time;
                    continue;
                }
                if(propertyName.equals("display")&& (dbrProperty.compareTo(DBRProperty.graphic)<0)) {
                    dbrProperty = DBRProperty.graphic;
                    continue;
                }
                if(propertyName.equals("control")&& (dbrProperty.compareTo(DBRProperty.control)<0)) {
                    dbrProperty = DBRProperty.control;
                    continue;
                }
            }
        }
        switch(dbrProperty) {
        case none:
            requestDBRType = valueDBRType;
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
                requestDBRType = DBRType.STS_ENUM;
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
                requestDBRType = DBRType.TIME_ENUM;
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
        } catch (Exception e) {
            message(e.getMessage(),MessageType.error);
            channelGetRequester.getDone(RequestResult.failure);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#getRequesterName()
     */
    public String getRequesterName() {
        return channel.getRequesterName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        channel.message(message, messageType);   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelGet#destroy()
     */
    public void destroy() {
        isDestroyed = true;
        if(channelProcess!=null) channelProcess.destroy();
        channel.remove(this);
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
        } catch (Exception e) {
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
        gov.aps.jca.dbr.Status status = null;
        gov.aps.jca.dbr.TimeStamp timeStamp = null;
        gov.aps.jca.dbr.Severity severity = null;
        double displayLow = 0.0;
        double displayHigh = 0.0;
        double controlLow = 0.0;
        double controlHigh = 0.0;
        String units = null;
        int precision = -1;

        if(pvIndex!=null) {
            DBR_Short dbr = (DBR_Short)getEvent.getDBR();
            pvIndex.put(dbr.getShortValue()[0]);
            dbIndex.postPut();
        } else if(requestDBRType==DBRType.BYTE) {
            DBR_Byte dbr = (DBR_Byte)getEvent.getDBR();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_BYTE) {
            DBR_STS_Byte dbr = (DBR_STS_Byte)getEvent.getDBR();
            status = dbr.getStatus();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }
        } else if(requestDBRType==DBRType.TIME_BYTE) {
            DBR_TIME_Byte dbr = (DBR_TIME_Byte)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }
        } else if(requestDBRType==DBRType.GR_BYTE) {
            DBR_GR_Byte dbr = (DBR_GR_Byte)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }
        } else if(requestDBRType==DBRType.CTRL_BYTE) {
            DBR_CTRL_Byte dbr = (DBR_CTRL_Byte)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            controlLow = dbr.getLowerCtrlLimit().doubleValue();
            controlHigh = dbr.getUpperCtrlLimit().doubleValue();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }

        } else if(requestDBRType==DBRType.SHORT) {
            DBR_Short dbr = (DBR_Short)getEvent.getDBR();
            if(elementCount==1) {
                PVShort pvValue = pvRecord.getShortField(valueFieldName);
                pvValue.put(dbr.getShortValue()[0]);
            } else {
                PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField(valueFieldName,Type.pvShort);
                pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_SHORT) {
            DBR_STS_Short dbr = (DBR_STS_Short)getEvent.getDBR();
            status = dbr.getStatus();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVShort pvValue = pvRecord.getShortField(valueFieldName);
                pvValue.put(dbr.getShortValue()[0]);
            } else {
                PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField(valueFieldName,Type.pvShort);
                pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
            }
        } else if(requestDBRType==DBRType.TIME_SHORT) {
            DBR_TIME_Short dbr = (DBR_TIME_Short)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVShort pvValue = pvRecord.getShortField(valueFieldName);
                pvValue.put(dbr.getShortValue()[0]);
            } else {
                PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField(valueFieldName,Type.pvShort);
                pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
            }
        } else if(requestDBRType==DBRType.GR_SHORT) {
            DBR_GR_Short dbr = (DBR_GR_Short)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            if(elementCount==1) {
                PVShort pvValue = pvRecord.getShortField(valueFieldName);
                pvValue.put(dbr.getShortValue()[0]);
            } else {
                PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField(valueFieldName,Type.pvShort);
                pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
            }
        } else if(requestDBRType==DBRType.CTRL_SHORT) {
            DBR_CTRL_Short dbr = (DBR_CTRL_Short)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            controlLow = dbr.getLowerCtrlLimit().doubleValue();
            controlHigh = dbr.getUpperCtrlLimit().doubleValue();
            if(elementCount==1) {
                PVShort pvValue = pvRecord.getShortField(valueFieldName);
                pvValue.put(dbr.getShortValue()[0]);
            } else {
                PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField(valueFieldName,Type.pvShort);
                pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
            }


        } else if(requestDBRType==DBRType.INT) {
            DBR_Int dbr = (DBR_Int)getEvent.getDBR();
            if(elementCount==1) {
                PVInt pvValue = pvRecord.getIntField(valueFieldName);
                pvValue.put(dbr.getIntValue()[0]);
            } else {
                PVIntArray pvValue = (PVIntArray)pvRecord.getArrayField(valueFieldName,Type.pvInt);
                pvValue.put(0, dbr.getCount(), dbr.getIntValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_INT) {
            DBR_STS_Int dbr = (DBR_STS_Int)getEvent.getDBR();
            status = dbr.getStatus();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVInt pvValue = pvRecord.getIntField(valueFieldName);
                pvValue.put(dbr.getIntValue()[0]);
            } else {
                PVIntArray pvValue = (PVIntArray)pvRecord.getArrayField(valueFieldName,Type.pvInt);
                pvValue.put(0, dbr.getCount(), dbr.getIntValue(), 0);
            }
        } else if(requestDBRType==DBRType.TIME_INT) {
            DBR_TIME_Int dbr = (DBR_TIME_Int)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVInt pvValue = pvRecord.getIntField(valueFieldName);
                pvValue.put(dbr.getIntValue()[0]);
            } else {
                PVIntArray pvValue = (PVIntArray)pvRecord.getArrayField(valueFieldName,Type.pvInt);
                pvValue.put(0, dbr.getCount(), dbr.getIntValue(), 0);
            }
        } else if(requestDBRType==DBRType.GR_INT) {
            DBR_GR_Int dbr = (DBR_GR_Int)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            if(elementCount==1) {
                PVInt pvValue = pvRecord.getIntField(valueFieldName);
                pvValue.put(dbr.getIntValue()[0]);
            } else {
                PVIntArray pvValue = (PVIntArray)pvRecord.getArrayField(valueFieldName,Type.pvInt);
                pvValue.put(0, dbr.getCount(), dbr.getIntValue(), 0);
            }
        } else if(requestDBRType==DBRType.CTRL_INT) {
            DBR_CTRL_Byte dbr = (DBR_CTRL_Byte)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            controlLow = dbr.getLowerCtrlLimit().doubleValue();
            controlHigh = dbr.getUpperCtrlLimit().doubleValue();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }

        } else if(requestDBRType==DBRType.FLOAT) {
            DBR_Float dbr = (DBR_Float)getEvent.getDBR();
            if(elementCount==1) {
                PVFloat pvValue = pvRecord.getFloatField(valueFieldName);
                pvValue.put(dbr.getFloatValue()[0]);
            } else {
                PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField(valueFieldName,Type.pvFloat);
                pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_FLOAT) {
            DBR_STS_Float dbr = (DBR_STS_Float)getEvent.getDBR();
            status = dbr.getStatus();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVFloat pvValue = pvRecord.getFloatField(valueFieldName);
                pvValue.put(dbr.getFloatValue()[0]);
            } else {
                PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField(valueFieldName,Type.pvFloat);
                pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
            }
        } else if(requestDBRType==DBRType.TIME_FLOAT) {
            DBR_TIME_Float dbr = (DBR_TIME_Float)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVFloat pvValue = pvRecord.getFloatField(valueFieldName);
                pvValue.put(dbr.getFloatValue()[0]);
            } else {
                PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField(valueFieldName,Type.pvFloat);
                pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
            }
        } else if(requestDBRType==DBRType.GR_FLOAT) {
            DBR_GR_Float dbr = (DBR_GR_Float)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            if(elementCount==1) {
                PVFloat pvValue = pvRecord.getFloatField(valueFieldName);
                pvValue.put(dbr.getFloatValue()[0]);
            } else {
                PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField(valueFieldName,Type.pvFloat);
                pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
            }
        } else if(requestDBRType==DBRType.CTRL_FLOAT) {
            DBR_CTRL_Float dbr = (DBR_CTRL_Float)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            controlLow = dbr.getLowerCtrlLimit().doubleValue();
            controlHigh = dbr.getUpperCtrlLimit().doubleValue();
            if(elementCount==1) {
                PVFloat pvValue = pvRecord.getFloatField(valueFieldName);
                pvValue.put(dbr.getFloatValue()[0]);
            } else {
                PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField(valueFieldName,Type.pvFloat);
                pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
            }
        } else if(requestDBRType==DBRType.DOUBLE) {
            DBR_Double dbr = (DBR_Double)getEvent.getDBR();
            if(elementCount==1) {
                PVDouble pvValue = pvRecord.getDoubleField(valueFieldName);
                pvValue.put(dbr.getDoubleValue()[0]);
            } else {
                PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField(valueFieldName,Type.pvDouble);
                pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_DOUBLE) {
            DBR_STS_Double dbr = (DBR_STS_Double)getEvent.getDBR();
            status = dbr.getStatus();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVDouble pvValue = pvRecord.getDoubleField(valueFieldName);
                pvValue.put(dbr.getDoubleValue()[0]);
            } else {
                PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField(valueFieldName,Type.pvDouble);
                pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
            }
        } else if(requestDBRType==DBRType.TIME_DOUBLE) {
            DBR_TIME_Double dbr = (DBR_TIME_Double)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVDouble pvValue = pvRecord.getDoubleField(valueFieldName);
                pvValue.put(dbr.getDoubleValue()[0]);
            } else {
                PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField(valueFieldName,Type.pvDouble);
                pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
            }
        } else if(requestDBRType==DBRType.GR_DOUBLE) {
            DBR_GR_Double dbr = (DBR_GR_Double)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            precision = dbr.getPrecision();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            if(elementCount==1) {
                PVDouble pvValue = pvRecord.getDoubleField(valueFieldName);
                pvValue.put(dbr.getDoubleValue()[0]);
            } else {
                PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField(valueFieldName,Type.pvDouble);
                pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
            }
        } else if(requestDBRType==DBRType.CTRL_DOUBLE) {
            DBR_CTRL_Double dbr = (DBR_CTRL_Double)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            units = dbr.getUnits();
            precision = dbr.getPrecision();
            displayLow = dbr.getLowerDispLimit().doubleValue();
            displayHigh = dbr.getUpperDispLimit().doubleValue();
            controlLow = dbr.getLowerCtrlLimit().doubleValue();
            controlHigh = dbr.getUpperCtrlLimit().doubleValue();
            if(elementCount==1) {
                PVDouble pvValue = pvRecord.getDoubleField(valueFieldName);
                pvValue.put(dbr.getDoubleValue()[0]);
            } else {
                PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField(valueFieldName,Type.pvDouble);
                pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
            }
        } else if(requestDBRType==DBRType.STRING) {
            DBR_String dbr = (DBR_String)getEvent.getDBR();
            if(elementCount==1) {
                PVString pvValue = pvRecord.getStringField(valueFieldName);
                pvValue.put(dbr.getStringValue()[0]);
            } else {
                PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField(valueFieldName,Type.pvString);
                pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_STRING) {
            DBR_STS_String dbr = (DBR_STS_String)getEvent.getDBR();
            status = dbr.getStatus();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVString pvValue = pvRecord.getStringField(valueFieldName);
                pvValue.put(dbr.getStringValue()[0]);
            } else {
                PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField(valueFieldName,Type.pvString);
                pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
            }
        } else if(requestDBRType==DBRType.TIME_STRING) {
            DBR_TIME_String dbr = (DBR_TIME_String)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVString pvValue = pvRecord.getStringField(valueFieldName);
                pvValue.put(dbr.getStringValue()[0]);
            } else {
                PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField(valueFieldName,Type.pvString);
                pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
            }
        } else if(requestDBRType==DBRType.GR_STRING) {
            DBR_GR_String dbr = (DBR_GR_String)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVString pvValue = pvRecord.getStringField(valueFieldName);
                pvValue.put(dbr.getStringValue()[0]);
            } else {
                PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField(valueFieldName,Type.pvString);
                pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
            }
        } else if(requestDBRType==DBRType.CTRL_STRING) {
            DBR_CTRL_String dbr = (DBR_CTRL_String)getEvent.getDBR();
            status = dbr.getStatus();
            timeStamp = dbr.getTimeStamp();
            severity = dbr.getSeverity();
            if(elementCount==1) {
                PVString pvValue = pvRecord.getStringField(valueFieldName);
                pvValue.put(dbr.getStringValue()[0]);
            } else {
                PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField(valueFieldName,Type.pvString);
                pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
            }    
        }
        PVStructure pvStructure = null;
        pvStructure = pvRecord.getStructureField("timeStamp", "timeStamp");
        if(timeStamp!=null && pvStructure!=null) {
            PVLong pvSeconds = pvStructure.getLongField("secondsPastEpoch");
            long seconds = timeStamp.secPastEpoch();
            seconds += 7305*86400;
            pvSeconds.put(seconds);
            PVInt pvNano = pvStructure.getIntField("nanoSeconds");
            pvNano.put((int)timeStamp.nsec());
        }
        pvStructure = pvRecord.getStructureField("alarm", "alarm");
        if(severity!=null && pvStructure!=null) {
            PVString pvMessage = pvStructure.getStringField("message");
            pvMessage.put(status.getName());
            PVEnumerated pvEnumerated = (PVEnumerated)pvStructure.getStructureField(
                    "severity","alarmSeverity").getPVEnumerated();
            PVInt pvIndex = pvEnumerated.getIndexField();
            pvIndex.put(severity.getValue());
        }
        pvStructure = pvRecord.getStructureField("display", "display");
        if(displayLow<displayHigh && pvStructure!=null) {
            if(units!=null) {
                PVString pvUnits = pvStructure.getStringField("units");
                if(pvUnits!=null) pvUnits.put(units.toString());
            }
            if(precision>=0) {
                PVInt pvResolution = pvStructure.getIntField("resolution");
                if(pvResolution!=null) pvResolution.put(precision);
            }
            pvStructure = pvStructure.getStructureField("limit","doubleLimit");
            if(pvStructure!=null) {
                PVDouble pvLow = pvStructure.getDoubleField("low");
                PVDouble pvHigh = pvStructure.getDoubleField("high");
                if(pvLow!=null && pvHigh!=null) {
                    pvLow.put(displayLow);
                    pvHigh.put(displayHigh);
                }
            }
        }
        pvStructure = pvRecord.getStructureField("control", "control");
        if(controlLow<controlHigh && pvStructure!=null) {
            pvStructure = pvStructure.getStructureField("limit","doubleLimit");
            if(pvStructure!=null) {
                PVDouble pvLow = pvStructure.getDoubleField("low");
                PVDouble pvHigh = pvStructure.getDoubleField("high");
                if(pvLow!=null && pvHigh!=null) {
                    pvLow.put(controlLow);
                    pvHigh.put(controlHigh);
                }
            }
        }
        Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
        while(channelFieldListIter.hasNext()) {
            ChannelField channelField = channelFieldListIter.next();
            channelGetRequester.nextGetField(channelField, channelField.getPVField());
        }
        channelGetRequester.getDone(RequestResult.success);
    }
}
