/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.regex.Pattern;

import org.epics.ioc.util.MessageType;


/**
 * Factory to create default implementations for PVField objects.
 * The PVField instances are created via interface PVDataCreate,
 * which is obtained via a call to <i>PVDataCreateFactory.getPVDataCreate</i>.
 * @author mrk
 *
 */
public class PVDataFactory {
    private PVDataFactory(){} // dont create
    
    /**
     * Get the interface for PVDataCreate.
     * @return The interface.
     */
    public static PVDataCreate getPVDataCreate() {
        return PVDataCreateImpl.getPVDataCreate();
    }
    
    private static final class PVDataCreateImpl implements PVDataCreate{
        private static PVDataCreateImpl singleImplementation = null;
        private static synchronized PVDataCreate getPVDataCreate() {
                if (singleImplementation==null) {
                    singleImplementation = new PVDataCreateImpl();
                }
                return singleImplementation;
        }
        // Guarantee that ImplementConvert can only be created via getPVDataCreate
        private PVDataCreateImpl() {}
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
            case pvBoolean: return new BooleanArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvByte:    return new ByteArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvShort:   return new ShortArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvInt:     return new IntArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvLong:    return new LongArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvFloat:   return new FloatArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvDouble:  return new DoubleArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvString:  return new StringArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvStructure: return new StructureArray(parent,array, capacity, capacityMutable);
            case pvArray:   return new ArrayArray(parent,array, capacity, capacityMutable);
            }
            throw new IllegalArgumentException("Illegal Type. Logic error");
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.db.PVDataCreate#createRecord(java.lang.String, org.epics.ioc.dbd.PVDRecordType)
         */
        public PVRecord createPVRecord(String recordName, Structure dbdRecordType) {
            PVRecord dbRecord = new BasePVRecord(recordName,dbdRecordType);
            return dbRecord;
        }
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");
    
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
    
    private static class BooleanArray extends AbstractPVArray implements PVBooleanArray
    {
        private boolean[] value;
        
        private BooleanArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new boolean[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {}
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                boolean[]newarray = new boolean[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBooleanArray#get(int, int, org.epics.ioc.pv.BooleanArrayData)
         */
        public int get(int offset, int len, BooleanArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBooleanArray#put(int, int, boolean[], int)
         */
        public int put(int offset, int len, boolean[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }            
        }
    }

    private static class ByteArray extends AbstractPVArray implements PVByteArray
    {
        private byte[] value;
        
        private ByteArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new byte[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                byte[]newarray = new byte[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByteArray#get(int, int, org.epics.ioc.pv.ByteArrayData)
         */
        public int get(int offset, int len, ByteArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByteArray#put(int, int, byte[], int)
         */
        public int put(int offset, int len, byte[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }           
        }
    }

    private static class ShortArray extends AbstractPVArray implements PVShortArray
    {
        private short[] value;
        
        private ShortArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new short[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                short[]newarray = new short[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShortArray#get(int, int, org.epics.ioc.pv.ShortArrayData)
         */
        public int get(int offset, int len, ShortArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }           
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShortArray#put(int, int, short[], int)
         */
        public int put(int offset, int len, short[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
    }

    private static class IntArray extends AbstractPVArray implements PVIntArray
    {
        private int[] value;
        
        private IntArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new int[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                int[]newarray = new int[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVIntArray#get(int, int, org.epics.ioc.pv.IntArrayData)
         */
        public int get(int offset, int len, IntArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVIntArray#put(int, int, int[], int)
         */
        public int put(int offset, int len, int[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
    }

    private static class LongArray extends AbstractPVArray implements PVLongArray
    {
        private long[] value;   
        
        private LongArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new long[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                long[]newarray = new long[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLongArray#get(int, int, org.epics.ioc.pv.LongArrayData)
         */
        public int get(int offset, int len, LongArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);}
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLongArray#put(int, int, long[], int)
         */
        public int put(int offset, int len, long[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }            
        }
    }

    private static class FloatArray extends AbstractPVArray implements PVFloatArray
    {
        private float[] value;
        
        private FloatArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new float[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                float[]newarray = new float[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloatArray#get(int, int, org.epics.ioc.pv.FloatArrayData)
         */
        public int get(int offset, int len, FloatArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);}            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloatArray#put(int, int, float[], int)
         */
        public int put(int offset, int len, float[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }            
        }
    }

    private static class DoubleArray extends AbstractPVArray implements PVDoubleArray
    {
        private double[] value;
        
        private DoubleArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new double[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                double[]newarray = new double[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDoubleArray#get(int, int, org.epics.ioc.pv.DoubleArrayData)
         */
        public int get(int offset, int len, DoubleArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDoubleArray#put(int, int, double[], int)
         */
        public int put(int offset, int len, double[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }            
        }     
    }

    private static class StringArray extends AbstractPVArray implements PVStringArray
    {

        private String[] value;
        
        private StringArray(PVField parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            value = new String[capacity];
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractPVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable) {
                super.message("not capacityMutable", MessageType.error);
                return;
            }
            super.asynAccessCallListener(true);
            try {
                if(length>len) length = len;
                String[]newarray = new String[len];
                if(length>0) System.arraycopy(value,0,newarray,0,length);
                value = newarray;
                capacity = len;
            } finally {
                super.asynAccessCallListener(false);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#get(int, int, org.epics.ioc.pv.StringArrayData)
         */
        public int get(int offset, int len, StringArrayData data) {
            super.asynAccessCallListener(true);
            try {
                int n = len;
                if(offset+len > length) n = length - offset;
                data.data = value;
                data.offset = offset;
                return n;
            } finally {
                super.asynAccessCallListener(false);
            }           
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#put(int, int, java.lang.String[], int)
         */
        public int put(int offset, int len, String[]from, int fromOffset) {
            if(!super.isMutable()) {
                super.message("not isMutable", MessageType.error);
                return 0;
            }
            super.asynAccessCallListener(true);
            try {
                if(offset+len > length) {
                    int newlength = offset + len;
                    if(newlength>capacity) {
                        setCapacity(newlength);
                        newlength = capacity;
                        len = newlength - offset;
                        if(len<=0) return 0;
                    }
                    length = newlength;
                }
                System.arraycopy(from,fromOffset,value,offset,len);
                return len;
            } finally {
                super.asynAccessCallListener(false);
            }            
        }
    }

    private static class StructureArray extends BasePVStructureArray
    {   
        private StructureArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
        }
    }

    private static class ArrayArray extends BasePVArrayArray
    {   
        private ArrayArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
        }
    }
}
