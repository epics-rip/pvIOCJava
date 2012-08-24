/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvdata.factory.BasePVByte;
import org.epics.pvdata.factory.BasePVDouble;
import org.epics.pvdata.factory.BasePVFloat;
import org.epics.pvdata.factory.BasePVInt;
import org.epics.pvdata.factory.BasePVLong;
import org.epics.pvdata.factory.BasePVShort;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVByte;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.support.alarm.AlarmSupport;
import org.epics.pvioc.support.alarm.AlarmSupportFactory;

/**
 * Factory for an enumerated structure.
 * @author mrk
 *
 */
public class ControlLimitFactory { 
    /**
     * replace the pvField implementation with an implementation that enforces control limits.
     * @param pvRecordField
     */
    public static void replacePVField(PVRecordField pvRecordField) {
    	PVField pvField = pvRecordField.getPVField();
        if(pvField.getField().getType()!=Type.scalar) {
            pvRecordField.message("replacePVField field is not scalar", MessageType.error);
            return;
        }
        Scalar scalar = (Scalar)pvField.getField();
        if(!scalar.getScalarType().isNumeric()) {
            pvRecordField.message("replacePVField field is not a numeric scalar", MessageType.error);
            return;
        }
        PVStructure pvParent = pvRecordField.getParent().getPVStructure();
        PVField pvLow = pvParent.getSubField("control.limitLow");
        PVField pvHigh = pvParent.getSubField("control.limitHigh");
        if(pvLow==null || pvHigh==null) {
            pvParent.message("replacePVField missing or invalid control structure", MessageType.error);
            return;
        }
        if(pvLow.getField().getType()!=Type.scalar) {
            pvLow.message("is not a scalar", MessageType.error);
            return;
        }
        if(pvHigh.getField().getType()!=Type.scalar) {
            pvLow.message("is not a scalar", MessageType.error);
            return;
        }
        new ControlLimitImpl(pvRecordField,(PVScalar)pvLow,(PVScalar)pvHigh);
    }

    private static class ControlLimitImpl {
        
        private static Convert convert = ConvertFactory.getConvert();
        private PVRecordField pvRecordField;
        private PVScalar valuePVField = null;
        private AlarmSupport alarmSupport = null;
        /** Constructor.
         * @param valuePVField The PVField interface for the value field.
         * @param lowPVField The PVField interface for the low limit.
         * @param highPVField The PVField interface for the high limit.
         */
        public ControlLimitImpl(PVRecordField pvRecordField, PVScalar lowPVField, PVScalar highPVField) {
        	this.pvRecordField = pvRecordField;
            valuePVField = (PVScalar)pvRecordField.getPVField();
            PVStructure parentPVField = valuePVField.getParent();
            PVScalar newPVField = null;
            Scalar valueField = valuePVField.getScalar();
            ScalarType type = valueField.getScalarType();
            switch(type) {
            case pvByte:
                newPVField = new ByteValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvShort:
                newPVField = new ShortValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvInt:
                newPVField = new IntValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvLong:
                newPVField = new LongValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvFloat:
                newPVField = new FloatValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvDouble:
                newPVField = new DoubleValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            default:
                throw new IllegalStateException("valuePVfield does not have a supported type");
            }
            pvRecordField.replacePVField(newPVField);
            double oldValue = convert.toDouble(valuePVField);
            convert.fromDouble(newPVField, oldValue);
        }
        
        private void raiseAlarm(boolean isHigh) {
            if(alarmSupport==null) {
                alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecordField);
                if(alarmSupport==null) {
                    valuePVField.message("ControlLimit: no alarmSupport", MessageType.warning);
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
                valuePVField.message(message, MessageType.warning);
            }
        }
        
        private class ByteValue extends BasePVByte implements PVByte {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private ByteValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVByte#get()
             */
            @Override
            public byte get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVByte#put(byte)
             */
            @Override
            public void put(byte value) {
                byte lowValue = convert.toByte(lowPVField);
                byte highValue = convert.toByte(highPVField);
                if(lowValue<highValue) {
                    if(value<lowValue) {
                        value = lowValue;
                        raiseAlarm(false);
                    } else if(value>highValue) {
                        value = highValue;
                        raiseAlarm(true);
                    }
                }
                this.value = value;
                super.postPut();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVByte#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) { // implemented to satisfy FindBugs
                return super.equals(obj);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVByte#hashCode()
             */
            @Override
            public int hashCode() { // implemented to satisfy FindBugs
                return super.hashCode();
            }
        }
        
        private class ShortValue extends BasePVShort implements PVShort {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private ShortValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVShort#get()
             */
            @Override
            public short get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVShort#put(short)
             */
            @Override
            public void put(short value) {
                short lowValue = convert.toShort(lowPVField);
                short highValue = convert.toShort(highPVField);
                if(lowValue<highValue) {
                    if(value<lowValue) {
                        value = lowValue;
                        raiseAlarm(false);
                    } else if(value>highValue) {
                        value = highValue;
                        raiseAlarm(true);
                    }
                }
                this.value = value;
                super.postPut();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVShort#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) { // implemented to satisfy FindBugs
                return super.equals(obj);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVShort#hashCode()
             */
            @Override
            public int hashCode() { // implemented to satisfy FindBugs
                return super.hashCode();
            }
        }
        private class IntValue extends BasePVInt implements PVInt {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private IntValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVInt#get()
             */
            @Override
            public int get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVInt#put(int)
             */
            @Override
            public void put(int value) {
                int lowValue = convert.toInt(lowPVField);
                int highValue = convert.toInt(highPVField);
                if(lowValue<highValue) {
                    if(value<lowValue) {
                        value = lowValue;
                        raiseAlarm(false);
                    } else if(value>highValue) {
                        value = highValue;
                        raiseAlarm(true);
                    }
                }
                this.value = value;
                super.postPut();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVInt#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) { // implemented to satisfy FindBugs
                return super.equals(obj);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVInt#hashCode()
             */
            @Override
            public int hashCode() { // implemented to satisfy FindBugs
                return super.hashCode();
            }
        }
        private class LongValue extends BasePVLong implements PVLong {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private LongValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVLong#get()
             */
            @Override
            public long get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVLong#put(long)
             */
            @Override
            public void put(long value) {
                long lowValue = convert.toLong(lowPVField);
                long highValue = convert.toLong(highPVField);
                if(lowValue<highValue) {
                    if(value<lowValue) {
                        value = lowValue;
                        raiseAlarm(false);
                    } else if(value>highValue) {
                        value = highValue;
                        raiseAlarm(true);
                    }
                }
                this.value = value;
                super.postPut();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVLong#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) { // implemented to satisfy FindBugs
                return super.equals(obj);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVLong#hashCode()
             */
            @Override
            public int hashCode() { // implemented to satisfy FindBugs
                return super.hashCode();
            }
        }
        private class FloatValue extends BasePVFloat implements PVFloat {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private FloatValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVFloat#get()
             */
            @Override
            public float get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVFloat#put(float)
             */
            @Override
            public void put(float value) {
                float lowValue = convert.toFloat(lowPVField);
                float highValue = convert.toFloat(highPVField);
                if(lowValue<highValue) {
                    if(value<lowValue) {
                        value = lowValue;
                        raiseAlarm(false);
                    } else if(value>highValue) {
                        value = highValue;
                        raiseAlarm(true);
                    }
                }
                this.value = value;
                super.postPut();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVFloat#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) { // implemented to satisfy FindBugs
                return super.equals(obj);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVFloat#hashCode()
             */
            @Override
            public int hashCode() { // implemented to satisfy FindBugs
                return super.hashCode();
            }
        }
        private class DoubleValue extends BasePVDouble implements PVDouble {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private DoubleValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVDouble#get()
             */
            @Override
            public double get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pv.PVDouble#put(double)
             */
            @Override
            public void put(double value) {
                double lowValue = convert.toDouble(lowPVField);
                double highValue = convert.toDouble(highPVField);
                if(lowValue<highValue) {
                    if(value<lowValue) {
                        value = lowValue;
                        raiseAlarm(false);
                    } else if(value>highValue) {
                        value = highValue;
                        raiseAlarm(true);
                    }
                }
                this.value = value;
                super.postPut();
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVDouble#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object obj) { // implemented to satisfy FindBugs
                return super.equals(obj);
            }
            /* (non-Javadoc)
             * @see org.epics.pvdata.factory.BasePVDouble#hashCode()
             */
            @Override
            public int hashCode() { // implemented to satisfy FindBugs
                return super.hashCode();
            }
        }
    }
}
