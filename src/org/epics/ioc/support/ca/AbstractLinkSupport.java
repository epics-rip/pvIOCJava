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
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.ioc.support.basic.GenericBase;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.Alarm;
import org.epics.pvData.property.AlarmFactory;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Type;

/**
 * Abstract Support for Channel Access Links.
 * This is nopt public since it is for use by this package.
 * @author mrk
 *
 */
abstract class AbstractLinkSupport extends GenericBase
implements ChannelListener,ChannelFieldGroupListener {
    /**
     * The convert implementation.
     */
    protected static final Convert convert = ConvertFactory.getConvert();
    /**
     * A pvProperty for use by AbstractLinkSupport and derived classes.
     */
    protected static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    /**
     * The channelRequesterName.
     */
    protected String channelRequesterName;
    /**
     * The pvStructure that this link supports.
     */
    protected PVStructure pvStructure;
    /**
     * The pvRecord for pvStructure.
     */
    protected PVRecord pvRecord;
    /**
     * The recordProcess for this record.
     */
    protected RecordProcess recordProcess = null;
    /**
     * The alarmSupport, which can be null.
     */
    protected AlarmSupport alarmSupport = null;
    /**
     * The name of the channel provider.
     */
    protected PVString providerPV = null;
    /**
     * The interface for getting the pvName.
     */
    protected PVString pvnamePV = null;
    /**
     * Is the value field an enumerated structure?
     */
    protected boolean valueIsEnumerated = false;
    /**
     * If valueIsEnumerated this is the interface for the choices PVStringArray.
     */
    protected PVStringArray valueChoicesPV = null;
    /**
     * If valueIsEnumerated this is the interface for the index PVInt.
     */
    protected PVInt valueIndexPV = null;
    /**
     * Is alarm a property?
     */
    protected boolean alarmIsProperty = false;
    /**
     * If alarm is a property this is the PVString interface for the message.
     */
    protected PVString alarmMessagePV = null;
    /**
     * If alarm is a property this is the PVInt interface for the severity.
     */
    protected PVInt alarmSeverityIndexPV = null;
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
     * The channel to which this link is connected.
     */
    protected Channel channel = null;
   
    private static final Pattern whiteSpacePattern = Pattern.compile("[, ]");
    private static final ChannelAccess channelAccess = ChannelAccessFactory.getChannelAccess();
    private PVString propertyNamesPV = null;
    private StringArrayData stringArrayData = null;
    
    
    /**
     * @param supportName
     * @param pvStructure
     */
    protected AbstractLinkSupport(
        String supportName,PVStructure pvStructure)
    {
        super(supportName,pvStructure);
        this.pvStructure = pvStructure;
        pvRecord = pvStructure.getPVRecord();
        channelRequesterName = pvStructure.getFullName();
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
     * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        if(!super.checkSupportState(SupportState.readyForInitialize,null)) return;
        while(true) {
            PVStructure parent = pvStructure.getParent();
            if(parent==null) break;
            PVField pvField = parent.getSubField("value");
            if(pvField==null) break;
            Enumerated valuePVEnumerated = EnumeratedFactory.getEnumerated(pvField);
            if(valuePVEnumerated==null) break;
            valueIsEnumerated = true;
            valueChoicesPV = valuePVEnumerated.getChoices();
            valueIndexPV = valuePVEnumerated.getIndex();
            stringArrayData = new StringArrayData();
            break;
        }
        recordProcess = recordSupport.getRecordProcess();
        alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
        providerPV = pvStructure.getStringField("providerName");
        if(providerPV==null) return;
        pvnamePV = pvStructure.getStringField("pvname");
        if(pvnamePV==null) return;
        if(pvProperty.findProperty(pvStructure,"propertyNames")!=null) {
            propertyNamesPV = pvStructure.getStringField("propertyNames");
            if(propertyNamesPV==null) return;
        }
        super.initialize(recordSupport);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    public void start() {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        String providerName = providerPV.get();
        String pvname = pvnamePV.get();
        
        if(propertyNamesPV!=null) {
            String value = propertyNamesPV.get();
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
                if(propertyName.equals("alarm")) {
                    PVField pvField = pvStructure.getSubField("alarm");
                    if(pvField==null) {
                        pvStructure.message("does not have fieldalarm", MessageType.warning);
                        continue;
                    }
                    Alarm alarm = AlarmFactory.getAlarm(pvField);
                    if(alarm==null) {
                        pvStructure.message("alarm is not valid structure", MessageType.warning);
                        continue;
                    }
                    alarmIsProperty = true;
                    alarmMessagePV = alarm.getAlarmMessage();
                    alarmSeverityIndexPV = alarm.getAlarmSeverityIndex();
                    continue;
                } else {
                    PVField pvField = null;
                    PVStructure parent = pvStructure.getParent();
                    if(parent!=null) {
                        pvField = pvProperty.findProperty(parent,propertyName);
                    }
                    if(pvField==null) {
                        pvStructure.message(
                                "propertyName " + propertyName + " does not exist",
                                MessageType.warning);
                        continue;
                    }
                    if(pvField.getField().getType()!=Type.structure) {
                        pvStructure.message(
                                "propertyName " + propertyName + " is not a structure",
                                MessageType.warning);
                        continue;
                    }
                }
                addPropertys[i] = true;
                num++;
            }
            if(num<=0) break;
            gotAdditionalPropertys = true;
            propertyPVFields = new PVField[num];
            int index = 0;
            for(int i=0; i<length; i++) {
                if(!addPropertys[i]) continue;
                String propertyName = propertyNames[i];
                PVStructure parent = pvStructure.getParent();
                propertyPVFields[index] = parent.getSubField(propertyName);
                index++;
            }
            break;
        }
        channel = channelAccess.createChannel(pvname,propertyNames, providerName, this);
        if(channel==null) {
            message("providerName " + providerName + " pvname " + pvname + " not found",MessageType.error);
            return;
        }
        super.start();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    public void stop() {
        channel.destroy();
        channel = null;
        alarmIsProperty = false;
        alarmMessagePV = null;
        alarmSeverityIndexPV = null;
        propertyNames = null;
        gotAdditionalPropertys = false;
        propertyPVFields = null;
        propertyPVFields = null;
        super.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message,MessageType messageType) {
        pvRecord.lock();
        try {
            pvStructure.message(message, messageType);
        } finally {
            pvRecord.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
     */
    public void destroy(Channel c) {
        pvRecord.lock();
        try {
            if(super.getSupportState()!=SupportState.ready) return;
        } finally {
            pvRecord.unlock();
        }
        recordProcess.stop();
        recordProcess.start();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel, boolean)
     */
    public void channelStateChange(Channel c, boolean isConnected) {
        if(isConnected) {
            pvRecord.lock();
            try {
                PVRecord pvRecord = channel.getPVRecord();
                if(valueIsEnumerated) {
                    Enumerated enumerated = null;
                    String fieldName = channel.getFieldName();
                    if(fieldName!=null) {
                        PVField pvField = pvRecord.getSubField(fieldName);
                        enumerated = EnumeratedFactory.getEnumerated(pvField);
                    }
                    if(enumerated!=null) {
                        PVStringArray fromPVArray =enumerated.getChoices();
                        int len = fromPVArray.getLength();
                        fromPVArray.get(0, len, stringArrayData);
                        valueChoicesPV.put(0, len, stringArrayData.data, 0);
                    }
                }
                if(gotAdditionalPropertys) {
                    for(int i=0; i< propertyPVFields.length; i++) {
                        PVField toPVField = propertyPVFields[i];
                        PVField fromPVField = pvRecord.getSubField(toPVField.getField().getFieldName());
                        if(fromPVField!=null) {
                            PVStructure pvStructure = (PVStructure)fromPVField;
                            convert.copyStructure(pvStructure, (PVStructure)toPVField);
                        }
                    }
                }
            } finally {
                pvRecord.unlock();
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
