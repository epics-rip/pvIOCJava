/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import java.util.regex.Pattern;

import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelAccess;
import org.epics.ioc.ca.ChannelAccessFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmFactory;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.util.MessageType;

/**
 * Abstract Support for Channel Access Links.
 * This is nopt public since it is for use by this package.
 * @author mrk
 *
 */
abstract class AbstractLinkSupport extends AbstractSupport
implements ChannelListener,ChannelFieldGroupListener {
    /**
     * The convert implementation.
     */
    protected static final Convert convert = ConvertFactory.getConvert();
    /**
     * The dbStructure for this Support.
     */
    protected DBStructure dbStructure;
    /**
     * The pvStructure for dbStructure.
     */
    protected PVStructure pvStructure;
    /**
     * The channelRequesterName.
     */
    protected String channelRequesterName;
    /**
     * The dbRecord for dbStructure.
     */
    protected DBRecord dbRecord;
    /**
     * The recordProcess for this record.
     */
    protected RecordProcess recordProcess = null;
    /**
     * The alarmSupport, which can be null.
     */
    protected AlarmSupport alarmSupport = null;
    /**
     * The interface for getting the channel provider name.
     */
    protected PVString providerPVString = null;
    /**
     * The interface for getting the pvName.
     */
    protected PVString pvnamePVString = null;
    
    /**
     * Is the value field and enumerated structure?
     */
    protected boolean valueIsEnumerated = false;
    /**
     * If valueIsEnumerated this is the interface for the choices DBField.
     */
    protected DBField valueChoicesDBField = null;
    /**
     * If valueIsEnumerated this is the interface for the choices PVStringArray.
     */
    protected PVStringArray valueChoicesPVStringArray = null;
    /**
     * If valueIsEnumerated this is the interface for the index DBField.
     */
    protected DBField valueIndexDBField = null;
    /**
     * If valueIsEnumerated this is the interface for the index PVInt.
     */
    protected PVInt valueIndexPVInt = null;
    
    /**
     * Is alarm a property?
     */
    protected boolean alarmIsProperty = false;
    /**
     * If alarm is a property this is the PVString interface for the message.
     */
    protected PVString alarmMessagePVString = null;
    /**
     * If alarm is a property this is the PVInt interface for the severity.
     */
    protected PVInt alarmSeverityIndexPVInt = null;
    
    /**
     * The array of propertyNames.
     */
    protected String[] propertyNames = null;
    // propertyPVFields does NOT include alarm
    /**
     * Are there any propertyNames except alarm.
     */
    protected boolean gotAdditionalPropertys = false;
    /**
     * The array of PVFields for the additional propertys.
     */
    protected PVField[] propertyPVFields = null;
    /**
     * The array of DBFields for the additional propertys.
     */
    protected DBField[] propertyDBFields = null;
    
    /**
     * The channel to which this link is connected.
     */
    protected Channel channel = null;
    
    private static final Pattern whiteSpacePattern = Pattern.compile("[, ]");
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private PVString propertyNamesPVString = null;
    private StringArrayData stringArrayData = null;
    
    
    /**
     * @param supportName
     * @param dbStructure
     */
    protected AbstractLinkSupport(
        String supportName,DBStructure dbStructure)
    {
        super(supportName,dbStructure);
        this.dbStructure = dbStructure;
        dbRecord = dbStructure.getDBRecord();
    }
    
    /**
     * Called after derived class is started
     */
    protected void connect() {
        if(super.getSupportState()!=SupportState.ready) return;
        channel.connect();
    }
    
    public abstract void connectionChange(boolean isConnected);
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize()
     */
    @Override
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,null)) return;
        pvStructure = dbStructure.getPVStructure();
        while(true) {
            PVField parent = dbStructure.getPVField().getParent();
            if(parent==null) break;
            PVField pvField = parent.getSubField("value");
            if(pvField==null) break;
            PVEnumerated valuePVEnumerated = pvField.getPVEnumerated();
            if(valuePVEnumerated==null) break;
            valueIsEnumerated = true;
            valueChoicesPVStringArray = valuePVEnumerated.getChoicesField();
            valueChoicesDBField = dbRecord.findDBField(valueChoicesPVStringArray);
            valueIndexPVInt = valuePVEnumerated.getIndexField();
            valueIndexDBField = dbRecord.findDBField(valueIndexPVInt);
            stringArrayData = new StringArrayData();
            break;
        }
        channelRequesterName = pvStructure.getFullName();
        recordProcess = dbRecord.getRecordProcess();
        alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
        providerPVString = pvStructure.getStringField("providerName");
        if(providerPVString==null) return;
        pvnamePVString = pvStructure.getStringField("pvname");
        if(pvnamePVString==null) return;
        if(pvStructure.findProperty("propertyNames")!=null) {
            propertyNamesPVString = pvStructure.getStringField("propertyNames");
            if(propertyNamesPVString==null) return;
        }
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    public void start() {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        String providerName = providerPVString.get();
        String pvname = pvnamePVString.get();
        
        if(propertyNamesPVString!=null) {
            String value = propertyNamesPVString.get();
            if(value!=null) {
                propertyNames = whiteSpacePattern.split(value);
            }
        }
        if(propertyNames==null) propertyNames = new String[0];
        while(true) {
            int length = propertyNames.length;
            if(length<=0) break;
            boolean[] addPropertys = new boolean[length];
            int num = 0;
            for(int i=0; i<length; i++) {
                String propertyName = propertyNames[i];
                addPropertys[i] = false;
                PVField pvField = null;
                PVField parent = dbStructure.getPVField().getParent();
                if(parent!=null) {
                    pvField = parent.findProperty(propertyName);
                }
                if(pvField==null) {
                    pvStructure.message(
                            "propertyName " + propertyName + " does not exist",
                            MessageType.warning);
                    continue;
                }
                if(propertyName.equals("alarm")) {
                    PVField message = pvField.getSubField("message");
                    PVField severity = pvField.getSubField("severity");
                    PVEnumerated pvEnumerated = null;
                    if(severity!=null) pvEnumerated = severity.getPVEnumerated();
                    if(message==null || pvEnumerated==null) {
                        pvStructure.message("alarm is not valid structure", MessageType.warning);
                        continue;
                    }
                    alarmIsProperty = true;
                    alarmMessagePVString = (PVString)message;
                    alarmSeverityIndexPVInt = pvEnumerated.getIndexField();
                    continue;
                }
                addPropertys[i] = true;
                num++;
            }
            if(num<=0) break;
            gotAdditionalPropertys = true;
            propertyPVFields = new PVField[num];
            propertyDBFields = new DBField[num];
            int index = 0;
            for(int i=0; i<length; i++) {
                if(!addPropertys[i]) continue;
                String propertyName = propertyNames[i];
                PVField parent = dbStructure.getPVField().getParent();
                propertyPVFields[index] = parent.getSubField(propertyName);
                propertyDBFields[index] = dbRecord.findDBField(propertyPVFields[index]);
                index++;
            }
            break;
        }
        
        channel = channelAccess.createChannel(pvname,propertyNames, providerName, this);
        if(channel==null) {
            message("providerName " + providerName + " pvname " + pvname + " not found",MessageType.error);
            return;
        }
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    public void stop() {
        channel.destroy();
        channel = null;
        alarmIsProperty = false;
        alarmMessagePVString = null;
        alarmSeverityIndexPVInt = null;
        propertyNames = null;
        gotAdditionalPropertys = false;
        propertyPVFields = null;
        propertyDBFields = null;
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#uninitialize()
     */
    public void uninitialize() {
        if(super.getSupportState()==SupportState.ready) {
            stop();
        }
        valueIsEnumerated = false;
        valueChoicesDBField = null;
        valueChoicesPVStringArray = null;
        valueIndexDBField = null;
        if(super.getSupportState()==SupportState.readyForInitialize) return;
        setSupportState(SupportState.readyForInitialize);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message,MessageType messageType) {
        dbRecord.lock();
        try {
            pvStructure.message(message, messageType);
        } finally {
            dbRecord.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
     */
    public void destroy(Channel c) {
        dbRecord.lock();
        try {
            if(super.getSupportState()!=SupportState.ready) return;
        } finally {
            dbRecord.unlock();
        }
        recordProcess.stop();
        recordProcess.start();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
     */
    public void channelStateChange(Channel c, boolean isConnected) {
        if(isConnected) {
            dbRecord.lock();
            try {
                PVRecord pvRecord = channel.getPVRecord();
                if(valueIsEnumerated) {
                    PVEnumerated pvEnumerated = null;
                    String fieldName = channel.getFieldName();
                    if(fieldName!=null) {
                        PVField pvField = pvRecord.getSubField(fieldName);
                        pvEnumerated = pvField.getPVEnumerated();
                    }
                    if(pvEnumerated!=null) {
                        PVStringArray fromPVArray =pvEnumerated.getChoicesField();
                        int len = fromPVArray.getLength();
                        fromPVArray.get(0, len, stringArrayData);
                        valueChoicesPVStringArray.put(0, len, stringArrayData.data, 0);
                        valueChoicesDBField.postPut();
                    }
                }
                if(gotAdditionalPropertys) {
                    for(int i=0; i< propertyPVFields.length; i++) {
                        PVField toPVField = propertyPVFields[i];
                        DBField dbField = propertyDBFields[i];
                        PVField fromPVField = pvRecord.getSubField(toPVField.getField().getFieldName());
                        if(fromPVField!=null) {
                            PVStructure pvStructure = (PVStructure)fromPVField;
                            convert.copyStructure(pvStructure, (PVStructure)toPVField);
                            dbField.postPut();
                        }
                    }
                }
            } finally {
                dbRecord.unlock();
            }
        }
        connectionChange(isConnected);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
     */
    public void accessRightsChange(Channel channel, ChannelField channelField) {
        // nothing to do         
    }
}
