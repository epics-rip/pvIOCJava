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
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.factory.PVReplaceFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorNode;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVLong;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */
public class BaseV3ChannelRecord implements V3ChannelRecord,GetListener,Runnable {
    protected static final Convert convert = ConvertFactory.getConvert();
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static enum DBRProperty {none,status,time,graphic,control};
    
    private V3Channel v3Channel;
    private Executor executor = null;
    private ExecutorNode executorNode = null;
    
    private DBRType nativeDBRType = null;
    private PVRecord pvRecord = null;
    private PVStructure pvAlarm = null;
    private PVString pvAlarmMessage = null;
    private PVInt pvAlarmIndex = null;
    private PVString pvAlarmChoice = null;
    private PVStringArray pvAlarmChoices = null;
    private String[] alarmChoices = null;
    private PVStructure pvTimeStamp = null;
    private PVLong pvSeconds = null;
    private PVInt pvNanoSeconds = null;
    private StringArrayData valueChoicesData = null;
    private PVScalar pvScalarValue = null;
    private PVArray pvArrayValue = null;
    // Following not null if nativeDBRType.isENUM(
    private PVInt pvIndex = null;
    private PVString pvChoice = null;
    private PVStringArray pvChoices = null;
    
    private V3ChannelRecordRequester v3ChannelRecordRequester = null;
    private RequestResult requestResult = null;
    
    /**
     * The Constructor
     * @param v3Channel The v3Channel.
     */
    public BaseV3ChannelRecord(V3Channel v3Channel) {
        this.v3Channel = v3Channel;
        executor = v3Channel.getExecutor();
        executorNode = executor.createNode(this);
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
            ScalarType type = v3Channel.getEnumRequestScalarType();
            if(type==ScalarType.pvInt) {
                nativeDBRType = DBRType.INT;
            } else if(type==ScalarType.pvString) {
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
        Type type = Type.scalar;
        ScalarType scalarType = null;
        if(nativeDBRType.isBYTE()) {
            scalarType = ScalarType.pvByte;
        } else if(nativeDBRType.isSHORT()) {
            scalarType= ScalarType.pvShort;
        } else if(nativeDBRType.isINT()) {
            scalarType = ScalarType.pvInt;
        } else if(nativeDBRType.isFLOAT()) {
            scalarType = ScalarType.pvFloat;
        } else if(nativeDBRType.isDOUBLE()) {
            scalarType = ScalarType.pvDouble;
        } else if(nativeDBRType.isSTRING()) {
            scalarType = ScalarType.pvString;
        } else if(nativeDBRType.isENUM()) {
            valueChoicesData = new StringArrayData();
            type = Type.structure;
        }
        Type elementType = null;
        Field valueField = null;
        if(type==Type.structure) {
            PVStructure pvEnumerated = masterPVDatabase.findStructure("org.epics.pvData.enumerated");
            Field[] valueFields = pvEnumerated.getStructure().getFields();
            valueField = fieldCreate.createStructure(valueFieldName, valueFields);
            if(dbrProperty==DBRProperty.control || dbrProperty==DBRProperty.graphic) {
                dbrProperty= DBRProperty.time;
            }
        } else if(elementCount<2) {
            valueField = fieldCreate.createScalar(valueFieldName, scalarType);
        } else {
            elementType = Type.scalarArray;
            valueField = fieldCreate.createArray(valueFieldName, scalarType);
        }
        Field[] fields = new Field[propertyNames.length + 1];
        fields[0] = valueField;
        int index = 1;
        int notUsed = 0;
        for(String propertyName : propertyNames) {
            if(elementType==Type.structure) {
                if(propertyName.equals("control")) {
                    notUsed++;
                    continue;
                }
                if(propertyName.equals("display")) {
                    notUsed++;
                    continue;
                }
            }
            PVStructure pvStructure = masterPVDatabase.findStructure(propertyName);
            if(pvStructure==null) pvStructure = masterPVDatabase.findStructure("org.epics.ioc." + propertyName);
            if(pvStructure==null) pvStructure = masterPVDatabase.findStructure("org.epics.pvData." + propertyName);
            if(pvStructure==null) {
                notUsed++;
                continue;
            }
            Field[] propertyFields = pvStructure.getStructure().getFields();
            fields[index++] = fieldCreate.createStructure(propertyName, propertyFields);
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
        pvRecord = pvDataCreate.createPVRecord(recordName,fields);
        if(nativeDBRType.isENUM()) {
            PVStructure pvStructure = (PVStructure)pvRecord.getPVFields()[0];
            PVAuxInfo pvAuxInfo = pvStructure.getPVAuxInfo();
            PVString pvString = (PVString)pvAuxInfo.createInfo("pvReplaceFactory", ScalarType.pvString);
            pvString.put("org.epics.pvData.enumeratedFactory");
            PVReplaceFactory.replace(masterPVDatabase, pvStructure);
        }
        for(PVField pvField : pvRecord.getPVFields()) {
            if(pvField.getField().getFieldName().equals("alarm")) {
                pvAlarm = (PVStructure)pvField;
                pvAlarmMessage = pvAlarm.getStringField("message");
                PVStructure pvStruct = pvAlarm.getStructureField("severity");
                if(pvStruct!=null) {
                    pvAlarmIndex = pvStruct.getIntField("index");
                    pvAlarmChoice = pvStruct.getStringField("choice");
                    pvAlarmChoices = (PVStringArray)pvStruct.getArrayField("choices", ScalarType.pvString);
                    PVStructure alarmSevr = masterPVDatabase.findStructure("org.epics.pvData.alarmSeverity");
                    PVStringArray pvStringArray = (PVStringArray)alarmSevr.getArrayField("choices", ScalarType.pvString);
                    StringArrayData stringArrayData = new StringArrayData();
                    pvStringArray.get(0, pvStringArray.getLength(), stringArrayData);
                    alarmChoices = stringArrayData.data;
                    pvAlarmChoices.put(0, pvStringArray.getLength(), alarmChoices,0);
                }
                if(pvAlarmMessage==null || pvAlarmIndex==null || pvAlarmChoice==null ) {
                    throw new RuntimeException("bad alarm structure");    
                }
            }
            if(pvField.getField().getFieldName().equals("timeStamp")) {
                pvTimeStamp = (PVStructure)pvField;
                pvSeconds = pvTimeStamp.getLongField("secondsPastEpoch");
                pvNanoSeconds = pvTimeStamp.getIntField("nanoSeconds");
                if(pvSeconds==null || pvNanoSeconds==null) {
                    throw new RuntimeException("bad timeStamp structure");
                }
            }
        }
        PVField pvValue = pvRecord.getPVFields()[0];
        if(nativeDBRType.isENUM()) {
            PVStructure pvStructure = (PVStructure)pvRecord.getPVFields()[0];
            pvIndex = pvStructure.getIntField("index");
            pvChoice = pvStructure.getStringField("choice");
            pvChoices =(PVStringArray)pvStructure.getArrayField("choices", ScalarType.pvString);
            String message = null;
            try {
                jcaChannel.get(DBRType.CTRL_ENUM,1,this);
            } catch (CAException e) {
                message = e.getMessage();
            } catch (IllegalStateException e) {
                message = e.getMessage();
            }
            if(message!=null) {
                v3Channel.message("get exception " + message, MessageType.error);
                return false;
            }
        } else {
            if(elementCount<2) {
                pvScalarValue = (PVScalar)pvValue;
            } else {
                pvArrayValue = (PVArray)pvValue;
            }
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
            String message = null;
            try {
                jcaChannel.get(requestDBRType,1,this);
            } catch (CAException e) {
                message = e.getMessage();
            } catch (IllegalStateException e) {
                message = e.getMessage();
            }
            if(message!=null) {
                v3Channel.message("get exception " + message, MessageType.error);
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
    
    private void setAlarm(AlarmSeverity alarmSeverity,String message) {
        int index = alarmSeverity.ordinal();
        if(pvAlarm!=null && index<alarmChoices.length) {
            String oldMessage = pvAlarmMessage.get();
            if(oldMessage!=message || (message!=null &&(!message.equals(oldMessage)))) {
                pvAlarmMessage.put(message);
            }
            if(pvAlarmIndex.get()!=index) {
                pvAlarmIndex.put(index);
            }
        } else {
            System.err.println(
                    pvRecord.getRecordName()
                    + " v3Ca error " + message
                    + " severity " + alarmSeverity.toString());
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3ChannelRecord#toRecord(gov.aps.jca.dbr.DBR)
     */
    public void toRecord(DBR fromDBR) {
        if(fromDBR==null) {
            setAlarm(AlarmSeverity.invalid,"fromDBR is null");
            return;
        }
        pvRecord.beginGroupPut();
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
        if(nativeDBRType.isENUM()) {
            int index = pvIndex.get();
            if(requestDBRType==DBRType.CTRL_ENUM) {
                DBR_CTRL_Enum dbr = (DBR_CTRL_Enum)fromDBR;
                String[] labels = dbr.getLabels();
                pvChoices.put(0, labels.length, labels, 0);
                index = dbr.getEnumValue()[0];
            } else if(requestDBRType==DBRType.INT) {
                DBR_Int dbr = (DBR_Int)fromDBR;
                index = dbr.getIntValue()[0];
            } else if(requestDBRType==DBRType.STS_INT) {
                DBR_STS_Int dbr = (DBR_STS_Int)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                index = dbr.getIntValue()[0];
            } else if(requestDBRType==DBRType.TIME_INT) {
                DBR_TIME_Int dbr = (DBR_TIME_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                index = dbr.getIntValue()[0];
            } else if(requestDBRType==DBRType.GR_INT) {
                DBR_GR_Int dbr = (DBR_GR_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                units = dbr.getUnits();
                displayLow = dbr.getLowerDispLimit().doubleValue();
                displayHigh = dbr.getUpperDispLimit().doubleValue();
                index = dbr.getIntValue()[0];
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
                index = dbr.getIntValue()[0];
            }  else if(requestDBRType==DBRType.STRING) {
                DBR_String dbr = (DBR_String)fromDBR;
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    index = -1;
                    int numChoices = pvChoices.get(0, pvChoices.getLength(), valueChoicesData);
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice");
                    }
                }
            } else if(requestDBRType==DBRType.STS_STRING) {
                DBR_STS_String dbr = (DBR_STS_String)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    index = -1;
                    int numChoices = pvChoices.get(0, pvChoices.getLength(), valueChoicesData);
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice");
                    } 
                }
            } else if(requestDBRType==DBRType.TIME_STRING) {
                DBR_TIME_String dbr = (DBR_TIME_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    index = -1;
                    int numChoices = pvChoices.get(0, pvChoices.getLength(), valueChoicesData);
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice");
                    } 
                }
            } else if(requestDBRType==DBRType.GR_STRING) {
                DBR_GR_String dbr = (DBR_GR_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    index = -1;
                    int numChoices = pvChoices.get(0, pvChoices.getLength(), valueChoicesData);
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice");
                    } 
                }
            } else if(requestDBRType==DBRType.CTRL_STRING) {
                DBR_CTRL_String dbr = (DBR_CTRL_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                String choice = dbr.getStringValue()[0];
                if(!choice.equals(pvChoice.get())) {
                    index = -1;
                    int numChoices = pvChoices.get(0, pvChoices.getLength(), valueChoicesData);
                    for(int i=0; i<numChoices; i++) {
                        if(choice.equals(valueChoicesData.data[i])) {
                            index = i;
                            break;
                        }
                    }
                    if(index==-1) {
                        setAlarm(AlarmSeverity.major,"Invalid choice");
                    } 
                }
            } else {
                setAlarm(AlarmSeverity.invalid,
                        " unsupported DBRType " + requestDBRType.getName());
                return;
            }
            if(index!=pvIndex.get()) {
                pvIndex.put(index);
            }
        } else {
            if(requestDBRType==DBRType.DOUBLE) {
                DBR_Double dbr = (DBR_Double)fromDBR;
                if(elementCount==1) {
                    convert.fromDouble(pvScalarValue, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvArrayValue, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_DOUBLE) {
                DBR_STS_Double dbr = (DBR_STS_Double)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromDouble(pvScalarValue, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvArrayValue, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_DOUBLE) {
                DBR_TIME_Double dbr = (DBR_TIME_Double)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromDouble(pvScalarValue, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvArrayValue, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.SHORT) {
                DBR_Short dbr = (DBR_Short)fromDBR;
                if(elementCount==1) {
                    convert.fromShort(pvScalarValue, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvArrayValue, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_SHORT) {
                DBR_STS_Short dbr = (DBR_STS_Short)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromShort(pvScalarValue, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvArrayValue, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_SHORT) {
                DBR_TIME_Short dbr = (DBR_TIME_Short)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromShort(pvScalarValue, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvArrayValue, 0, dbr.getCount(), dbr.getShortValue(), 0);
                }
            } else if(requestDBRType==DBRType.INT) {
                DBR_Int dbr = (DBR_Int)fromDBR;
                if(elementCount==1) {
                    convert.fromInt(pvScalarValue, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvArrayValue, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_INT) {
                DBR_STS_Int dbr = (DBR_STS_Int)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromInt(pvScalarValue, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvArrayValue, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_INT) {
                DBR_TIME_Int dbr = (DBR_TIME_Int)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromInt(pvScalarValue, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvArrayValue, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.BYTE) {
                DBR_Byte dbr = (DBR_Byte)fromDBR;
                if(elementCount==1) {
                    convert.fromByte(pvScalarValue, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvArrayValue, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_BYTE) {
                DBR_STS_Byte dbr = (DBR_STS_Byte)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromByte(pvScalarValue, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvArrayValue, 0, dbr.getCount(), dbr.getByteValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_BYTE) {
                DBR_TIME_Byte dbr = (DBR_TIME_Byte)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromByte(pvScalarValue, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvArrayValue, 0, dbr.getCount(), dbr.getByteValue(), 0);
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
                    convert.fromByte(pvScalarValue, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvArrayValue, 0, dbr.getCount(), dbr.getByteValue(), 0);
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
                    convert.fromByte(pvScalarValue, dbr.getByteValue()[0]);
                } else {
                    convert.fromByteArray(pvArrayValue, 0, dbr.getCount(), dbr.getByteValue(), 0);
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
                    convert.fromShort(pvScalarValue, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvArrayValue, 0, dbr.getCount(), dbr.getShortValue(), 0);
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
                    convert.fromShort(pvScalarValue, dbr.getShortValue()[0]);
                } else {
                    convert.fromShortArray(pvArrayValue, 0, dbr.getCount(), dbr.getShortValue(), 0);
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
                    convert.fromInt(pvScalarValue, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvArrayValue, 0, dbr.getCount(), dbr.getIntValue(), 0);
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
                    convert.fromInt(pvScalarValue, dbr.getIntValue()[0]);
                } else {
                    convert.fromIntArray(pvArrayValue, 0, dbr.getCount(), dbr.getIntValue(), 0);
                }
            } else if(requestDBRType==DBRType.FLOAT) {
                DBR_Float dbr = (DBR_Float)fromDBR;
                if(elementCount==1) {
                    convert.fromFloat(pvScalarValue, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvArrayValue, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_FLOAT) {
                DBR_STS_Float dbr = (DBR_STS_Float)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromFloat(pvScalarValue, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvArrayValue, 0, dbr.getCount(), dbr.getFloatValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_FLOAT) {
                DBR_TIME_Float dbr = (DBR_TIME_Float)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromFloat(pvScalarValue, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvArrayValue, 0, dbr.getCount(), dbr.getFloatValue(), 0);
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
                    convert.fromFloat(pvScalarValue, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvArrayValue, 0, dbr.getCount(), dbr.getFloatValue(), 0);
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
                    convert.fromFloat(pvScalarValue, dbr.getFloatValue()[0]);
                } else {
                    convert.fromFloatArray(pvArrayValue, 0, dbr.getCount(), dbr.getFloatValue(), 0);
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
                    convert.fromDouble(pvScalarValue, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvArrayValue, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
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
                    convert.fromDouble(pvScalarValue, dbr.getDoubleValue()[0]);
                } else {
                    convert.fromDoubleArray(pvArrayValue, 0, dbr.getCount(), dbr.getDoubleValue(), 0);
                }
            } else if(requestDBRType==DBRType.STRING) {
                DBR_String dbr = (DBR_String)fromDBR;
                if(elementCount==1) {
                    convert.fromString(pvScalarValue, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray(pvArrayValue, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.STS_STRING) {
                DBR_STS_String dbr = (DBR_STS_String)fromDBR;
                status = dbr.getStatus();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvScalarValue, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray(pvArrayValue, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.TIME_STRING) {
                DBR_TIME_String dbr = (DBR_TIME_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvScalarValue, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray(pvArrayValue, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.GR_STRING) {
                DBR_GR_String dbr = (DBR_GR_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvScalarValue, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray(pvArrayValue, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else if(requestDBRType==DBRType.CTRL_STRING) {
                DBR_CTRL_String dbr = (DBR_CTRL_String)fromDBR;
                status = dbr.getStatus();
                timeStamp = dbr.getTimeStamp();
                severity = dbr.getSeverity();
                if(elementCount==1) {
                    convert.fromString(pvScalarValue, dbr.getStringValue()[0]);
                } else {
                    convert.fromStringArray(pvArrayValue, 0, dbr.getCount(), dbr.getStringValue(), 0);
                }
            } else {
                setAlarm(AlarmSeverity.invalid,
                        " unsupported DBRType " + requestDBRType.getName());
                return;
            }
        }

        PVStructure pvStructure = null;
        if(timeStamp!=null&&pvTimeStamp!=null) {
            long seconds = timeStamp.secPastEpoch();
            seconds += 7305*86400;
            pvSeconds.put(seconds);
            pvNanoSeconds.put((int)timeStamp.nsec());
        }
        if(severity!=null && pvAlarm!=null) {
            int index = severity.getValue();
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(index);
            String message = status.getName();
            setAlarm(alarmSeverity,message);
        }
        if(displayLow<displayHigh) {
            pvStructure = pvRecord.getStructureField("display");
            if(pvStructure!=null) {
                if(units!=null) {
                    PVString pvUnits = pvStructure.getStringField("units");
                    if(pvUnits!=null) {
                        pvUnits.put(units.toString());
                    }
                }
                if(precision>=0) {
                    PVInt pvResolution = pvStructure.getIntField("resolution");
                    if(pvResolution!=null) {
                        pvResolution.put(precision);
                    }
                }
                PVStructure pvLimits = pvStructure.getStructureField("limit");
                if(pvLimits!=null) {
                    PVDouble pvLow = pvLimits.getDoubleField("low");
                    PVDouble pvHigh = pvLimits.getDoubleField("high");
                    if(pvLow!=null && pvHigh!=null) {
                        pvLow.put(displayLow);
                        pvHigh.put(displayHigh);
                    }
                }
            }
        }
        if(controlLow<controlHigh) {
            pvStructure = pvRecord.getStructureField("control");
            if(pvStructure!=null) {
                pvStructure = pvStructure.getStructureField("limit");
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
        pvRecord.endGroupPut();
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
            toRecord(fromDBR);
            requestResult = RequestResult.success;
        }
        executor.execute(executorNode);
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        v3ChannelRecordRequester.createPVRecordDone(requestResult);
    }
}
