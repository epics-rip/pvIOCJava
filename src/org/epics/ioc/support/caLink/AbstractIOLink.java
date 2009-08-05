/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import java.util.regex.Pattern;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.Alarm;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;


/**
 * Abstract support for channel access link that transfers data.
 * @author mrk
 *
 */
abstract class AbstractIOLink extends AbstractLink {
    /**
     * The convert implementation.
     */
    protected static final Convert convert = ConvertFactory.getConvert();
    protected PVStructure pvRequest = null;
    /**
     * The value field.
     */
    protected PVField valuePVField = null;
    /**
     * Is the value field an enumerated structure?
     */
    protected boolean valueIsEnumerated = false;
    /**
     * If valueIsEnumerated this is the interface for the index PVInt.
     */
    protected PVInt valueIndexPV = null;
    /**
     * Is alarm a property?
     */
    protected boolean alarmIsProperty = false;
    /**
     * The array of propertyNames. AbstractIOLink handles this.
     * It does not include alarm
     */
    protected String[] propertyNames = null;
    /**
     * The array of PVFields for the additional propertys.
     */
    protected PVField[] propertyPVFields = null;
    private PVString propertyNamesPV = null;
    private static final Pattern commaPattern = Pattern.compile("[,]");
    /**
     * Constructor.
     * @param supportName The support name.
     * @param pvField The field which is supported.
     */
    public AbstractIOLink(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    
    public void initialize(LocateSupport recordSupport) {
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
            valueIndexPV = valuePVEnumerated.getIndex();
        }
        if(pvStructure.getSubField("propertyNames")==null) {
            return; // if no properties then done
        }
        propertyNamesPV = pvStructure.getStringField("propertyNames");
        if(propertyNamesPV==null) {
            super.uninitialize();
            return;
        }
        String value = propertyNamesPV.get();
        if(value!=null) {
            propertyNames = commaPattern.split(value);
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
                    AlarmSupport alarmSupport = AlarmSupportFactory.findAlarmSupport(pvStructure,recordSupport);
                    if(alarmSupport==null || alarmSupport.getPVField()!=pvField) {
                        super.message("illegal alarm field", MessageType.error);
                        super.uninitialize();
                        return;
                    }
                    Alarm alarm = alarmSupport.getAlarm();
                    if(alarm==null) {
                        pvStructure.message("alarm is not valid structure", MessageType.warning);
                        continue;
                    }
                    alarmIsProperty = true;
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
            propertyPVFields = new PVField[num];
            String[] newPropertyNames = new String[num];
            int index = 0;
            for(int i=0; i<length; i++) {
                if(!addPropertys[i]) continue;
                String propertyName = propertyNames[i];
                newPropertyNames[index] = propertyName;
                PVStructure parent = pvStructure.getParent();
                propertyPVFields[index] = parent.getSubField(propertyName);
                index++;
            }
            propertyNames = newPropertyNames;
            break;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        String pvname = pvnamePV.get();
        String provider = providerPV.get();
        String fieldname = "value";
        if(provider.equals("caV3")) fieldname ="VAL";
        int index = pvname.indexOf('.');
        if(index>0) {
            fieldname = pvname.substring(index-1);
            pvname = pvname.substring(0, index);
        }
        pvRequest = pvDataCreate.createPVStructure(null, pvname, new Field[0]);
        PVString pvField = (PVString)pvDataCreate.createPVScalar(pvRequest, "value", ScalarType.pvString);
        pvField.put(fieldname);
        pvRequest.appendPVField(pvField);
        if(alarmIsProperty) {
            pvField = (PVString)pvDataCreate.createPVScalar(pvRequest, "alarm", ScalarType.pvString);
            pvField.put("alarm");
            pvRequest.appendPVField(pvField);
        }
        if(propertyNames!=null && propertyNames.length>0) {
            for(int i=0; i<propertyNames.length; i++) {
                String name = propertyNames[i];
                pvField = (PVString)pvDataCreate.createPVScalar(pvRequest, name, ScalarType.pvString);
                pvField.put(name);
                pvRequest.appendPVField(pvField);
            }
        }
        String providerName = providerPV.get();
        pvname = pvnamePV.get();
        if(providerName.equals("caV3")) {
            if(alarmIsProperty || (propertyNames!=null && propertyNames.length>0)) {
                boolean addComma = false;
                int indexPeriod = pvname.indexOf('.');
                if(indexPeriod<1) pvname += ".value";
                pvname += "{";
                if(alarmIsProperty) {
                    pvname += "alarm";
                    addComma = true;
                }
                if(propertyNames!=null && propertyNames.length>0) {
                    for(int i=0; i<propertyNames.length; i++) {
                        if(addComma) pvname += ",";
                        pvname += propertyNames[i];
                        addComma = true;
                    }
                    pvname += "}";
                }
                pvnamePV.put(pvname);
            }
        }
        super.start(afterStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#uninitialize()
     */
    public void uninitialize() {
        alarmIsProperty = false;
        propertyNames = null;
        propertyPVFields = null;
        propertyPVFields = null;
        valueIndexPV = null;
        valueIsEnumerated = false;
        valuePVField = null;
        super.stop();
    }
}
