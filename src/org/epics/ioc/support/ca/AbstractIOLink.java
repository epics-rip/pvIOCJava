/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import java.util.regex.Pattern;

import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportState;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.Alarm;
import org.epics.pvData.property.AlarmFactory;
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
 * Abstract support for channel access link that transfers data.
 * @author mrk
 *
 */
abstract class AbstractIOLink extends AbstractLink  {
    /**
     * The convert implementation.
     */
    protected static final Convert convert = ConvertFactory.getConvert();
    /**
     * The value field.
     */
    protected PVField valuePVField = null;
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
     * Are there any propertyNames except alarm.
     */
    protected boolean gotAdditionalPropertys = false;
    /**
     * The array of PVFields for the additional propertys.
     */
    protected PVField[] propertyPVFields = null;
    
    private PVString propertyNamesPV = null;
    private StringArrayData stringArrayData = null;
    private static final Pattern whiteSpacePattern = Pattern.compile("[, ]");
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvField The field which is supported.
     */
    public AbstractIOLink(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    
    public void initialize(RecordSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        PVStructure pvParent = super.pvStructure;
        while(pvParent!=null) {
            valuePVField = pvParent.getSubField("value");
            if(valuePVField!=null) break;
            pvParent = pvParent.getParent();
        }
        if(valuePVField==null) {
            super.message("value field not found", MessageType.error);
            super.uninitialize();
            return;
        }
        Enumerated valuePVEnumerated = EnumeratedFactory.getEnumerated(valuePVField);
        if(valuePVEnumerated!=null) {
            valueIsEnumerated = true;
            valueChoicesPV = valuePVEnumerated.getChoices();
            valueIndexPV = valuePVEnumerated.getIndex();
            stringArrayData = new StringArrayData();
        }
        recordProcess = recordSupport.getRecordProcess();
        if(pvStructure.getSubField("propertyNames")!=null) {
            propertyNamesPV = pvStructure.getStringField("propertyNames");
            if(propertyNamesPV==null) {
                super.uninitialize();
                return;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    public void start() {
        super.start();
        if(!super.checkSupportState(SupportState.ready,null)) return;
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
                        pvStructure.message("does not have field alarm", MessageType.warning);
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
                        pvField =parent.getSubField(propertyName);
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
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    public void stop() {
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
     * @see org.epics.ioc.support.ca.AbstractLink#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
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
                        valueChoicesPV.postPut();
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
    }
}
