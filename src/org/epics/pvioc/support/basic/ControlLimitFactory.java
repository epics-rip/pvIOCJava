/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.*;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.alarm.AlarmSupport;
import org.epics.pvioc.support.alarm.AlarmSupportFactory;

/**
 * Factory for control limit
 * @author mrk
 *
 */
public class ControlLimitFactory { 
    
    public static Support create(PVRecordField pvRecordField)
    {
        ControlLimitImpl controlLimitImpl = new ControlLimitImpl(pvRecordField);
        if(controlLimitImpl.init()) return controlLimitImpl;
        return null;
    }
    
    static private class ControlLimitImpl extends AbstractSupport
    {    
        private static String supportName = "org.epics.pvioc.controlLimit";
        private PVRecordField pvRecordField;
        private AlarmSupport alarmSupport = null;
        private PVDouble pvValue;
        private PVDouble pvLimitLow;
        private PVDouble pvLimitHigh;
        
        ControlLimitImpl(PVRecordField pvRecordField) {
            super(supportName,pvRecordField);
            this.pvRecordField = pvRecordField;
        }
        
        boolean init()
        {
            PVField pvField = pvRecordField.getPVField();
            if(!pvField.getFieldName().equals("value")) {
                pvRecordField.message("field is not named value", MessageType.error);
                return false;
            }
            if(pvField.getField().getType()!=Type.scalar) {
                pvRecordField.message("field is not scalar", MessageType.error);
                return false;
            }
            Scalar scalar = (Scalar)pvField.getField();
            if(!(scalar.getScalarType()==ScalarType.pvDouble)) {
                pvRecordField.message("field is not a double", MessageType.error);
                return false;
            }
            pvValue = (PVDouble)pvField;
            PVStructure pvParent = pvRecordField.getParent().getPVStructure();
            pvLimitLow = pvParent.getSubField(PVDouble.class, "control.limitLow");
            if(pvLimitLow==null) {
                pvRecordField.message("control.limitLow is not a double", MessageType.error);
                return false;
            }
            pvLimitHigh = pvParent.getSubField(PVDouble.class, "control.limitHigh");
            if(pvLimitHigh==null) {
                pvRecordField.message("control.limitHigh is not a double", MessageType.error);
                return false;
            }
            return true;
        }
        
        private void raiseAlarm(boolean isHigh) {
            if(alarmSupport==null) {
                alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecordField);
                if(alarmSupport==null) {
                    pvRecordField.message("ControlLimit: no alarmSupport", MessageType.warning);
                }
            }
            String message = null;
            if(isHigh) {
                message = "ControlLimit: attempt to exceed high limit";
            } else {
                message = "ControlLimit: attempt to exceed low limit";
            }
            if(alarmSupport!=null) {
                alarmSupport.setAlarm(message, AlarmSeverity.MINOR,AlarmStatus.RECORD);
            } else {
                pvRecordField.message(message, MessageType.warning);
            }
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.support.AbstractSupport#process(org.epics.pvioc.support.SupportProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            double value = pvValue.get();
            double lowValue = pvLimitLow.get();
            double highValue = pvLimitHigh.get();
            if(lowValue<highValue) {
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
            }
            if(value!=pvValue.get()) pvValue.put(value);
            super.process(supportProcessRequester);
        }
    }
}
