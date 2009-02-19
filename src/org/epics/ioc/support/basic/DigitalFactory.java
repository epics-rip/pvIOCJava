/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.factory.PVDataFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVAuxInfo;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Support for a digital input or output record.
 * @author mrk
 *
 */
public class DigitalFactory {
    /**
     * Create the support.
     * @param pvStructure The field for which to create support.
     * @return The support instance.
     */
    public static Support create(PVStructure pvStructure) {
        PVAuxInfo pvAuxInfo = pvStructure.getPVAuxInfo();
        PVScalar pvScalar = pvAuxInfo.getInfo("supportFactory");
        if(pvScalar==null) {
            pvStructure.message("no pvAuxInfo with name supportFactory. Why??", MessageType.error);
            return null;
        }
        if(pvScalar.getScalar().getScalarType()!=ScalarType.pvString) {
            pvStructure.message("pvAuxInfo for support is not a string. Why??", MessageType.error);
            return null;
        }
        String supportName = ((PVString)pvScalar).get();
        Support support = null;
        if(supportName.equals(digitalInputName)) {
            support = new DigitalInput(supportName,pvStructure);
        } else if(supportName.equals(digitalOutputName)) {
            support =  new DigitalOutput(supportName,pvStructure);
        }
        return support;
    }
    
    private static final String digitalInputName = "digitalInputFactory";
    private static final String digitalOutputName = "digitalOutputFcatory";
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private static PVDatabase pvDatabaseMaster = PVDatabaseFactory.getMaster();
    private static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private static Convert convert = ConvertFactory.getConvert();
    

    
    static private abstract class DigitalBase extends AbstractSupport
    {
        
        protected String supportName;
        protected PVStructure pvStates;
        
        protected PVStringArray pvValueChoices = null;
        protected PVInt pvValueIndex = null;
        
        protected PVInt pvRegisterValue = null;
        
        protected int[] values = null;
        
        protected PVStructure pvValueAlarmStateAlarm = null;
        
        protected DigitalBase(String supportName,PVStructure pvStructure) {
            super(supportName,pvStructure);
            this.pvStates = pvStructure;
            this.supportName = supportName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
         */
        public void initialize(RecordSupport recordSupport) {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            PVStructure parentPVField = pvStates.getParent().getParent();
            PVField pvField = pvProperty.findProperty(parentPVField, "value");
            if(pvField==null) {
                super.message("parent does not have a value field", MessageType.error);
                return;
            }
            Enumerated enumerated = EnumeratedFactory.getEnumerated(pvField);
            if(pvField==null) {
                pvField.message("this is not an enumerated field", MessageType.error);
                return;
            }
            pvValueIndex = enumerated.getIndex();
            pvValueChoices = enumerated.getChoices();
            PVStructure pvValueAlarm = parentPVField.getStructureField("valueAlarm");
            if(pvValueAlarm==null) {
                super.message("valueAlarm does not exist", MessageType.error);
                return;
            }
            pvValueAlarmStateAlarm = pvValueAlarm.getStructureField("changeStateAlarm");
            if(pvValueAlarmStateAlarm==null) {
                pvValueAlarm.message("valueAlarm does not have a stateAlarm field. Why???", MessageType.error);
                return;
            }
            parentPVField = pvStates.getParent();
            pvRegisterValue = parentPVField.getIntField("value");
            if(pvRegisterValue==null) {
                parentPVField.message("value field of type int not found", MessageType.error);
                return;
            }
            if(!initFields()) {
                super.message("DigitalFactory: initialize failed", MessageType.error);
                return;
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            setSupportState(SupportState.readyForInitialize);
        }
        
        private boolean initFields() {
            // from the state fields this code 
            // 1) sets value.choices
            // 2) creates the valueAlarm.stateAlarm array
            PVField[] pvStatesFields = pvStates.getPVFields();
            int nstates = pvStatesFields.length;
            if(nstates<1) return false;
            String[] names = new String[nstates];
            values = new int[nstates];
            PVStructure pvEnumeratedAlarmState = pvDatabaseMaster.findStructure("enumeratedAlarmState");
            Field[] pvValueAlarmStateAlarmFields = pvEnumeratedAlarmState.getStructure().getFields();
            for(int indState=0; indState<nstates; indState++) {
                PVField pvField = pvStatesFields[indState];
                if(pvField.getField().getType()!=Type.structure) {
                    pvField.message("not a structure", MessageType.error);
                    return false;
                }
                PVStructure pvState = (PVStructure)pvField;
                PVString pvName = pvState.getStringField("name");
                if(pvName==null) {
                    pvField.message("did not find name", MessageType.error);
                    return false;
                }
                names[indState] = pvName.get();
                PVInt pvValue = pvState.getIntField("value");
                if(pvValue==null) {
                    pvField.message("did not find value", MessageType.error);
                    return false;
                }
                values[indState] = pvValue.get();
                PVString pvMessage = pvState.getStringField("message");
                if(pvMessage==null) {
                    pvField.message("did not find message", MessageType.error);
                    return false;
                }
                PVStructure pvStructure = pvState.getStructureField("severity");
                if(pvStructure==null) {
                    pvField.message("did not find severity", MessageType.error);
                    return false;
                }
                Enumerated enumerated = AlarmSeverity.getAlarmSeverity(pvStructure);
                if(enumerated==null) {
                    super.message(
                            "states index " + indState + " field name is not an alarmSeverity",
                            MessageType.error);
                    return false;
                }
                PVStructure pvNew = pvDataCreate.createPVStructure(
                    pvValueAlarmStateAlarm,
                    String.valueOf(indState),
                    pvStructure.getStructure().getFields());
                convert.copyStructure(pvStructure,pvNew);
                pvValueAlarmStateAlarm.appendPVField(pvNew);
            }          
            pvValueChoices.put(0, nstates, names, 0);
            return true;
        } 
    }
    
    static private class DigitalInput extends DigitalBase {
        private int prevRegisterValue = 0;
        
        private DigitalInput(String supportName,PVStructure pvStructure) {
            super(supportName,pvStructure);
        }
        
        public void process(SupportProcessRequester supportProcessRequester)
        {
            int newValue = pvRegisterValue.get();
            if(newValue!=prevRegisterValue) {
                prevRegisterValue = newValue;
                for(int i=0; i< values.length; i++) {
                    if(values[i]==newValue) {
                        pvValueIndex.put(i);
                        pvValueIndex.postPut();
                        break;
                    }
                }
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
    
    static private class DigitalOutput extends DigitalBase {
        private int prevValueIndex = 0;
        private DigitalOutput(String supportName,PVStructure pvStructure) {
            super(supportName,pvStructure);
        }
        
        public void process(SupportProcessRequester supportProcessRequester)
        {
            int value = pvValueIndex.get();
            if(prevValueIndex!=value) {
                prevValueIndex = value;
                if(value<0 || value>=values.length) {
                    pvStates.message("Illegal value", MessageType.warning);
                } else {
                    pvRegisterValue.put(value);
                }
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
    }
}
