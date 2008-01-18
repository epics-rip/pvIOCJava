/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_CTRL_Enum;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;

import org.epics.ioc.ca.AbstractChannel;
import org.epics.ioc.ca.BaseChannelField;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelGet;
import org.epics.ioc.ca.ChannelGetRequester;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelMonitor;
import org.epics.ioc.ca.ChannelMonitorRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.ca.ChannelPut;
import org.epics.ioc.ca.ChannelPutGet;
import org.epics.ioc.ca.ChannelPutGetRequester;
import org.epics.ioc.ca.ChannelPutRequester;
import org.epics.ioc.create.EnumeratedFactory;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBRecordFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;

/**
 * Base class that implements V3Channel.
 * @author mrk
 *
 */
public class BaseV3Channel extends AbstractChannel
implements V3Channel,ConnectionListener,GetListener {
    
    
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static DBD dbd = DBDFactory.getMasterDBD();
    
    private V3ChannelProvider channelProvider;
    private String pvName;
    private String recordName;
    private String valueFieldName;
    private String[] propertyNames;

    private gov.aps.jca.Channel jcaChannel = null;
    private volatile boolean isReady = false;
    private boolean isConnected = false;
    private int elementCount = 0;
    private DBRType valueDBRType = null;
    private PVRecord pvRecord = null;
    private DBRecord dbRecord = null;
    private PVField valuePVField = null;
    private PVEnumerated valuePVEnumerated = null;

    
    /**
     * The constructer.
     * @param channelProvider The channel provider.
     * @param listener The channelListener.
     * @param pvName The pvName.
     * @param recordName The recordName.
     * @param valueFieldName The name of the value field.
     * @param propertys An array of desired properties.
     * @param options Options.
     */
    public BaseV3Channel(V3ChannelProvider channelProvider,ChannelListener listener,String pvName,
            String recordName,String valueFieldName,String[] propertys, String options)
    {
        super(listener,options);
        this.channelProvider = channelProvider;
        this.pvName = pvName;
        this.recordName = recordName;
        this.valueFieldName = valueFieldName;
        this.propertyNames = propertys;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getJcaChannel()
     */
    public Channel getJcaChannel() {
        return jcaChannel;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getContext()
     */
    public Context getContext() {
        return channelProvider.getContext();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getDBRecord()
     */
    public DBRecord getDBRecord() {
        return dbRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getPropertyNames()
     */
    public String[] getPropertyNames() {
        return propertyNames;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getValueDBRType()
     */
    public DBRType getValueDBRType() {
        return valueDBRType;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.caV3.V3Channel#getValueFieldName()
     */
    public String getValueFieldName() {
        return valueFieldName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.AbstractChannel#connect()
     */
     @Override
     public void connect() {
         try {
             isReady = false;
             jcaChannel = channelProvider.getContext().createChannel(pvName,this);
             isReady = true;
         } catch (Exception e) {
             message(e.getMessage(),MessageType.error);
             return;
         }
     }
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.AbstractChannel#disconnect()
      */
     @Override
     public void disconnect() {
         try {
             jcaChannel.destroy();
         } catch (CAException e) {
             message(e.getMessage(),MessageType.error);
         }
         super.disconnect();
     }                     
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.Channel#createChannelField(java.lang.String)
      */
     public ChannelField createChannelField(String name) {
         if(!isConnected) {
             message("createChannelField but not connected",MessageType.warning);
             return null;
         }
         if(name==null || name.length()<=0) return new BaseChannelField(dbRecord,pvRecord);
         PVField pvField = pvRecord.findProperty(name);
         if(pvField==null) return null;
         return new BaseChannelField(dbRecord,pvField);               
     }
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.Channel#createChannelProcess(org.epics.ioc.ca.ChannelProcessRequester)
      */
     public ChannelProcess createChannelProcess(ChannelProcessRequester channelProcessRequester)
     {
         if(!isConnected) {
             channelProcessRequester.message(
                     "createChannelProcess but not connected",MessageType.warning);
             return null;
         }
         BaseV3ChannelProcess channelProcess = new BaseV3ChannelProcess(channelProcessRequester);
         boolean success = channelProcess.init(this);
         if(success) {
             super.add(channelProcess);
             return channelProcess;
         }
         return null;
     } 
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.Channel#createChannelGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelGetRequester, boolean)
      */
     public ChannelGet createChannelGet(ChannelFieldGroup channelFieldGroup,
             ChannelGetRequester channelGetRequester, boolean process)
     {
         if(!isConnected) {
             channelGetRequester.message(
                     "createChannelGet but not connected",MessageType.warning);
             return null;
         }
         BaseV3ChannelGet channelGet = new BaseV3ChannelGet(channelFieldGroup,channelGetRequester,process);
         boolean success = channelGet.init(this);
         if(success) {
             super.add(channelGet);
             return channelGet;
         }
         return null;
     }        
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.Channel#createChannelPut(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutRequester, boolean)
      */
     public ChannelPut createChannelPut(ChannelFieldGroup channelFieldGroup,
             ChannelPutRequester channelPutRequester, boolean process)
     {
         if(!isConnected) {
             channelPutRequester.message(
                     "createChannelPut but not connected",MessageType.warning);
             return null;
         }
         BaseV3ChannelPut channelPut = new BaseV3ChannelPut(channelFieldGroup,channelPutRequester,process);
         boolean success = channelPut.init(this);
         if(success) {
             super.add(channelPut);
             return channelPut;
         }
         return null;
     }        
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.Channel#createChannelPutGet(org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelFieldGroup, org.epics.ioc.ca.ChannelPutGetRequester, boolean)
      */
     public ChannelPutGet createChannelPutGet(ChannelFieldGroup putFieldGroup,
             ChannelFieldGroup getFieldGroup, ChannelPutGetRequester channelPutGetRequester,
             boolean process)
     {
         if(!isConnected) {
             channelPutGetRequester.message(
                     "createChannelPutGet but not connected",MessageType.warning);
             return null;
         }
         BaseV3ChannelPutGet channelPutGet = new BaseV3ChannelPutGet(
                 putFieldGroup,getFieldGroup,channelPutGetRequester,process);
         boolean success = channelPutGet.init(this);
         if(success) {
             super.add(channelPutGet);
             return channelPutGet;
         }
         return null;
     }
     /* (non-Javadoc)
      * @see org.epics.ioc.ca.Channel#createOnChange(org.epics.ioc.ca.ChannelMonitorNotifyRequester, boolean)
      */
     public ChannelMonitor createChannelMonitor(ChannelMonitorRequester channelMonitorRequester)
     {
         if(!isConnected) {
             message(
                     "createChannelMonitor but not connected",MessageType.warning);    
             return null;
         }
         BaseV3ChannelMonitor channelMonitor = new BaseV3ChannelMonitor(channelMonitorRequester);
         boolean success = channelMonitor.init(this);
         if(success) {
             super.add(channelMonitor);
             return channelMonitor;
         }
         return null;
     }
     /* (non-Javadoc)
      * @see gov.aps.jca.event.ConnectionListener#connectionChanged(gov.aps.jca.event.ConnectionEvent)
      */
     public void connectionChanged(ConnectionEvent arg0) {
         while(!isReady) {
             try {
                 Thread.sleep(1);
             } catch (InterruptedException e) {

             }
         }
         isConnected = arg0.isConnected();
         if(isConnected) {
             elementCount = jcaChannel.getElementCount();
             valueDBRType = jcaChannel.getFieldType();
             createPVRecord();
             if(valueDBRType==DBRType.ENUM) {
                 try {
                     jcaChannel.get(DBRType.CTRL_ENUM,elementCount,this);
                 } catch (Exception e) {
                     message(e.getMessage(),MessageType.error);
                 }
             } else {
                 super.connect();
             }
         } else {
             super.disconnect();
             pvRecord = null;
         }
     }
     /* (non-Javadoc)
      * @see gov.aps.jca.event.GetListener#getCompleted(gov.aps.jca.event.GetEvent)
      */
     public void getCompleted(GetEvent arg0) {
         DBR_CTRL_Enum ctrlEnum = (DBR_CTRL_Enum)arg0.getDBR();;
         String[] labels = ctrlEnum.getLabels();
         PVStringArray pvChoices = valuePVEnumerated.getChoicesField();
         pvChoices.put(0, labels.length, labels, 0);
         DBField dbField = dbRecord.findDBField(pvChoices);
         dbField.postPut();
         PVInt pvIndex = valuePVEnumerated.getIndexField();
         pvIndex.put(ctrlEnum.getEnumValue()[0]);
         dbField = dbRecord.findDBField(pvIndex);
         dbField.postPut();
         super.connect();
     }

     private void createPVRecord() {
         Type type = null;
         if(valueDBRType.isBYTE()) {
             type = Type.pvByte;
         } else if(valueDBRType.isSHORT()) {
             type= Type.pvShort;
         } else if(valueDBRType.isINT()) {
             type = Type.pvInt;
         } else if(valueDBRType.isFLOAT()) {
             type = Type.pvFloat;
         } else if(valueDBRType.isDOUBLE()) {
             type = Type.pvDouble;
         } else if(valueDBRType.isSTRING()) {
             type = Type.pvString;
         } else if(valueDBRType.isENUM()) {
             type = Type.pvStructure;
         }
         Type elementType = null;
         Field valueField = null;
         if(elementCount<2) {
             if(type==Type.pvStructure) {
                 DBDStructure dbdEnumerated = dbd.getStructure("enumerated");
                 Field[] valueFields = dbdEnumerated.getFields();
                 valueField = fieldCreate.createStructure(valueFieldName, "value", valueFields);
             } else {
                 valueField = fieldCreate.createField(valueFieldName, type);
             }
         } else {
             elementType = type;
             type = Type.pvArray;
             valueField = fieldCreate.createArray(valueFieldName, elementType);
         }
         Field[] fields = new Field[propertyNames.length + 1];
         fields[0] = valueField;
         int index = 1;
         for(String property : propertyNames) {
             DBDStructure dbdStructure = dbd.getStructure(property);
             if(dbdStructure==null) dbdStructure = dbd.getStructure("null");
             Field[] propertyFields = dbdStructure.getFields();
             fields[index++] = fieldCreate.createStructure(property, property, propertyFields);
         }
         Structure structure = fieldCreate.createStructure("caV3", "caV3", fields);
         pvRecord = pvDataCreate.createPVRecord(recordName, structure);
         dbRecord = DBRecordFactory.create(pvRecord);
         valuePVField = pvRecord.getPVFields()[0];
         if(valueDBRType.isENUM()) {
             DBField dbField = dbRecord.findDBField(valuePVField);
             EnumeratedFactory.create(dbField);
             valuePVEnumerated = valuePVField.getPVEnumerated();
         }
         super.SetPVRecord(pvRecord,valueFieldName);
     }
}