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
import gov.aps.jca.dbr.DBR_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Byte;
import gov.aps.jca.dbr.DBR_CTRL_Double;
import gov.aps.jca.dbr.DBR_CTRL_Enum;
import gov.aps.jca.dbr.DBR_CTRL_Float;
import gov.aps.jca.dbr.DBR_CTRL_Int;
import gov.aps.jca.dbr.DBR_CTRL_Short;
import gov.aps.jca.dbr.DBR_CTRL_String;
import gov.aps.jca.dbr.DBR_Double;
import gov.aps.jca.dbr.DBR_Float;
import gov.aps.jca.dbr.DBR_GR_Byte;
import gov.aps.jca.dbr.DBR_GR_Double;
import gov.aps.jca.dbr.DBR_GR_Float;
import gov.aps.jca.dbr.DBR_GR_Int;
import gov.aps.jca.dbr.DBR_GR_Short;
import gov.aps.jca.dbr.DBR_GR_String;
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
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelMonitorRequester;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVByteArray;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
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
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * @author mrk
 *
 */
public class BaseV3ChannelRecord implements V3ChannelRecord,GetListener,Runnable {
    protected static final Convert convert = ConvertFactory.getConvert();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static DBD dbd = DBDFactory.getMasterDBD();
    private static enum DBRProperty {none,status,time,graphic,control};
    
    private V3Channel v3Channel;
    private IOCExecutor iocExecutor = null;
    
    private DBRType nativeDBRType = null;
    private PVRecord pvRecord = null;
    private StringArrayData alarmSeverityData = new StringArrayData();
    private StringArrayData valueChoicesData = null;
    
    private V3ChannelRecordRequester v3ChannelRecordRequester = null;
    private RequestResult requestResult = null;
    
    /**
     * The Constructor
     * @param v3Channel The v3Channel.
     */
    public BaseV3ChannelRecord(V3Channel v3Channel) {
        this.v3Channel = v3Channel;
        iocExecutor = v3Channel.getIOCExecutor();
    }
    /**
     * Create a PVRecord for the Channel.
     * @param recordName
     */
    public boolean createPVRecord(
        V3ChannelRecordRequester v3ChannelRecordRequester,String recordName)
    {
        this.v3ChannelRecordRequester = v3ChannelRecordRequester;
        requestResult = RequestResult.success;
        gov.aps.jca.Channel jcaChannel = v3Channel.getJCAChannel();
        int elementCount = jcaChannel.getElementCount();
        nativeDBRType = jcaChannel.getFieldType();
        if(nativeDBRType.isENUM()) {
            Type type = v3Channel.getEnumRequestType();
            if(type==Type.pvInt) {
                nativeDBRType = DBRType.INT;
            } else if(type==Type.pvString) {
                nativeDBRType = DBRType.STRING;
            }
        }
        String valueFieldName = v3Channel.getValueFieldName();
        String[] propertyNames = v3Channel.getPropertyNames();
        DBRProperty dbrProperty = DBRProperty.none;
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
        Type type = null;
        if(nativeDBRType.isBYTE()) {
            type = Type.pvByte;
        } else if(nativeDBRType.isSHORT()) {
            type= Type.pvShort;
        } else if(nativeDBRType.isINT()) {
            type = Type.pvInt;
        } else if(nativeDBRType.isFLOAT()) {
            type = Type.pvFloat;
        } else if(nativeDBRType.isDOUBLE()) {
            type = Type.pvDouble;
        } else if(nativeDBRType.isSTRING()) {
            type = Type.pvString;
        } else if(nativeDBRType.isENUM()) {
            valueChoicesData = new StringArrayData();
            type = Type.pvStructure;
        }
        Type elementType = null;
        Field valueField = null;
        if(type==Type.pvStructure) {
            DBDStructure dbdEnumerated = dbd.getStructure("enumerated");
            Field[] valueFields = dbdEnumerated.getFields();
            valueField = fieldCreate.createStructure(valueFieldName, "value", valueFields);
            if(dbrProperty==DBRProperty.control || dbrProperty==DBRProperty.graphic) {
                dbrProperty= DBRProperty.time;
            }
        } else if(elementCount<2) {
            valueField = fieldCreate.createField(valueFieldName, type);
        } else {
            elementType = type;
            type = Type.pvArray;
            valueField = fieldCreate.createArray(valueFieldName, elementType);
        }
        Field[] fields = new Field[propertyNames.length + 1];
        fields[0] = valueField;
        int index = 1;
        int notUsed = 0;
        for(String propertyName : propertyNames) {
            if(type==Type.pvStructure) {
                if(propertyName.equals("control")) {
                    notUsed++;
                    notUsed++;
                    continue;
                }
                if(propertyName.equals("display")) {
                    continue;
                }
            }
            DBDStructure dbdStructure = dbd.getStructure(propertyName);
            if(dbdStructure==null) dbdStructure = dbd.getStructure("null");
            Field[] propertyFields = dbdStructure.getFields();
            fields[index++] = fieldCreate.createStructure(propertyName, propertyName, propertyFields);
        }
        if(notUsed>0) {
            Field[] newFields = new Field[propertyNames.length + 1 - notUsed];
            index = 0;
            for(Field field : fields) {
                if(field!=null) {
                    newFields[index++] = field;
                }
            }
            fields = newFields;
        }
        Structure structure = fieldCreate.createStructure("caV3", "caV3", fields);
        pvRecord = pvDataCreate.createPVRecord(recordName, structure);
        PVField valuePVField = pvRecord.getPVFields()[0];
        if(nativeDBRType.isENUM()) {
            valuePVField.getPVEnumerated();
            try {
                jcaChannel.get(DBRType.CTRL_ENUM,1,this);
            } catch (CAException e) {
                return false;
            }
        } else {
            DBRType requestDBRType = null;
            switch(dbrProperty) {
            case none:
                requestDBRType = nativeDBRType;
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
                    requestDBRType = DBRType.STS_ENUM;
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
                    requestDBRType = DBRType.TIME_ENUM;
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
            try {
                jcaChannel.get(requestDBRType,1,this);
            } catch (CAException e) {
                return false;
            }
        }
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelRecord#getPVRecord()
     */
    public PVRecord getPVRecord() {
        return pvRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelRecord#getValueDBRType()
     */
    public DBRType getNativeDBRType() {
        return nativeDBRType;
    }
    private void setAlarm(AlarmSeverity alarmSeverity,String message,ChannelMonitorRequester channelMonitorRequester) {
        PVStructure pvStructure = pvRecord.getStructureField("alarm", "alarm");
        if(pvStructure!=null) {
            PVString pvMessage = pvStructure.getStringField("message");
            if(message==null || !message.equals(pvMessage.get())) {
                pvMessage.put(message);
                if(channelMonitorRequester!=null) {
                    channelMonitorRequester.dataPut(pvStructure, pvMessage);
                }
            }
            PVEnumerated pvEnumerated = (PVEnumerated)pvStructure.getStructureField(
                    "severity","alarmSeverity").getPVEnumerated();
            int index = alarmSeverity.ordinal();
            PVStringArray pvChoices = pvEnumerated.getChoicesField();
            int len = pvChoices.get(0, pvChoices.getLength(), alarmSeverityData);
            if(index<0 || index>=len) {
                throw new IllegalStateException("Logic error");
            }
            PVInt pvIndex = pvEnumerated.getIndexField();
            if(pvIndex.get()!=alarmSeverity.ordinal()) {
                pvIndex.put(index);
                PVString pvChoice = pvEnumerated.getChoiceField();
                pvChoice.put(alarmSeverityData.data[index]);
                if(channelMonitorRequester!=null) {
                    channelMonitorRequester.dataPut(pvStructure, pvIndex);
                    channelMonitorRequester.dataPut(pvStructure, pvChoice);
                }
            }
        } else {
            System.err.println(
                pvRecord.getRecordName()
                + " v3Ca error " + message
                + " severity " + alarmSeverity.toString());
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelRecord#toRecord(gov.aps.jca.dbr.DBR, org.epics.ioc.ca.ChannelMonitorRequester)
     */
    public void toRecord(DBR fromDBR,ChannelMonitorRequester channelMonitorRequester) {
        if(fromDBR==null) {
            setAlarm(AlarmSeverity.invalid,"fromDBR is null",channelMonitorRequester);
            return;
        }
        gov.aps.jca.dbr.Status status = null;
        gov.aps.jca.dbr.TimeStamp timeStamp = null;
        gov.aps.jca.dbr.Severity severity = null;
        gov.aps.jca.Channel jcaChannel = v3Channel.getJCAChannel();
        int elementCount = jcaChannel.getElementCount();
        
        double displayLow = 0.0;
        double displayHigh = 0.0;
        double controlLow = 0.0;
        double controlHigh = 0.0;
        String units = null;
        int precision = -1;
        DBRType requestDBRType = fromDBR.getType();
        PVField pvValueField = pvRecord.findProperty(v3Channel.getValueFieldName());
        
        if(nativeDBRType.isENUM()) {
            if(elementCount!=1) {
                setAlarm(AlarmSeverity.invalid,
                        "DBRType is ENUM but elementCount is "
                        + elementCount,
                        channelMonitorRequester);
                return;
            }
            PVEnumerated pvEnumerated = pvValueField.getPVEnumerated();
            if(pvEnumerated==null) {
                setAlarm(AlarmSeverity.invalid,
                        "DBRType is ENUM but "
                        + pvValueField.getFullFieldName()
                        + " is not an enumerated structure",channelMonitorRequester);
                return;
            }
            PVInt pvIndex = pvEnumerated.getIndexField();
            PVString pvChoice = pvEnumerated.getChoiceField();
            PVStringArray pvChoices = pvEnumerated.getChoicesField();
            int numChoices = pvChoices.get(0, pvChoices.getLength(), valueChoicesData);
            if(requestDBRType==DBRType.CTRL_ENUM) {
                DBR_CTRL_Enum dbr = (DBR_CTRL_Enum)fromDBR;
                String[] labels = dbr.getLabels();
                pvChoices.put(0, labels.length, labels, 0);
                int index = dbr.getEnumValue()[0];
                numChoices = pvChoices.get(0, labels.length, valueChoicesData);
                pvIndex.put(index);
                pvChoice.put(valueChoicesData.data[index]);
                if(channelMonitorRequester!=null) {
                    channelMonitorRequester.dataPut(pvValueField, pvChoices);
                    channelMonitorRequester.dataPut(pvValueField, pvIndex);
                    channelMonitorRequester.dataPut(pvValueField, pvChoice);
                }
            } else if(requestDBRType==DBRType.INT) {
                DBR_Int dbr = (DBR_Int)fromDBR;
                int index = dbr.getIntValue()[0];
                if(index!=pvIndex.get()) {
                    pvIndex.put(index);
                    pvChoice.put(valueChoicesData.data[index]);
                    if(channelMonitorRequester!=null) {
                        channelMonitorRequester.dataPut(pvValueField, pvIndex);
                        channelMonitorRequester.dataPut(pvValueField, pvChoice);
                    }
                }
            } else if(requestDBRType==DBRType.STS_INT) {
                DBR_STS_Int dbr = (DBR_STS_Int)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                int index = dbr.getIntValue()[0];
                if(index!=pvIndex.get()) {
                    pvIndex.put(index);
                    pvChoice.put(valueChoicesData.data[index]);
                    if(channelMonitorRequester!=null) {
                        channelMonitorRequester.dataPut(pvValueField, pvIndex);
                        channelMonitorRequester.dataPut(pvValueField, pvChoice);
                    }
                }
            } else if(requestDBRType==DBRType.TIME_INT) {
                DBR_TIME_Int dbr = (DBR_TIME_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                int index = dbr.getIntValue()[0];
                if(index!=pvIndex.get()) {
                    pvIndex.put(index);
                    pvChoice.put(valueChoicesData.data[index]);
                    if(channelMonitorRequester!=null) {
                        channelMonitorRequester.dataPut(pvValueField, pvIndex);
                        channelMonitorRequester.dataPut(pvValueField, pvChoice);
                    }
                }
            } else if(requestDBRType==DBRType.GR_INT) {
                DBR_GR_Int dbr = (DBR_GR_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                int index = dbr.getIntValue()[0];
                if(index!=pvIndex.get()) {
                    pvIndex.put(index);
                    pvChoice.put(valueChoicesData.data[index]);
                    if(channelMonitorRequester!=null) {
                        channelMonitorRequester.dataPut(pvValueField, pvIndex);
                        channelMonitorRequester.dataPut(pvValueField, pvChoice);
                    }
                }
            } else if(requestDBRType==DBRType.CTRL_INT) {
                DBR_CTRL_Int dbr = (DBR_CTRL_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                controlLow = dbr.getLowerCtrlLimit().doubleValue();
                controlHigh = dbr.getUpperCtrlLimit().doubleValue();
                int index = dbr.getIntValue()[0];
                if(index!=pvIndex.get()) {
                    pvIndex.put(index);
                    pvChoice.put(valueChoicesData.data[index]);
                    if(channelMonitorRequester!=null) {
                        channelMonitorRequester.dataPut(pvValueField, pvIndex);
                        channelMonitorRequester.dataPut(pvValueField, pvChoice);
                    }
                }
            }  else if(requestDBRType==DBRType.STRING) {
                DBR_String dbr = (DBR_String)fromDBR;
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    int index = -1;
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            pvIndex.put(index);
                            pvChoice.put(valueChoicesData.data[index]);
                            if(channelMonitorRequester!=null) {
                                channelMonitorRequester.dataPut(pvValueField, pvIndex);
                                channelMonitorRequester.dataPut(pvValueField, pvChoice);
                            }
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice",channelMonitorRequester);
                    }
                }
            } else if(requestDBRType==DBRType.STS_STRING) {
                DBR_STS_String dbr = (DBR_STS_String)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    int index = -1;
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            pvIndex.put(index);
                            pvChoice.put(valueChoicesData.data[index]);
                            if(channelMonitorRequester!=null) {
                                channelMonitorRequester.dataPut(pvValueField, pvIndex);
                                channelMonitorRequester.dataPut(pvValueField, pvChoice);
                            }
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice",channelMonitorRequester);
                    } 
                }
            } else if(requestDBRType==DBRType.TIME_STRING) {
                DBR_TIME_String dbr = (DBR_TIME_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    int index = -1;
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            pvIndex.put(index);
                            pvChoice.put(valueChoicesData.data[index]);
                            if(channelMonitorRequester!=null) {
                                channelMonitorRequester.dataPut(pvValueField, pvIndex);
                                channelMonitorRequester.dataPut(pvValueField, pvChoice);
                            }
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice",channelMonitorRequester);
                    } 
                }
            } else if(requestDBRType==DBRType.GR_STRING) {
                DBR_GR_String dbr = (DBR_GR_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    int index = -1;
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            pvIndex.put(index);
                            pvChoice.put(valueChoicesData.data[index]);
                            if(channelMonitorRequester!=null) {
                                channelMonitorRequester.dataPut(pvValueField, pvIndex);
                                channelMonitorRequester.dataPut(pvValueField, pvChoice);
                            }
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice",channelMonitorRequester);
                    } 
                }
            } else if(requestDBRType==DBRType.CTRL_STRING) {
                DBR_CTRL_String dbr = (DBR_CTRL_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    int index = -1;
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            pvIndex.put(index);
                            pvChoice.put(valueChoicesData.data[index]);
                            if(channelMonitorRequester!=null) {
                                channelMonitorRequester.dataPut(pvValueField, pvIndex);
                                channelMonitorRequester.dataPut(pvValueField, pvChoice);
                            }
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice",channelMonitorRequester);
                    } 
                }
            } else {
                setAlarm(AlarmSeverity.invalid,
                        " unsupported DBRType " + requestDBRType.getName(),
                        channelMonitorRequester);
                return;
            }
        } else {
            if(requestDBRType==DBRType.DOUBLE) {
                DBR_Double dbr = (DBR_Double)fromDBR;
                if(elementCount==1) {
                    convert.fromDouble(pvValueField, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvValueField, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_DOUBLE) {
                DBR_STS_Double dbr = (DBR_STS_Double)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromDouble(pvValueField, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvValueField, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_DOUBLE) {
                DBR_TIME_Double dbr = (DBR_TIME_Double)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromDouble(pvValueField, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvValueField, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.SHORT) {
                DBR_Short dbr = (DBR_Short)fromDBR;
                if(elementCount==1) {
                    convert.fromShort(pvValueField, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvValueField, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_SHORT) {
                DBR_STS_Short dbr = (DBR_STS_Short)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromShort(pvValueField, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvValueField, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_SHORT) {
                DBR_TIME_Short dbr = (DBR_TIME_Short)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromShort(pvValueField, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvValueField, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.INT) {
                DBR_Int dbr = (DBR_Int)fromDBR;
                if(elementCount==1) {
                    convert.fromInt(pvValueField, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvValueField, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_INT) {
                DBR_STS_Int dbr = (DBR_STS_Int)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromInt(pvValueField, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvValueField, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_INT) {
                DBR_TIME_Int dbr = (DBR_TIME_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromInt(pvValueField, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvValueField, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.BYTE) {
                DBR_Byte dbr = (DBR_Byte)fromDBR;
                if(elementCount==1) {
                    convert.fromByte(pvValueField, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvValueField, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_BYTE) {
                DBR_STS_Byte dbr = (DBR_STS_Byte)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromByte(pvValueField, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvValueField, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_BYTE) {
                DBR_TIME_Byte dbr = (DBR_TIME_Byte)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromByte(pvValueField, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvValueField, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_BYTE) {
                DBR_GR_Byte dbr = (DBR_GR_Byte)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromByte(pvValueField, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvValueField, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_BYTE) {
                DBR_CTRL_Byte dbr = (DBR_CTRL_Byte)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                controlLow = dbr.getLowerCtrlLimit().doubleValue();
                controlHigh = dbr.getUpperCtrlLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromByte(pvValueField, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvValueField, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_SHORT) {
                DBR_GR_Short dbr = (DBR_GR_Short)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromShort(pvValueField, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvValueField, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_SHORT) {
                DBR_CTRL_Short dbr = (DBR_CTRL_Short)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                controlLow = dbr.getLowerCtrlLimit().doubleValue();
                controlHigh = dbr.getUpperCtrlLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromShort(pvValueField, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvValueField, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_INT) {
                DBR_GR_Int dbr = (DBR_GR_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromInt(pvValueField, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvValueField, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_INT) {
                DBR_CTRL_Int dbr = (DBR_CTRL_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                controlLow = dbr.getLowerCtrlLimit().doubleValue();
                controlHigh = dbr.getUpperCtrlLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromInt(pvValueField, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvValueField, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.FLOAT) {
                DBR_Float dbr = (DBR_Float)fromDBR;
                if(elementCount==1) {
                    convert.fromFloat(pvValueField, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvValueField, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_FLOAT) {
                DBR_STS_Float dbr = (DBR_STS_Float)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromFloat(pvValueField, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvValueField, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_FLOAT) {
                DBR_TIME_Float dbr = (DBR_TIME_Float)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromFloat(pvValueField, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvValueField, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_FLOAT) {
                DBR_GR_Float dbr = (DBR_GR_Float)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromFloat(pvValueField, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvValueField, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_FLOAT) {
                DBR_CTRL_Float dbr = (DBR_CTRL_Float)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                controlLow = dbr.getLowerCtrlLimit().doubleValue();
                controlHigh = dbr.getUpperCtrlLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromFloat(pvValueField, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvValueField, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_DOUBLE) {
                DBR_GR_Double dbr = (DBR_GR_Double)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                precision = dbr.getPrecision();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                if(elementCount==1) {
                    convert.fromDouble(pvValueField, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvValueField, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_DOUBLE) {
                DBR_CTRL_Double dbr = (DBR_CTRL_Double)fromDBR;
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
                    convert.fromDouble(pvValueField, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvValueField, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.STRING) {
                DBR_String dbr = (DBR_String)fromDBR;
                if(elementCount==1) {
                    convert.fromString(pvValueField, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray((PVArray)pvValueField, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_STRING) {
                DBR_STS_String dbr = (DBR_STS_String)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvValueField, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray((PVArray)pvValueField, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_STRING) {
                DBR_TIME_String dbr = (DBR_TIME_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvValueField, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray((PVArray)pvValueField, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_STRING) {
                DBR_GR_String dbr = (DBR_GR_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvValueField, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray((PVArray)pvValueField, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_STRING) {
                DBR_CTRL_String dbr = (DBR_CTRL_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvValueField, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray((PVArray)pvValueField, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else {
                setAlarm(AlarmSeverity.invalid,
                        " unsupported DBRType " + requestDBRType.getName(),
                        channelMonitorRequester);
                return;
            }
            if(channelMonitorRequester!=null) {
                channelMonitorRequester.dataPut(pvValueField);
            }
        }

        PVStructure pvStructure = null;
        if(timeStamp!=null) {
            pvStructure = pvRecord.getStructureField("timeStamp", "timeStamp");
            if(pvStructure!=null) {
                PVLong pvSeconds = pvStructure.getLongField("secondsPastEpoch");
                long seconds = timeStamp.secPastEpoch();
                seconds += 7305*86400;
                pvSeconds.put(seconds);
                PVInt pvNano = pvStructure.getIntField("nanoSeconds");
                pvNano.put((int)timeStamp.nsec());
                if(channelMonitorRequester!=null) {
                    channelMonitorRequester.dataPut(pvStructure);
                }
            }
        }
        if(severity!=null) {
            pvStructure = pvRecord.getStructureField("alarm", "alarm");
            if(pvStructure!=null) {
                int index = severity.getValue();
                AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(index);
                String message = status.getName();
                setAlarm(alarmSeverity,message,channelMonitorRequester);
            }
        }
        if(displayLow<displayHigh) {
            pvStructure = pvRecord.getStructureField("display", "display");
            if(pvStructure!=null) {
                if(units!=null) {
                    PVString pvUnits = pvStructure.getStringField("units");
                    if(pvUnits!=null) {
                        pvUnits.put(units.toString());
                        if(channelMonitorRequester!=null) {
                            channelMonitorRequester.dataPut(pvStructure,pvUnits);
                        }
                    }
                }
                if(precision>=0) {
                    PVInt pvResolution = pvStructure.getIntField("resolution");
                    if(pvResolution!=null) {
                        pvResolution.put(precision);
                        if(channelMonitorRequester!=null) {
                            channelMonitorRequester.dataPut(pvStructure,pvResolution);
                        }
                    }
                }
                PVStructure pvLimits = pvStructure.getStructureField("limit","doubleLimit");
                if(pvLimits!=null) {
                    PVDouble pvLow = pvLimits.getDoubleField("low");
                    PVDouble pvHigh = pvLimits.getDoubleField("high");
                    if(pvLow!=null && pvHigh!=null) {
                        pvLow.put(displayLow);
                        pvHigh.put(displayHigh);
                        if(channelMonitorRequester!=null) {
                            channelMonitorRequester.dataPut(pvStructure,pvLow);
                            channelMonitorRequester.dataPut(pvStructure,pvHigh);
                        }
                    }
                }
            }
        }
        if(controlLow<controlHigh) {
            pvStructure = pvRecord.getStructureField("control", "control");
            if(pvStructure!=null) {
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
        }
    }
    /* (non-Javadoc)
     * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
     */
    public void getCompleted(GetEvent getEvent) {
        DBR fromDBR = getEvent.getDBR();
        if(fromDBR==null) {
            CAStatus caStatus = getEvent.getStatus();
            ChannelListener channelListener = v3Channel.getChannelListener();
            if(caStatus==null) {
                channelListener.message(getEvent.toString(),MessageType.error);
            } else {
                channelListener.message(caStatus.getMessage(),MessageType.error);
            }
            requestResult = RequestResult.failure;
        } else {
            toRecord(fromDBR,null);
            requestResult = RequestResult.success;
        }
        iocExecutor.execute(this);
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        v3ChannelRecordRequester.createPVRecordDone(requestResult);
    }
}
