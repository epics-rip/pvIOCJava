/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

import java.util.regex.Pattern;

/**
 * Factory to create default implementations for field data
 * @author mrk
 *
 */
public class FieldDataFactory {
   
    /**
     * Create implementation for all non-array fields except enum.
     * @param parent The parent interface.
     * @param dbdField The reflection interface for the field
     * @return The DBData implementation
     */
    public static DBData createData(DBData parent,DBDField dbdField)
    {
        if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
        DBType dbType = dbdField.getDBType();
        switch(dbType) {
        case dbPvType:
            Type type = dbdField.getType();
            switch(type) {
            case pvUnknown: return new UnknownData(parent,dbdField);
            case pvBoolean: return new BooleanData(parent,dbdField);
            case pvByte:    return new ByteData(parent,dbdField);
            case pvShort:   return new ShortData(parent,dbdField);
            case pvInt:     return new IntData(parent,dbdField);
            case pvLong:    return new LongData(parent,dbdField);
            case pvFloat:   return new FloatData(parent,dbdField);
            case pvDouble:  return new DoubleData(parent,dbdField);
            case pvString:  return new StringData(parent,dbdField);
            case pvEnum:    return createEnumData(parent,dbdField,null);
            }
        case dbMenu: return new MenuData(parent,(DBDMenuField)dbdField);
        case dbStructure: return new StructureData(parent,(DBDStructureField)dbdField);
        case dbArray: return createArrayData(parent,dbdField,0,true);
        case dbLink: return new UnknownData(parent,dbdField);
        }
        throw new IllegalArgumentException(
            "Illegal Type. Must be pvUnknown,...,pvString");
    }

    /**
     * Create an implementation for an enumerated field.
     * @param dbdField The reflection interface for the field.
     * @param choice The enum choices.
     * @return The DBData implementation.
     */
    public static DBData createEnumData(DBData parent,DBDField dbdField, String[] choice)
    {
        if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
        return new EnumData(parent,(DBDEnumField)dbdField,choice);
    }

    /**
     * Create an implementation for an array field.
     * @param dbdField The reflection interface for the field.
     * @param capacity The default capacity for the field.
     * @param capacityMutable Can the capacity be changed after initialization?
     * @return The DBArray implementation.
     */
    public static DBArray createArrayData(DBData parent,
            DBDField dbdField,int capacity,boolean capacityMutable)
    {
        if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
        DBDArrayField dbdArrayField = (DBDArrayField)dbdField;
        DBType elementDbType= dbdArrayField.getElementDBType();
        switch(elementDbType) {
        case dbPvType: {
                Type elementType = dbdArrayField.getElementType();
                switch(elementType) {
                case pvBoolean: return new BooleanArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvByte:    return new ByteArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvShort:   return new ShortArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvInt:     return new IntArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvLong:    return new LongArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvFloat:   return new FloatArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvDouble:  return new DoubleArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvString:  return new StringArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvEnum:    return new EnumArray(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                }
                throw new IllegalArgumentException(
                    "Illegal Type. Logic error");
            }
        case dbMenu:
            return new MenuArray(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        case dbStructure:
            return new StructureArray(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        case dbArray:
            return new ArrayArray(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        case dbLink:
            return new LinkArray(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        }
        throw new IllegalArgumentException("Illegal Type. Logic error");
    }
    
    /**
     * Create a record instance.
     * @param recordName The instance name.
     * @param dbdRecordType The reflection interface for the record type.
     * @return The interface for accessing the record instance.
     */
    public static DBRecord createRecord(String recordName, DBDRecordType dbdRecordType) {
        DBRecord dbRecord = new RecordData(recordName,dbdRecordType);
        return dbRecord;
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");


    private static class UnknownData extends AbstractDBData implements DBLink {

        public String toString() {
            return toString(0);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
                + super.toString(indentLevel);
        }

        UnknownData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
        }

    }

    private static class BooleanData extends AbstractDBData
        implements DBBoolean
    {

        public boolean get() {
            return value;
        }

        public void put(boolean value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        BooleanData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = false;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Boolean.parseBoolean(defaultValue);
            }
        }
        
        private boolean value;

    }

    private static class ByteData extends AbstractDBData implements DBByte {

        public byte get() {
            return value;
        }

        public void put(byte value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        ByteData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Byte.decode(defaultValue);
            }
        }
        
        private byte value;

    }

    private static class ShortData extends AbstractDBData implements DBShort {

        public short get() {
            return value;
        }

        public void put(short value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        ShortData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Short.decode(defaultValue);
            }
        }
        
        private short value;

    }

    private static class IntData extends AbstractDBData implements DBInt {

        public int get() {
            return value;
        }

        public void put(int value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        IntData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Integer.decode(defaultValue);
            }
        }
        
        private int value;

    }

    private static class LongData extends AbstractDBData implements DBLong {

        public long get() {
            return value;
        }

        public void put(long value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        LongData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Long.decode(defaultValue);
            }
        }
        
        private long value;

    }

    private static class FloatData extends AbstractDBData implements DBFloat {

        public float get() {
            return value;
        }

        public void put(float value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        FloatData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        
        private float value;

    }

    private static class DoubleData extends AbstractDBData implements DBDouble {

        public double get() {
            return value;
        }

        public void put(double value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        DoubleData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        
        private double value;

    }

    private static class StringData extends AbstractDBData implements DBString {

        public String get() {
            return value;
        }

        public void put(String value) {
            if(getField().isMutable()) {
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        StringData(DBData parent,DBDField dbdField) {
            super(parent,dbdField);
            value = null;
            String defaultValue = dbdField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        
        private String value;

    }

    private static class EnumData extends AbstractDBEnum {

        
        EnumData(DBData parent,DBDEnumField dbdEnumField, String[]choice) {
            super(parent,dbdEnumField,choice);
        }
    }

    private static class MenuData extends AbstractDBMenu {

        MenuData(DBData parent,DBDMenuField dbdMenuField) {
            super(parent,dbdMenuField);
            String defaultValue = dbdMenuField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] choices = super.getChoices();
                int index = -1;
                for(int i=0; i<choices.length; i++) {
                    if(defaultValue.equals(choices[i])) {
                        index = i; break;
                    }
                }
                if(index==-1) {
                    throw new IllegalStateException("default value is not a choice");
                }
                super.setIndex(index);
            }
        }
        
    }

    private static class StructureData extends AbstractDBStructure
    {
        StructureData(DBData parent,DBDStructureField dbdStructureField) {
            super(parent,dbdStructureField);
        }
    }
    
    private static class RecordData extends AbstractDBRecord
    {
        RecordData(String recordName,DBDRecordType dbdRecordType) {
            super(recordName,dbdRecordType);
        }
    }

    private static class BooleanArray
        extends AbstractDBArray implements DBBooleanArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, BooleanArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, boolean[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            boolean[]newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        BooleanArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private boolean[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ByteArray
        extends AbstractDBArray implements DBByteArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, ByteArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, byte[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            byte[]newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ByteArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new byte[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
     
        private byte[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ShortArray
        extends AbstractDBArray implements DBShortArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, ShortArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, short[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            short[]newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ShortArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new short[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private short[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class IntArray
        extends AbstractDBArray implements DBIntArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, IntArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, int[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            int[]newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        IntArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new int[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private int[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class LongArray
        extends AbstractDBArray implements DBLongArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, LongArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, long[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            long[]newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        LongArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new long[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private long[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class FloatArray
        extends AbstractDBArray implements DBFloatArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, FloatArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, float[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            float[]newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        FloatArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new float[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private float[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class DoubleArray
        extends AbstractDBArray implements DBDoubleArray
    {

        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, DoubleArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, double[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            double[]newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        DoubleArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new double[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private double[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class StringArray
        extends AbstractDBArray implements DBStringArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, StringArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, String[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            String[]newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        StringArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new String[capacity];
            String defaultValue = dbdArrayField.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                String[] values = primitivePattern.split(defaultValue);
                try {
                    convert.fromStringArray(this,0,values.length,values,0);
                } catch (NumberFormatException e) {
                }
            }
        }
        
        private String[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class EnumArray
        extends AbstractDBArray implements DBEnumArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, EnumArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, PVEnum[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBEnum[]newarray = new DBEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        EnumArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBEnum[capacity];
        }
        
        private DBEnum[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class MenuArray
        extends AbstractDBArray implements DBMenuArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int get(int offset, int len, MenuArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, DBMenu[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBMenu[]newarray = new DBMenu[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        MenuArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBMenu[capacity];
        }
        
        private DBMenu[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class StructureArray
        extends AbstractDBArray implements DBStructureArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBStructure[]newarray = new DBStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public int get(int offset, int len, StructureArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, PVStructure[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int get(int offset, int len, DBStructureArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, DBStructure[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }

        StructureArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBStructure[capacity];
        }
        
        private DBStructure[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayArray
        extends AbstractDBArray implements DBArrayArray
    {
 

        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBArray[]newarray = new DBArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public int get(int offset, int len, ArrayArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, PVArray[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        public int get(int offset, int len, DBArrayArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, DBArray[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }

        ArrayArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBArray[capacity];
        }
        
        private DBArray[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class LinkArray
        extends AbstractDBArray implements DBLinkArray
    {
        public String toString() {
            return toString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel)
            + super.toString(indentLevel);
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBLink[]newarray = new DBLink[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public int get(int offset, int len, LinkArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        public int put(int offset, int len, DBLink[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }

        LinkArray(DBData parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBLink[capacity];
        }
        
        private DBLink[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

}
