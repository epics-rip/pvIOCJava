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
     * create implementation for all non-array fields except enum.
     * @param parent the parent interface.
     * @param dbdField the reflection interface for the field
     * @return the DBData implementation
     */
    public static DBData createData(DBStructure parent,DBDField dbdField)
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
        case dbLink: return new LinkData(parent,(DBDLinkField)dbdField);
        }
        throw new IllegalArgumentException(
            "Illegal Type. Must be pvUnknown,...,pvString");
    }

    /**
     * create an implementation for an enumerated field.
     * @param dbdField the reflection interface for the field.
     * @param choice the enum choices.
     * @return the DBData implementation.
     */
    public static DBData createEnumData(DBStructure parent,DBDField dbdField, String[] choice)
    {
        if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
        return new EnumData(parent,(DBDEnumField)dbdField,choice);
    }

    /**
     * create an implementation for an array field.
     * @param dbdField the reflection interface for the field.
     * @param capacity the default capacity for the field.
     * @param capacityMutable can the capacity be changed after initialization.
     * @return the DBArray implementation.
     */
    public static DBArray createArrayData(DBStructure parent,
            DBDField dbdField,int capacity,boolean capacityMutable)
    {
        if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
        DBType elementDbType= dbdField.getAttribute().getElementDBType();
        switch(elementDbType) {
        case dbPvType: {
                Type elementType = dbdField.getAttribute().getElementType();
                switch(elementType) {
                case pvBoolean: return new ArrayBooleanData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvByte:    return new ArrayByteData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvShort:   return new ArrayShortData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvInt:     return new ArrayIntData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvLong:    return new ArrayLongData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvFloat:   return new ArrayFloatData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvDouble:  return new ArrayDoubleData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvString:  return new ArrayStringData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                case pvEnum:    return new ArrayEnumData(parent,
                    (DBDArrayField)dbdField, capacity, capacityMutable);
                }
                throw new IllegalArgumentException(
                    "Illegal Type. Logic error");
            }
        case dbMenu:
            return new ArrayMenuData(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        case dbStructure:
            return new ArrayStructureData(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        case dbArray:
            return new ArrayArrayData(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        case dbLink:
            return new ArrayLinkData(parent,
                (DBDArrayField)dbdField, capacity, capacityMutable);
        }
        throw new IllegalArgumentException("Illegal Type. Logic error");
    }
    
    /**
     * create a record instance.
     * @param recordName the instance name.
     * @param dbdRecordType the reflection interface for the record type.
     * @return the interface for accessing the record instance.
     */
    public static DBRecord createRecord(String recordName, DBDRecordType dbdRecordType) {
        DBRecord dbRecord = new RecordData(recordName,dbdRecordType);
        return dbRecord;
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern primitivePattern = Pattern.compile("[, ]");


    private static class UnknownData extends AbstractDBData {

        public String toString() {
            return convert.getString(this);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        UnknownData(DBStructure parent,DBDField dbdField) {
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        BooleanData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = false;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ByteData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ShortData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        IntData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        LongData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        FloatData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        DoubleData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
            String defaultValue = dbdField.getAttribute().getDefault();
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
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        StringData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = null;
            String defaultValue = dbdField.getAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        
        private String value;

    }

    private static class EnumData extends AbstractDBEnum {

        
        EnumData(DBStructure parent,DBDEnumField dbdEnumField, String[]choice) {
            super(parent,dbdEnumField,choice);
        }
    }

    private static class MenuData extends AbstractDBMenu {

        MenuData(DBStructure parent,DBDMenuField dbdMenuField) {
            super(parent,dbdMenuField);
        }
        
    }

    private static class StructureData extends AbstractDBStructure
    {
        StructureData(DBStructure parent,DBDStructureField dbdStructureField) {
            super(parent,dbdStructureField);
        }
    }
    
    private static class RecordData extends AbstractDBRecord
    {
        RecordData(String recordName,DBDRecordType dbdRecordType) {
            super(recordName,dbdRecordType);
        }
    }

    private static class LinkData extends AbstractDBLink
    {
        LinkData(DBStructure parent,DBDLinkField dbdLinkField)
        {
            super(parent,dbdLinkField);
        }
    }

    private static class ArrayBooleanData
        extends AbstractDBArray implements DBBooleanArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, boolean[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, boolean[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayBooleanData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayByteData
        extends AbstractDBArray implements DBByteArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, byte[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, byte[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayByteData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new byte[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayShortData
        extends AbstractDBArray implements DBShortArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, short[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, short[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayShortData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new short[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayIntData
        extends AbstractDBArray implements DBIntArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, int[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, int[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayIntData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new int[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayLongData
        extends AbstractDBArray implements DBLongArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, long[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, long[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayLongData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new long[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayFloatData
        extends AbstractDBArray implements DBFloatArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, float[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, float[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayFloatData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new float[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayDoubleData
        extends AbstractDBArray implements DBDoubleArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, double[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, double[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayDoubleData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new double[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayStringData
        extends AbstractDBArray implements DBStringArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, String[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, String[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayStringData(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new String[capacity];
            String defaultValue = dbdArrayField.getAttribute().getDefault();
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

    private static class ArrayEnumData
        extends AbstractDBArray implements DBEnumArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, PVEnum[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVEnum[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayEnumData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayMenuData
        extends AbstractDBArray implements DBMenuArray
    {
        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
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

        public int get(int offset, int len, DBMenu[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBMenu[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayMenuData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayStructureData
        extends AbstractDBArray implements DBStructureArray
    {
        public int get(int offset, int len, PVStructure[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVStructure[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
           return len;
        }

        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
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

        public int get(int offset, int len, DBStructure[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBStructure[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayStructureData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayArrayData
        extends AbstractDBArray implements DBArrayArray
    {
 
        public int get(int offset, int len, PVArray[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVArray[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
           return len;
        }

        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
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

        public int get(int offset, int len, DBArray[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBArray[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayArrayData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayLinkData
        extends AbstractDBArray implements DBLinkArray
    {
 
        public int get(int offset, int len, PVStructure[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVStructure[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
           return len;
        }

        public String toString() {
            return getString(0);
        }
        
        public String toString(int indentLevel) {
            return getString(indentLevel);
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

        public int get(int offset, int len, DBLink[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBLink[] from, int fromOffset) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           postPut();
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

        ArrayLinkData(DBStructure parent,DBDArrayField dbdArrayField,
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
