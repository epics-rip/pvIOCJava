/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Factory to create default implementations for field data
 * @author mrk
 *
 */
public class FieldDataFactory {
   
    /**
     * create implementation for all scalar fields that are of type dbPvData
     * @param dbdField the reflection interface for the field
     * @return the DBData implementation
     */
    public static DBData createData(DBDField dbdField)
    {
        DBType dbType = dbdField.getDBType();
        switch(dbType) {
        case dbPvType:
            Type type = dbdField.getType();
            switch(type) {
            case pvUnknown: return new UnknownData(dbdField);
            case pvBoolean: return new BooleanData(dbdField);
            case pvByte:    return new ByteData(dbdField);
            case pvShort:   return new ShortData(dbdField);
            case pvInt:     return new IntData(dbdField);
            case pvLong:    return new LongData(dbdField);
            case pvFloat:   return new FloatData(dbdField);
            case pvDouble:  return new DoubleData(dbdField);
            case pvString:  return new StringData(dbdField);
            case pvEnum:    return createEnumData(dbdField,null);
            }
        case dbMenu: return new MenuData(dbdField);
        case dbStructure: return new StructureData(dbdField);
        case dbArray: return createArrayData(dbdField,0,true);
        case dbLink: return new LinkData(dbdField);
        }
        throw new IllegalArgumentException(
            "Illegal Type. Must be pvUnknown,...,pvString");
    }

    /**
     * create an implementation for an enumerated field
     * @param dbdField the reflection interface for the field
     * @param choice the enum choices
     * @return the DBData implementation
     */
    public static DBData createEnumData(DBDField dbdField, String[] choice)
    {
        return new EnumData(dbdField,choice);
    }

    /**
     * create an implementation for an array field
     * @param dbdArrayField the reflection interface for the field
     * @param capacity the default capacity for the field
     * @param capacityMutable can the capacity be changed after initialization
     * @return the DBArray implementation
     */
    public static DBArray createArrayData(
            DBDField dbdField,int capacity,boolean capacityMutable)
    {
        DBType elementDbType= dbdField.getDBDAttribute().getElementDBType();
        switch(elementDbType) {
        case dbPvType: {
                Type elementType = dbdField.getDBDAttribute().getElementType();
                switch(elementType) {
                case pvBoolean: return new ArrayBooleanData(
                    dbdField, capacity, capacityMutable);
                case pvByte:    return new ArrayByteData(
                    dbdField, capacity, capacityMutable);
                case pvShort:   return new ArrayShortData(
                    dbdField, capacity, capacityMutable);
                case pvInt:     return new ArrayIntData(
                    dbdField, capacity, capacityMutable);
                case pvLong:    return new ArrayLongData(
                    dbdField, capacity, capacityMutable);
                case pvFloat:   return new ArrayFloatData(
                    dbdField, capacity, capacityMutable);
                case pvDouble:  return new ArrayDoubleData(
                    dbdField, capacity, capacityMutable);
                case pvString:  return new ArrayStringData(
                    dbdField, capacity, capacityMutable);
                case pvEnum:    return new ArrayEnumData(
                    dbdField, capacity, capacityMutable);
                }
                throw new IllegalArgumentException(
                    "Illegal Type. Logic error");
            }
        case dbMenu:
            return new ArrayMenuData(
                 dbdField, capacity, capacityMutable);
        case dbStructure:
            return new ArrayStructureData(
                 dbdField, capacity, capacityMutable);
        case dbArray:
            return new ArrayArrayData(
                 dbdField, capacity, capacityMutable);
        case dbLink:
            return new ArrayLinkData(
                 dbdField, capacity, capacityMutable);
        }
        throw new IllegalArgumentException("Illegal Type. Logic error");
    }
    
    public static DBRecord createRecord(String recordName, DBDRecordType dbdRecordType) {
        return new RecordData(recordName,dbdRecordType);
    }
    
    private static Convert convert = ConvertFactory.getPVConvert();

    private static void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    private static String indentString = "    ";

    private static class UnknownData extends AbstractDBData {

        public String toString() {
            return convert.getString(this);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        UnknownData(DBDField dbdField) {
            super(dbdField);
        }

    }

    private static class BooleanData extends AbstractDBData
        implements DBBoolean
    {

        public boolean get() {
            return value;
        }

        public void put(boolean value) {
            if(super.getField().isMutable()) { this.value = value; return ;}
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        BooleanData(DBDField dbdField) {
            super(dbdField);
            value = false;
        }
        
        private boolean value;

    }

    private static class ByteData extends AbstractDBData implements DBByte {

        public byte get() {
            return value;
        }

        public void put(byte value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ByteData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private byte value;

    }

    private static class ShortData extends AbstractDBData implements DBShort {

        public short get() {
            return value;
        }

        public void put(short value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }

        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ShortData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private short value;

    }

    private static class IntData extends AbstractDBData implements DBInt {

        public int get() {
            return value;
        }

        public void put(int value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        IntData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private int value;

    }

    private static class LongData extends AbstractDBData implements DBLong {

        public long get() {
            return value;
        }

        public void put(long value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        LongData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private long value;

    }

    private static class FloatData extends AbstractDBData implements DBFloat {

        public float get() {
            return value;
        }

        public void put(float value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        FloatData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private float value;

    }

    private static class DoubleData extends AbstractDBData implements DBDouble {

        public double get() {
            return value;
        }

        public void put(double value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        DoubleData(DBDField dbdField) {
            super(dbdField);
            value = 0;
        }
        
        private double value;

    }

    private static class StringData extends AbstractDBData implements DBString {

        public String get() {
            return value;
        }

        public void put(String value) {
            if(super.getField().isMutable()) { this.value = value; return; }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        StringData(DBDField dbdField) {
            super(dbdField);
            value = null;
        }
        
        private String value;

    }

    private static class EnumData extends AbstractDBEnum {

        
        EnumData(DBDField dbdField, String[]choice) {
            super(dbdField,choice);
        }
    }

    private static class MenuData extends AbstractDBMenu {

        MenuData(DBDField dbdField) {
            super(dbdField);
        }
        
    }

    private static class StructureData extends AbstractDBStructure
    {
        StructureData(DBDField dbdField) {
            super(dbdField);
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
        LinkData(DBDField dbdField)
        {
            super(dbdField);
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

        ArrayBooleanData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
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

        ArrayByteData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new byte[capacity];
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

        ArrayShortData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new short[capacity];
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

        ArrayIntData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new int[capacity];
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

        ArrayLongData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new long[capacity];
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

        ArrayFloatData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new float[capacity];
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

        ArrayDoubleData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new double[capacity];
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

        ArrayStringData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new String[capacity];
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

        ArrayEnumData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
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

        ArrayMenuData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
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

        ArrayStructureData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
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

        ArrayArrayData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
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

        ArrayLinkData(DBDField dbdField,
            int capacity,boolean capacityMutable)
        {
            super(dbdField);
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
