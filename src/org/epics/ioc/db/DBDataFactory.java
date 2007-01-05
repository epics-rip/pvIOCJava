/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.regex.Pattern;

import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;


/**
 * Factory to create default implementations for field data
 * @author mrk
 *
 */
public class DBDataFactory {
    private DBDataFactory(){} // dont create
    
    public static DBDataCreate getDBDataCreate() {
        return DBDataCreateImpl.getDBDataCreate();
    }
    
    private static final class DBDataCreateImpl implements DBDataCreate{
        private static DBDataCreateImpl singleImplementation = null;
        private static synchronized DBDataCreate getDBDataCreate() {
                if (singleImplementation==null) {
                    singleImplementation = new DBDataCreateImpl();
                }
                return singleImplementation;
        }
        // Guarantee that ImplementConvert can only be created via getDBDataCreate
        private DBDataCreateImpl() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataCreate#createData(org.epics.ioc.db.DBData, org.epics.ioc.pv.Field)
         */
        public DBData createData(DBData parent,Field field)
        {
            if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
            String defaultValue = field.getFieldAttribute().getDefault();
            DBD dbd = DBDFactory.getMasterDBD();
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
            case pvEnum:    return createEnumData(parent,field,null);
            case pvMenu: {
                    Menu menu = (Menu)field;
                    DBDMenu dbdMenu = dbd.getMenu(menu.getMenuName());
                    return createMenuData(parent,menu,defaultValue,dbdMenu.getChoices());
                }
            case pvStructure: {
                    Structure structure = (Structure)field;
                    return new DBStructureBase(parent,structure);
                }
            case pvArray: return (DBData)createArrayData(parent,field,0,true);
            case pvLink: return new DBLinkBase(parent,field);
            }
            throw new IllegalArgumentException(
                "Illegal Type. Must be pvBoolean,...,pvLink");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataCreate#createEnumData(org.epics.ioc.db.DBData, org.epics.ioc.pv.Field, java.lang.String[])
         */
        public DBData createEnumData(DBData parent,Field field, String[] choice)
        {
            if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
            return new DBEnumBase(parent,(Enum)field,choice);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataCreate#createArrayData(org.epics.ioc.db.DBData, org.epics.ioc.pv.Field, int, boolean)
         */
        public PVArray createArrayData(DBData parent,
                Field field,int capacity,boolean capacityMutable)
        {
            if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
            Array array = (Array)field;
            Type elementType= array.getElementType();
            String defaultValue = field.getFieldAttribute().getDefault();
            switch(elementType) {
            case pvBoolean: return new BooleanArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvByte:    return new ByteArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvShort:   return new ShortArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvInt:     return new IntArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvLong:    return new LongArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvFloat:   return new FloatArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvDouble:  return new DoubleArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvString:  return new StringArray(parent,array,capacity,capacityMutable,defaultValue);
            case pvEnum:    return new EnumArray(parent,array, capacity, capacityMutable);
            case pvMenu:    return new MenuArray(parent,array, capacity, capacityMutable);
            case pvStructure: return new StructureArray(parent,array, capacity, capacityMutable);
            case pvArray:   return new ArrayArray(parent,array, capacity, capacityMutable);
            case pvLink:    return new LinkArray(parent,array, capacity, capacityMutable);
            }
            throw new IllegalArgumentException("Illegal Type. Logic error");
        }    
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataCreate#createRecord(java.lang.String, org.epics.ioc.dbd.DBDRecordType)
         */
        public DBRecord createRecord(String recordName, DBDRecordType dbdRecordType) {
            DBRecord dbRecord = new DBRecordBase(recordName,dbdRecordType);
            return dbRecord;
        }
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");

    private static DBData createMenuData(DBData parent,Menu menu,String defaultValue,String[]choice) {
        PVMenu pvMenu = new DBMenuBase(parent,menu,choice);
        if(defaultValue!=null && defaultValue.length()>0) {
            String[] choices = pvMenu.getChoices();
            int index = -1;
            for(int i=0; i<choices.length; i++) {
                if(defaultValue.equals(choices[i])) {
                    index = i; break;
                }
            }
            if(index==-1) {
                throw new IllegalStateException("default value is not a choice");
            }
            pvMenu.setIndex(index);
        }
        return (DBData)pvMenu;
    }
    
    private static class BooleanData extends AbstractDBData
        implements PVBoolean
    {
        private boolean value;

        private BooleanData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class ByteData extends AbstractDBData implements PVByte {
        private byte value;
        
        private ByteData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class ShortData extends AbstractDBData implements PVShort {
        private short value;
        
        private ShortData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class IntData extends AbstractDBData implements PVInt {
        private int value;

        private IntData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class LongData extends AbstractDBData implements PVLong {
        private long value;
        
        private LongData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class FloatData extends AbstractDBData implements PVFloat {
        private float value;
        
        private FloatData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class DoubleData extends AbstractDBData implements PVDouble {
        private double value;
        
        private DoubleData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static class StringData extends AbstractDBData implements PVString {
        private String value;
        
        private StringData(DBData parent,Field field,String defaultValue) {
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
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }

    private static abstract class DBArray extends AbstractDBData implements PVArray{
        protected int length = 0;
        protected int capacity;
        protected boolean capacityMutable = true;
        /**
         * Constructer that derived classes must call.
         * @param parent The parent interface.
         * @param dbdArrayField The reflection interface for the DBArray data.
         */
        protected DBArray(DBData parent,Array array,int capacity,boolean capacityMutable) {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        abstract public void setCapacity(int capacity);
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
    }
    
    private static class BooleanArray extends DBArray implements PVBooleanArray
    {
        private boolean[] value;
        
        private BooleanArray(DBData parent,Array array,
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            boolean[]newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBooleanArray#get(int, int, org.epics.ioc.pv.BooleanArrayData)
         */
        public int get(int offset, int len, BooleanArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBooleanArray#put(int, int, boolean[], int)
         */
        public int put(int offset, int len, boolean[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class ByteArray extends DBArray implements PVByteArray
    {
        private byte[] value;
        
        private ByteArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            byte[]newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByteArray#get(int, int, org.epics.ioc.pv.ByteArrayData)
         */
        public int get(int offset, int len, ByteArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByteArray#put(int, int, byte[], int)
         */
        public int put(int offset, int len, byte[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class ShortArray extends DBArray implements PVShortArray
    {
        private short[] value;
        
        private ShortArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            short[]newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShortArray#get(int, int, org.epics.ioc.pv.ShortArrayData)
         */
        public int get(int offset, int len, ShortArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShortArray#put(int, int, short[], int)
         */
        public int put(int offset, int len, short[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class IntArray extends DBArray implements PVIntArray
    {
        private int[] value;
        
        private IntArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            int[]newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVIntArray#get(int, int, org.epics.ioc.pv.IntArrayData)
         */
        public int get(int offset, int len, IntArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVIntArray#put(int, int, int[], int)
         */
        public int put(int offset, int len, int[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class LongArray extends DBArray implements PVLongArray
    {
        private long[] value;   
        
        private LongArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            long[]newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLongArray#get(int, int, org.epics.ioc.pv.LongArrayData)
         */
        public int get(int offset, int len, LongArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLongArray#put(int, int, long[], int)
         */
        public int put(int offset, int len, long[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class FloatArray extends DBArray implements PVFloatArray
    {
        private float[] value;
        
        private FloatArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            float[]newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloatArray#get(int, int, org.epics.ioc.pv.FloatArrayData)
         */
        public int get(int offset, int len, FloatArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloatArray#put(int, int, float[], int)
         */
        public int put(int offset, int len, float[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class DoubleArray extends DBArray implements PVDoubleArray
    {
        private double[] value;
        
        private DoubleArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            double[]newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDoubleArray#get(int, int, org.epics.ioc.pv.DoubleArrayData)
         */
        public int get(int offset, int len, DoubleArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDoubleArray#put(int, int, double[], int)
         */
        public int put(int offset, int len, double[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }     
    }

    private static class StringArray extends DBArray implements PVStringArray
    {

        private String[] value;
        
        private StringArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
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
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            String[]newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#get(int, int, org.epics.ioc.pv.StringArrayData)
         */
        public int get(int offset, int len, StringArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#put(int, int, java.lang.String[], int)
         */
        public int put(int offset, int len, String[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class EnumArray extends DBArray implements PVEnumArray
    {

        private PVEnum[] value;
        
        private EnumArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVEnum[capacity];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVEnum[]newarray = new PVEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumArray#get(int, int, org.epics.ioc.pv.EnumArrayData)
         */
        public int get(int offset, int len, EnumArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumArray#put(int, int, org.epics.ioc.pv.PVEnum[], int)
         */
        public int put(int offset, int len, PVEnum[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    }

    private static class MenuArray extends DBArray implements PVMenuArray
    {
        private PVMenu[] value;
        
        private MenuArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVMenu[capacity];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVMenu[]newarray = new PVMenu[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVMenuArray#get(int, int, org.epics.ioc.pv.MenuArrayData)
         */
        public int get(int offset, int len, MenuArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVMenuArray#put(int, int, org.epics.ioc.pv.PVMenu[], int)
         */
        public int put(int offset, int len, PVMenu[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    newLine(builder,indentLevel+1);
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }       
    }

    private static class StructureArray extends DBArray implements PVStructureArray
    {
        private PVStructure[] value;
        
        private StructureArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVStructure[capacity];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVStructure[]newarray = new PVStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructureArray#get(int, int, org.epics.ioc.pv.StructureArrayData)
         */
        public int get(int offset, int len, StructureArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructureArray#put(int, int, org.epics.ioc.pv.PVStructure[], int)
         */
        public int put(int offset, int len, PVStructure[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }
    }

    private static class ArrayArray extends DBArray implements PVArrayArray
    {
        private PVArray[] value;
        
        private ArrayArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVArray[capacity];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVArray[]newarray = new PVArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArrayArray#get(int, int, org.epics.ioc.pv.ArrayArrayData)
         */
        public int get(int offset, int len, ArrayArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArrayArray#put(int, int, org.epics.ioc.pv.PVArray[], int)
         */
        public int put(int offset, int len, PVArray[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                newLine(builder,indentLevel + 1);
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }
    }

    private static class LinkArray extends DBArray implements PVLinkArray
    {
        private PVLink[] value;
        
        private LinkArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array,capacity,capacityMutable);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVLink[capacity];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBDataFactory.DBArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVLink[]newarray = new PVLink[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLinkArray#get(int, int, org.epics.ioc.pv.LinkArrayData)
         */
        public int get(int offset, int len, LinkArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLinkArray#put(int, int, org.epics.ioc.pv.PVLink[], int)
         */
        public int put(int offset, int len, PVLink[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
                if(i<length-1) newLine(builder,indentLevel + 1);
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }
    }
}
