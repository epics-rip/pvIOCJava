/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;
import org.epics.ioc.dbd.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;

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
    public static DBData createData(DBData parent,Field field)
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
                return new MenuData(parent,menu,defaultValue,dbdMenu.getChoices());
            }
        case pvStructure: {
                Structure structure = (Structure)field;
                return new StructureData(parent,structure);
            }
        case pvArray: return (DBData)createArrayData(parent,field,0,true);
        case pvLink: return new LinkData(parent,field);
        }
        throw new IllegalArgumentException(
            "Illegal Type. Must be pvBoolean,...,pvLink");
    }

    /**
     * Create an implementation for an enumerated field.
     * @param dbdField The reflection interface for the field.
     * @param choice The enum choices.
     * @return The DBData implementation.
     */
    public static DBData createEnumData(DBData parent,Field field, String[] choice)
    {
        if(parent==null) throw new IllegalArgumentException("Illegal parent is null");
        return new EnumData(parent,(Enum)field,choice);
    }

    /**
     * Create an implementation for an array field.
     * @param dbdField The reflection interface for the field.
     * @param capacity The default capacity for the field.
     * @param capacityMutable Can the capacity be changed after initialization?
     * @return The DBArray implementation.
     */
    public static PVArray createArrayData(DBData parent,
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


    private static class LinkData extends AbstractDBData implements PVLink {
        private PVStructure configDBStructure = null;
        
        private LinkData(DBData parent,Field field) {
            super(parent,field);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.db.AbstractDBData#setSupportName(java.lang.String)
         */
        public String setSupportName(String name) {
            DBD dbd = getRecord().getDBD();
            if(dbd==null) return "DBD was not set";
            DBDLinkSupport dbdLinkSupport = dbd.getLinkSupport(name);
            if(dbdLinkSupport==null) return "support " + name + " not defined";
            super.setSupportName(name);
            String configurationStructureName = dbdLinkSupport.getConfigurationStructureName();
            if(configurationStructureName==null) return null;
            DBDStructure dbdStructure = dbd.getStructure(configurationStructureName);
            if(dbdStructure==null) {
                return "configurationStructure " + configurationStructureName
                    + " for support " + name
                    + " does not exist";
            }
            Field field =DBDFieldFactory.createStructureField(
                name, null,dbdStructure.getFieldAttribute(), dbdStructure,dbd);
            configDBStructure  = (PVStructure)FieldDataFactory.createData(this,field);
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLink#getConfigurationStructure()
         */
        public PVStructure getConfigurationStructure() {
            return configDBStructure;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLink#setConfigurationStructure(org.epics.ioc.pv.PVStructure)
         */
        public boolean setConfigurationStructure(PVStructure pvStructure) {
            if(configDBStructure==null) return false;
            if(!convert.isCopyStructureCompatible(
            (Structure)pvStructure.getField(),
            (Structure)configDBStructure.getField())) {
                return false;
            }
            convert.copyStructure(pvStructure, configDBStructure);
            return true;
        }

        public String toString() {
            return toString(0);
        }

        public String toString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append(convert.getString(this, indentLevel));
            builder.append(super.toString(indentLevel));
            if(configDBStructure!=null) {
                builder.append(configDBStructure.toString(indentLevel));
            }
            return builder.toString();
        }

    }

    private static class BooleanData extends AbstractDBData
        implements PVBoolean
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

        private BooleanData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = false;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Boolean.parseBoolean(defaultValue);
            }
        }
        
        private boolean value;

    }

    private static class ByteData extends AbstractDBData implements PVByte {

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

        private ByteData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Byte.decode(defaultValue);
            }
        }
        
        private byte value;

    }

    private static class ShortData extends AbstractDBData implements PVShort {

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

        private ShortData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Short.decode(defaultValue);
            }
        }
        
        private short value;

    }

    private static class IntData extends AbstractDBData implements PVInt {

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

        private IntData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Integer.decode(defaultValue);
            }
        }
        
        private int value;

    }

    private static class LongData extends AbstractDBData implements PVLong {

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

        private LongData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Long.decode(defaultValue);
            }
        }
        
        private long value;

    }

    private static class FloatData extends AbstractDBData implements PVFloat {

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

        private FloatData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        
        private float value;

    }

    private static class DoubleData extends AbstractDBData implements PVDouble {

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

        private DoubleData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = 0;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        
        private double value;

    }

    private static class StringData extends AbstractDBData implements PVString {

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

        private StringData(DBData parent,Field field,String defaultValue) {
            super(parent,field);
            value = null;
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        
        private String value;

    }

    private static class EnumData extends AbstractDBEnum {

        
        private EnumData(DBData parent,Enum enumField, String[]choice) {
            super(parent,enumField,choice);
        }
    }

    private static class MenuData extends AbstractDBMenu {

        private MenuData(DBData parent,Menu menu,String defaultValue,String[]choice) {
            super(parent,menu,choice);
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
        StructureData(DBData parent,Structure structure) {
            super(parent,structure);
        }
    }
    
    private static class RecordData extends AbstractDBRecord
    {
        RecordData(String recordName,DBDRecordType dbdRecordType) {
            super(recordName,dbdRecordType);
        }
    }

    private static class BooleanArray
        extends AbstractDBArray implements PVBooleanArray
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

        private BooleanArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
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
        extends AbstractDBArray implements PVByteArray
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

        private ByteArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
     
        private byte[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ShortArray
        extends AbstractDBArray implements PVShortArray
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

        private ShortArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
        
        private short[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class IntArray
        extends AbstractDBArray implements PVIntArray
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

        private IntArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
        
        private int[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class LongArray
        extends AbstractDBArray implements PVLongArray
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

        private LongArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
        
        private long[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class FloatArray
        extends AbstractDBArray implements PVFloatArray
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

        private FloatArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
        
        private float[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class DoubleArray
        extends AbstractDBArray implements PVDoubleArray
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

        private DoubleArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
        
        private double[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class StringArray
        extends AbstractDBArray implements PVStringArray
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

        private StringArray(DBData parent,Array array,
            int capacity,boolean capacityMutable,String defaultValue)
        {
            super(parent,array);
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
        
        private String[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class EnumArray
        extends AbstractDBArray implements PVEnumArray
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
            PVEnum[]newarray = new PVEnum[len];
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

        private EnumArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVEnum[capacity];
        }
        
        private PVEnum[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class MenuArray
        extends AbstractDBArray implements PVMenuArray
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
        public int put(int offset, int len, PVMenu[]from, int fromOffset) {
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
            PVMenu[]newarray = new PVMenu[len];
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

        private MenuArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVMenu[capacity];
        }
        
        private PVMenu[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class StructureArray
        extends AbstractDBArray implements PVStructureArray
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
            PVStructure[]newarray = new PVStructure[len];
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
        
        private StructureArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVStructure[capacity];
        }
        
        private PVStructure[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayArray
        extends AbstractDBArray implements PVArrayArray
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
            PVArray[]newarray = new PVArray[len];
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

        private ArrayArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVArray[capacity];
        }
        
        private PVArray[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class LinkArray
        extends AbstractDBArray implements PVLinkArray
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
            PVLink[]newarray = new PVLink[len];
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
        public int put(int offset, int len, PVLink[]from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }

        private LinkArray(DBData parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVLink[capacity];
        }
        
        private PVLink[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

}
