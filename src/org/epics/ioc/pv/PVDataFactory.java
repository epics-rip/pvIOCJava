/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.MessageType;


/**
 * Factory to create default implementations for PVField objects.
 * The PVField instances are created via interface PVDataCreate,
 * which is obtained via a call to <i>PVDataCreateFactory.getPVDataCreate</i>.
 * @author mrk
 *
 */
public class PVDataFactory {
    private PVDataFactory() {} // don't create
    private static PVDataCreateImpl pvdataCreate = new PVDataCreateImpl();
    /**
     * Get the interface for PVDataCreate.
     * @return The interface.
     */
    public static PVDataCreate getPVDataCreate() {
        return pvdataCreate;
    }
    
    private static final class PVDataCreateImpl implements PVDataCreate{
        private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
        /* (non-Javadoc)
         * @see org.epics.ioc.db.PVDataCreate#createData(org.epics.ioc.db.PVData, org.epics.ioc.pv.Field)
         */
        public PVField createPVField(PVField parent,Field field)
        {
            if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
            String defaultValue = field.getFieldAttribute().getAttribute("default");
            Type type = field.getType();
            switch(type) {
            case pvBoolean: return new BooleanData(parent,field,defaultValue);
            case pvByte:    return new ByteData(parent,field,defaultValue);
            case pvShort:   return new ShortData(parent,field,defaultValue);
            case pvInt:     return new IntData(parent,field,defaultValue);
            case pvLong:    return new LongData(parent,field,defaultValue);
            case pvFloat:   return new FloatData(parent,field,defaultValue);
            case pvDouble:  return new DoubleData(parent,field,defaultValue);
            case pvString:  return new StringData(parent,field,defaultValue);
            case pvStructure: {
                    Structure structure = (Structure)field;
                    return new BasePVStructure(parent,structure);
                }
            case pvArray: return (PVField)createPVArray(parent,field,0,true);
            }
            throw new IllegalArgumentException(
                "Illegal Type. Must be pvBoolean,...,pvStructure");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.PVDataCreate#createArrayData(org.epics.ioc.db.PVData, org.epics.ioc.pv.Field, int, boolean)
         */
        public PVArray createPVArray(PVField parent,
                Field field,int capacity,boolean capacityMutable)
        {
            if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
            Array array = (Array)field;
            Type elementType= array.getElementType();
            String defaultValue = field.getFieldAttribute().getAttribute("default");
            switch(elementType) {
            case pvBoolean: return new BasePVBooleanArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvByte:    return new BasePVByteArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvShort:   return new BasePVShortArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvInt:     return new BasePVIntArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvLong:    return new BasePVLongArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvFloat:   return new BasePVFloatArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvDouble:  return new BasePVDoubleArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvString:  return new BasePVStringArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvStructure: return new BasePVStructureArray(parent,array, capacity, capacityMutable);
            case pvArray:   return new BasePVArrayArray(parent,array, capacity, capacityMutable);
            }
            throw new IllegalArgumentException("Illegal Type. Logic error");
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDataCreate#createPVRecord(java.lang.String, org.epics.ioc.pv.Structure)
         */
        public PVRecord createPVRecord(String recordName, Structure structure) {
            Structure copy = fieldCreate.createStructure("", structure.getStructureName(), structure.getFields());
            PVRecord dbRecord = new BasePVRecord(recordName,copy);
            return dbRecord;
        }
    }
    
    private static class BooleanData extends AbstractPVField
        implements PVBoolean
    {
        private boolean value;

        private BooleanData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = false;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Boolean.parseBoolean(defaultValue);
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }       
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class ByteData extends AbstractPVField implements PVByte {
        private byte value;
        
        private ByteData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Byte.decode(defaultValue);
            }
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
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class ShortData extends AbstractPVField implements PVShort {
        private short value;
        
        private ShortData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Short.decode(defaultValue);
            }
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
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class IntData extends AbstractPVField implements PVInt {
        private int value;

        private IntData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Integer.decode(defaultValue);
            }
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
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class LongData extends AbstractPVField implements PVLong {
        private long value;
        
        private LongData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Long.decode(defaultValue);
            }
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
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class FloatData extends AbstractPVField implements PVFloat {
        private float value;
        
        private FloatData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
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
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class DoubleData extends AbstractPVField implements PVDouble {
        private double value;
        
        private DoubleData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
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
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class StringData extends AbstractPVField implements PVString {
        private String value;
        
        private StringData(PVField parent,Field field,String defaultValue) {
            super(parent,field);
            value = null;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#get()
         */
        public String get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#put(java.lang.String)
         */
        public void put(String value) {
            if(super.isMutable()) {
                this.value = value;
                return ;
            }
            super.message("not isMutable", MessageType.error);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }
}
