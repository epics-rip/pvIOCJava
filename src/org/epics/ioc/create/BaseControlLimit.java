/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;

/**
 * Base class for an enumerated structure, which is a structure that has an array of string choices
 * and an index field and a choice field.
 * The index an choice select one of the choices.
 * A put to the index field will also update the choice field and a put to the choice will update the index.
 * This class overrides the PVField implementation for all three fields.
 * @author mrk
 *
 */
public class BaseControlLimit implements Create{
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * The constructor.
     * @param dbIndex The DBField for the index.
     * @param dbChoice The DBField for the choice.
     * @param dbChoices The DBField for the choices.
     */
    public BaseControlLimit(DBField valueDBField, DBField lowDBField, DBField highDBField) {
        PVField valuePVField = valueDBField.getPVField();
        PVField newPVField = null;
        PVField parentPVField = valuePVField.getParent();
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
    
    private static class ByteValue extends AbstractPVField implements PVByte {
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
            if(value<lowValue) value = lowValue;
            if(value>highValue) value = highValue;
            this.value = value;
        }
    }
    private static class ShortValue extends AbstractPVField implements PVShort {
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
            if(value<lowValue) value = lowValue;
            if(value>highValue) value = highValue;
            this.value = value;
        }
    }
    private static class IntValue extends AbstractPVField implements PVInt {
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
            if(value<lowValue) value = lowValue;
            if(value>highValue) value = highValue;
            this.value = value;
        }
    }
    private static class LongValue extends AbstractPVField implements PVLong {
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
            if(value<lowValue) value = lowValue;
            if(value>highValue) value = highValue;
            this.value = value;
        }
    }
    private static class FloatValue extends AbstractPVField implements PVFloat {
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
            if(value<lowValue) value = lowValue;
            if(value>highValue) value = highValue;
            this.value = value;
        }
    }
    private static class DoubleValue extends AbstractPVField implements PVDouble {
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
            if(value<lowValue) value = lowValue;
            if(value>highValue) value = highValue;
            this.value = value;
        }
    }
}
