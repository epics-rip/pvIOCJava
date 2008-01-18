/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAStatus;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_Int;
import gov.aps.jca.dbr.DBR_STS_Byte;
import gov.aps.jca.dbr.DBR_STS_Double;
import gov.aps.jca.dbr.DBR_STS_Float;
import gov.aps.jca.dbr.DBR_STS_Int;
import gov.aps.jca.dbr.DBR_STS_Short;
import gov.aps.jca.dbr.DBR_STS_String;
import gov.aps.jca.dbr.DBR_Short;
import gov.aps.jca.dbr.DBR_String;
import gov.aps.jca.dbr.DBR_TIME_Byte;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.dbr.DBR_TIME_Float;
import gov.aps.jca.dbr.DBR_TIME_Int;
import gov.aps.jca.dbr.DBR_TIME_Short;
import gov.aps.jca.dbr.DBR_TIME_String;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import java.util.Iterator;
import java.util.List;

import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelMonitor;
import org.epics.ioc.ca.ChannelMonitorRequester;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.pv.PVByte;
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
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.PVShortArray;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;

/**
 * Base class that implements ChannelMonitor for communicating with a V3 IOC.
 * @author mrk
 *
 */
public class BaseV3ChannelMonitor implements ChannelMonitor,MonitorListener
{
    private static enum DBRProperty {none,status,time};
    
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelMonitorRequester channelMonitorRequester;
    
    private gov.aps.jca.Channel jcaChannel = null;
    private DBRType valueDBRType = null;
    
    private V3Channel channel = null;
    private DBRecord dbRecord = null;
    private PVRecord pvRecord = null;
    private String valueFieldName = null;
    private String[] propertyNames = null;
    private int elementCount = 0;
    private PVField pvAlarm = null;
    
    
    private boolean isDestroyed = false;
    private DBRType requestDBRType = null;
    
    private PVField pvValue = null;
    private PVInt pvIndex = null;
    private DBField dbIndex = null;
    
    private List<ChannelField> channelFieldList;
    private DBRProperty dbrProperty = DBRProperty.none;

    private Monitor monitor = null;
    

    /**
     * Constructer.
     * @param channelMonitorRequester The channelMonitorRequester.
     */
    public BaseV3ChannelMonitor(ChannelMonitorRequester channelMonitorRequester) {
        this.channelMonitorRequester = channelMonitorRequester;
    }
    public boolean init(V3Channel channel)
    {
        this.channel = channel;
        jcaChannel = channel.getJcaChannel();;
        valueDBRType = channel.getValueDBRType();
        dbRecord = channel.getDBRecord();
        elementCount = jcaChannel.getElementCount();
        pvRecord = channel.getPVRecord();
        valueFieldName = channel.getValueFieldName();
        propertyNames = channel.getPropertyNames();
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#destroy()
     */
    public void destroy() {
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#getData(org.epics.ioc.ca.CD)
     */
    public void getData(CD cd) {
        List<ChannelField> channelFieldList = channelFieldGroup.getList();
        for(ChannelField cf : channelFieldList) {
            ChannelField channelField = (ChannelField)cf;
            PVField pvField = channelField.getPVField();
            cd.put(pvField);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#setFieldGroup(org.epics.ioc.ca.ChannelFieldGroup)
     */
    public void setFieldGroup(ChannelFieldGroup channelFieldGroup) {
        this.channelFieldGroup = channelFieldGroup;
        channelFieldList = channelFieldGroup.getList();
        pvValue = pvRecord.getPVFields()[0];
        if(valueDBRType.isENUM()) {
            if(elementCount!=1) {
                channelMonitorRequester.message(
                        "array of enumerated not supported", MessageType.error);
                    return;
            }
            PVEnumerated pvEnumerated = pvValue.getPVEnumerated();
            pvIndex = pvEnumerated.getIndexField();
            dbIndex = dbRecord.findDBField(pvIndex);
            requestDBRType = DBRType.SHORT;
            return;
        }
        
        
        if(propertyNames.length>0) {
            for(String propertyName : propertyNames) {
                if(propertyName.equals("alarm")&& (dbrProperty.compareTo(DBRProperty.status)<0)) {
                    dbrProperty = DBRProperty.status;
                    pvAlarm = pvRecord.findProperty("alarm");
                    continue;
                }
                if(propertyName.equals("timeStamp")&& (dbrProperty.compareTo(DBRProperty.time)<0)) {
                    dbrProperty = DBRProperty.time;
                    pvAlarm = pvRecord.findProperty("alarm");
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
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#start()
     */
    public void start() {
        try {
            monitor = jcaChannel.addMonitor(requestDBRType, elementCount, 0x0ff, this);
        } catch (Exception e) {
            channelMonitorRequester.message(e.getMessage(),MessageType.error);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitor#stop()
     */
    public void stop() {
        try {
            monitor.clear();
        } catch (Exception e) {
            channelMonitorRequester.message(e.getMessage(),MessageType.error);
        }
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.MonitorListener#monitorChanged(gov.aps.jca.event.MonitorEvent)
     */
    public void monitorChanged(MonitorEvent monitorEvent) {
        gov.aps.jca.dbr.Status status = null;
        gov.aps.jca.dbr.TimeStamp timeStamp = null;
        gov.aps.jca.dbr.Severity severity = null;
        CAStatus caStatus = monitorEvent.getStatus();
        if(!caStatus.isSuccessful()) {
            channelMonitorRequester.message(caStatus.getMessage(),MessageType.error);
            return;
        }
        channelMonitorRequester.beginPut();
        if(pvIndex!=null) {
            DBR_Short dbr = (DBR_Short)monitorEvent.getDBR();
            pvIndex.put(dbr.getShortValue()[0]);
            dbIndex.postPut();
        } else if(requestDBRType==DBRType.BYTE) {
            DBR_Byte dbr = (DBR_Byte)monitorEvent.getDBR();
            if(elementCount==1) {
                PVByte pvValue = pvRecord.getByteField(valueFieldName);
                pvValue.put(dbr.getByteValue()[0]);
            } else {
                PVByteArray pvValue = (PVByteArray)pvRecord.getArrayField(valueFieldName,Type.pvByte);
                pvValue.put(0, dbr.getCount(), dbr.getByteValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_BYTE) {
            DBR_STS_Byte dbr = (DBR_STS_Byte)monitorEvent.getDBR();
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
            DBR_TIME_Byte dbr = (DBR_TIME_Byte)monitorEvent.getDBR();
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
        } else if(requestDBRType==DBRType.SHORT) {
            DBR_Short dbr = (DBR_Short)monitorEvent.getDBR();
            if(elementCount==1) {
                PVShort pvValue = pvRecord.getShortField(valueFieldName);
                pvValue.put(dbr.getShortValue()[0]);
            } else {
                PVShortArray pvValue = (PVShortArray)pvRecord.getArrayField(valueFieldName,Type.pvShort);
                pvValue.put(0, dbr.getCount(), dbr.getShortValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_SHORT) {
            DBR_STS_Short dbr = (DBR_STS_Short)monitorEvent.getDBR();
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
            DBR_TIME_Short dbr = (DBR_TIME_Short)monitorEvent.getDBR();
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
        } else if(requestDBRType==DBRType.INT) {
            DBR_Int dbr = (DBR_Int)monitorEvent.getDBR();
            if(elementCount==1) {
                PVInt pvValue = pvRecord.getIntField(valueFieldName);
                pvValue.put(dbr.getIntValue()[0]);
            } else {
                PVIntArray pvValue = (PVIntArray)pvRecord.getArrayField(valueFieldName,Type.pvInt);
                pvValue.put(0, dbr.getCount(), dbr.getIntValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_INT) {
            DBR_STS_Int dbr = (DBR_STS_Int)monitorEvent.getDBR();
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
            DBR_TIME_Int dbr = (DBR_TIME_Int)monitorEvent.getDBR();
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
        } else if(requestDBRType==DBRType.FLOAT) {
            DBR_Float dbr = (DBR_Float)monitorEvent.getDBR();
            if(elementCount==1) {
                PVFloat pvValue = pvRecord.getFloatField(valueFieldName);
                pvValue.put(dbr.getFloatValue()[0]);
            } else {
                PVFloatArray pvValue = (PVFloatArray)pvRecord.getArrayField(valueFieldName,Type.pvFloat);
                pvValue.put(0, dbr.getCount(), dbr.getFloatValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_FLOAT) {
            DBR_STS_Float dbr = (DBR_STS_Float)monitorEvent.getDBR();
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
            DBR_TIME_Float dbr = (DBR_TIME_Float)monitorEvent.getDBR();
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
        } else if(requestDBRType==DBRType.DOUBLE) {
            DBR_Double dbr = (DBR_Double)monitorEvent.getDBR();
            if(elementCount==1) {
                PVDouble pvValue = pvRecord.getDoubleField(valueFieldName);
                pvValue.put(dbr.getDoubleValue()[0]);
            } else {
                PVDoubleArray pvValue = (PVDoubleArray)pvRecord.getArrayField(valueFieldName,Type.pvDouble);
                pvValue.put(0, dbr.getCount(), dbr.getDoubleValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_DOUBLE) {
            DBR_STS_Double dbr = (DBR_STS_Double)monitorEvent.getDBR();
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
            DBR_TIME_Double dbr = (DBR_TIME_Double)monitorEvent.getDBR();
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
        } else if(requestDBRType==DBRType.STRING) {
            DBR_String dbr = (DBR_String)monitorEvent.getDBR();
            if(elementCount==1) {
                PVString pvValue = pvRecord.getStringField(valueFieldName);
                pvValue.put(dbr.getStringValue()[0]);
            } else {
                PVStringArray pvValue = (PVStringArray)pvRecord.getArrayField(valueFieldName,Type.pvString);
                pvValue.put(0, dbr.getCount(), dbr.getStringValue(), 0);
            }
        } else if(requestDBRType==DBRType.STS_STRING) {
            DBR_STS_String dbr = (DBR_STS_String)monitorEvent.getDBR();
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
            DBR_TIME_String dbr = (DBR_TIME_String)monitorEvent.getDBR();
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
        if(pvIndex==null) {
            channelMonitorRequester.dataPut(pvValue);
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
            channelMonitorRequester.dataPut(pvStructure);
        }
        pvStructure = pvRecord.getStructureField("alarm", "alarm");
        if(severity!=null && pvStructure!=null) {
            PVString pvMessage = pvStructure.getStringField("message");
            String message = pvMessage.get();
            String statusValue = status.getName();
            //Message.put(status.getName());
            PVEnumerated pvEnumerated = (PVEnumerated)pvStructure.getStructureField(
                    "severity","alarmSeverity").getPVEnumerated();
            PVInt pvIndex = pvEnumerated.getIndexField();
            int oldValue = pvIndex.get();
            int newValue = severity.getValue();
            if(oldValue!=newValue || !message.equals(statusValue)) {
                pvIndex.put(severity.getValue());
                pvMessage.put(statusValue);
                channelMonitorRequester.dataPut(pvAlarm,pvIndex);
                channelMonitorRequester.dataPut(pvAlarm,pvMessage);
            }
            
        }
        Iterator<ChannelField> channelFieldListIter = channelFieldList.iterator();
        while(channelFieldListIter.hasNext()) {
            ChannelField channelField = channelFieldListIter.next();
            channelField.postPut();
        }
        
        channelMonitorRequester.endPut();
    }
}