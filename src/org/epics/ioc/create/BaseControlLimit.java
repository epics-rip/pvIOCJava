/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.db.DBField;
import org.epics.ioc.pv.AbstractPVField;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVByte;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVFloat;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVLong;
import org.epics.ioc.pv.PVShort;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AlarmFactory;
import org.epics.ioc.support.AlarmSupport;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;

/**
 * Base class for controlLimit. The PVField for the value fieldis replaced by an implemtation that enforces the control limits.
 * @author mrk
 *
 */
public class BaseControlLimit implements Create{
    private static Convert convert = ConvertFactory.getConvert();
    private DBField valueDBField = null;
    private PVField valuePVField = null;
    private AlarmSupport alarmSupport = null;
    /** Constructor.
     * @param valueDBField The DBField interface for the value field.
     * @param lowDBField The DBField interface for the low limit.
     * @param highDBField The DBField interface for the high limit.
     */
    public BaseControlLimit(DBField valueDBField, DBField lowDBField, DBField highDBField) {
        this.valueDBField = valueDBField;
        valuePVField = valueDBField.getPVField();
        PVField parentPVField = valuePVField.getParent();
        PVField newPVField = null;
        Field valueField = valuePVField.getField();
        Type type = valuePVField.getField().getType();
        switch(type) {
        case pvByte:
            newPVField = new ByteValue(parentPVField,valueField,lowDBField,highDBField);
            break;
        case pvShort:
            newPVField = new ShortValue(parentPVField,valueField,lowDBField,highDBField);
            break;
        case pvInt:
            newPVField = new IntValue(parentPVField,valueField,lowDBField,highDBField);
            break;
        case pvLong:
            newPVField = new LongValue(parentPVField,valueField,lowDBField,highDBField);
            break;
        case pvFloat:
            newPVField = new FloatValue(parentPVField,valueField,lowDBField,highDBField);
            break;
        case pvDouble:
            newPVField = new DoubleValue(parentPVField,valueField,lowDBField,highDBField);
            break;
        default:
            throw new IllegalStateException("valueDBfield does not have a supported type");
        }
        valueDBField.replacePVField(newPVField);
        double oldValue = convert.toDouble(valuePVField);
        convert.fromDouble(newPVField, oldValue);
    }
    
    private void raiseAlarm(boolean isHigh) {
        if(alarmSupport==null) {
            alarmSupport = AlarmFactory.findAlarmSupport(valueDBField);
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
            alarmSupport.setAlarm(message, AlarmSeverity.minor);
        } else {
            valuePVField.message(message, MessageType.warning);
        }
    }
    
    private class ByteValue extends AbstractPVField implements PVByte {
        private DBField lowDBField;
        private DBField highDBField;
        private byte value;
        private ByteValue(PVField parent,Field field,DBField lowDBField,DBField highDBField) {
            super(parent,field);
            this.lowDBField = lowDBField;
            this.highDBField = highDBField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByte#get()
         */
        public byte get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByte#put(byte)
         */
        public void put(byte value) {
            byte lowValue = convert.toByte(lowDBField.getPVField());
            byte highValue = convert.toByte(highDBField.getPVField());
            if(lowValue>highValue) return;
            if(value<lowValue) {
                value = lowValue;
                raiseAlarm(false);
            } else if(value>highValue) {
                value = highValue;
                raiseAlarm(true);
            }
            this.value = value;
        }
    }
    private class ShortValue extends AbstractPVField implements PVShort {
        private DBField lowDBField;
        private DBField highDBField;
        private short value;
        private ShortValue(PVField parent,Field field,DBField lowDBField,DBField highDBField) {
            super(parent,field);
            this.lowDBField = lowDBField;
            this.highDBField = highDBField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShort#get()
         */
        public short get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShort#put(short)
         */
        public void put(short value) {
            short lowValue = convert.toShort(lowDBField.getPVField());
            short highValue = convert.toShort(highDBField.getPVField());
            if(lowValue>highValue) return;
            if(value<lowValue) {
                value = lowValue;
                raiseAlarm(false);
            } else if(value>highValue) {
                value = highValue;
                raiseAlarm(true);
            }
            this.value = value;
        }
    }
    private class IntValue extends AbstractPVField implements PVInt {
        private DBField lowDBField;
        private DBField highDBField;
        private int value;
        private IntValue(PVField parent,Field field,DBField lowDBField,DBField highDBField) {
            super(parent,field);
            this.lowDBField = lowDBField;
            this.highDBField = highDBField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#get()
         */
        public int get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#put(int)
         */
        public void put(int value) {
            int lowValue = convert.toInt(lowDBField.getPVField());
            int highValue = convert.toInt(highDBField.getPVField());
            if(lowValue>highValue) return;
            if(value<lowValue) {
                value = lowValue;
                raiseAlarm(false);
            } else if(value>highValue) {
                value = highValue;
                raiseAlarm(true);
            }
            this.value = value;
        }
    }
    private class LongValue extends AbstractPVField implements PVLong {
        private DBField lowDBField;
        private DBField highDBField;
        private long value;
        private LongValue(PVField parent,Field field,DBField lowDBField,DBField highDBField) {
            super(parent,field);
            this.lowDBField = lowDBField;
            this.highDBField = highDBField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLong#get()
         */
        public long get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLong#put(long)
         */
        public void put(long value) {
            long lowValue = convert.toLong(lowDBField.getPVField());
            long highValue = convert.toLong(highDBField.getPVField());
            if(lowValue>highValue) return;
            if(value<lowValue) {
                value = lowValue;
                raiseAlarm(false);
            } else if(value>highValue) {
                value = highValue;
                raiseAlarm(true);
            }
            this.value = value;
        }
    }
    private class FloatValue extends AbstractPVField implements PVFloat {
        private DBField lowDBField;
        private DBField highDBField;
        private float value;
        private FloatValue(PVField parent,Field field,DBField lowDBField,DBField highDBField) {
            super(parent,field);
            this.lowDBField = lowDBField;
            this.highDBField = highDBField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloat#get()
         */
        public float get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloat#put(float)
         */
        public void put(float value) {
            float lowValue = convert.toFloat(lowDBField.getPVField());
            float highValue = convert.toFloat(highDBField.getPVField());
            if(lowValue>highValue) return;
            if(value<lowValue) {
                value = lowValue;
                raiseAlarm(false);
            } else if(value>highValue) {
                value = highValue;
                raiseAlarm(true);
            }
            this.value = value;
        }
    }
    private class DoubleValue extends AbstractPVField implements PVDouble {
        private DBField lowDBField;
        private DBField highDBField;
        private double value;
        private DoubleValue(PVField parent,Field field,DBField lowDBField,DBField highDBField) {
            super(parent,field);
            this.lowDBField = lowDBField;
            this.highDBField = highDBField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#get()
         */
        public double get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#put(double)
         */
        public void put(double value) {
            double lowValue = convert.toDouble(lowDBField.getPVField());
            double highValue = convert.toDouble(highDBField.getPVField());
            if(lowValue>highValue) return;
            if(value<lowValue) {
                value = lowValue;
                raiseAlarm(false);
            } else if(value>highValue) {
                value = highValue;
                raiseAlarm(true);
            }
            this.value = value;
        }
    }
}
