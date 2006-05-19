/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess.example;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Enum;

/**
 * Creates a test database.
 * It provides methods to create fields that contain data with any of the supported types.
 * @author mrk
 *
 */

public class DatabaseExample {
    /**
     * Constructor.
     * @param name the name for the database.
     */
    public DatabaseExample(String name) {
        this.name = name;
    }
    
    /**
     * get the database name.
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * create a scalar field.
     * @param name the field name.
     * @param type the type which can be a primitive type or pvString.
     * @param property a propery array. This can be null.
     * @return the PVData interface for the newly created field.
     */
    public PVData createData(String name,Type type, Property[] property) {
    	switch(type) {
    	case pvBoolean : {
            Field field = FieldFactory.createField(name,type, property);
            return new BooleanData(field);
        }
    	case pvByte : {
            Field field = FieldFactory.createField(name,type, property);
            return new ByteData(field);
        }
    	case pvShort : {
            Field field = FieldFactory.createField(name,type, property);
            return new ShortData(field);
        }
    	case pvInt : {
            Field field = FieldFactory.createField(name,type, property);
            return new IntData(field);
        }
    	case pvLong : {
            Field field = FieldFactory.createField(name,type, property);
            return new LongData(field);
        }
    	case pvFloat : {
            Field field = FieldFactory.createField(name,type, property);
            return new FloatData(field);
        }
    	case pvDouble : {
            Field field = FieldFactory.createField(name,type,property);
            return new DoubleData(field);
        }
    	case pvString : {
            Field field = FieldFactory.createField(name,type,property);
            return new StringData(field);
        }
    	default: {throw new Error ("type not implemented");}
    	}
    }

    /**
     * create an enum field.
     * @param name the field name.
     * @param choicesMutable are the choices mutable?
     * @param property a propery array. This can be null.
     * @return the PVEnum interface for the newly created field.
     */
    public PVEnum createEnumData(String name,
    boolean choicesMutable, Property[] property)
    {
        Enum field = FieldFactory.createEnumField(name,choicesMutable,property);
        return new EnumData(field);
    }

    /**
     * create a structure field.
     * @param name the field name.
     * @param structureName the structure name.
     * @param pvData the PVData interfaces for the fields of the structure.
     * These must have already been created.
     * @param property a propery array. This can be null.
     * @return the PVStructure interface for the newly created field.
     */
    public PVStructure createStructureData(String name, String structureName,
            PVData[] pvData, Property[] property)
    {
        int length = pvData.length;
        Field[] field = new Field[length];
        for(int i =0; i < length; i++)  field[i] = pvData[i].getField();
        Structure structure = FieldFactory.createStructureField(
            name,structureName,field,property);
        return new StructureData(structure,pvData);
    }

    /**
     * create an array field.
     * @param name the field name.
     * @param type the element type.
     * @param property a propery array. This can be null.
     * @return the PVArray interface for the newly created array field.
     */
    public PVArray createArrayData(String name,Type type, Property[] property) {
    	switch(type) {
    	case pvBoolean : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new BooleanArrayData(array);
        }
    	case pvByte : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new ByteArrayData(array);
        }
    	case pvShort : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new ShortArrayData(array);
        }
    	case pvInt : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new IntArrayData(array);
        }
    	case pvLong : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new LongArrayData(array);
        }
    	case pvFloat : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new FloatArrayData(array);
        }
    	case pvDouble : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new DoubleArrayData(array);
        }
        case pvString : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new StringArrayData(array);
        }
        case pvEnum : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new EnumArrayData(array);
        }
        case pvStructure : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new StructureArrayData(array);
        }
        case pvArray : {
            Array array = FieldFactory.createArrayField(name,type,property);
            return new ArrayArrayData(array);
        }
    	default: {throw new Error ("type not implemented");}
    	}
    }

    private String name;
    private static Convert convert = ConvertFactory.getConvert();

    private static class BooleanData implements PVBoolean {
        boolean value;
        Field field;
        BooleanData(Field field) {this.field = field; value = false;}
        public boolean get() { return value; }
        public void put(boolean value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }

    private static class ByteData implements PVByte {
        byte value;
        Field field;
        ByteData(Field field) {this.field = field; value = 0;}
        public byte get() { return value; }
        public void put(byte value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class ShortData implements PVShort {
        short value;
        Field field;
        ShortData(Field field) {this.field = field; value = 0;}
        public short get() { return value; }
        public void put(short value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class IntData implements PVInt {
        int value;
        Field field;
        IntData(Field field) {this.field = field; value = 0;}
        public int get() { return value; }
        public void put(int value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class LongData implements PVLong {
        long value;
        Field field;
        LongData(Field field) {this.field = field; value = 0;}
        public long get() { return value; }
        public void put(long value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class FloatData implements PVFloat {
        float value;
        Field field;
        FloatData(Field field) {this.field = field; value = 0;}
        public float get() { return value; }
        public void put(float value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class DoubleData implements PVDouble {
        double value;
        Field field;
        DoubleData(Field field) {this.field = field; value = 0;}
        public double get() { return value; }
        public void put(double value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class StringData implements PVString {
        String value;
        Field field;
        StringData(Field field) {this.field = field; value = null;}
        public String get() { return value; }
        public void put(String value) { this.value = value;}
        public Field getField() { return field;}
    
        public String toString() {
            return value;
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }

    private static class EnumData implements PVEnum {
        private int index;
        private String[] choice;
        private Enum field;

        private final static String[] EMPTY_STRING_ARRAY = new String[0];

        EnumData(Enum field) {
            this.field = field;
            index = 0;
            choice = EMPTY_STRING_ARRAY;
        }

        public String[] getChoices() {
            return choice;
        }

        public int getIndex() {
            return index;
        }

        public boolean setChoices(String[] choice) {
            if(!field.isChoicesMutable()) return false;
            this.choice = choice;
            return true;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Field getField() {
            return field;
        }
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }

    private static class StructureData implements PVStructure {
        private Structure structure;
        private PVData[] pvData;

        public PVData[] getFieldPVDatas() {
            return pvData;
        }

        public Field getField() {
            return structure;
        }

        public StructureData(Structure structure, PVData[] pvData) {
            super();
            this.structure = structure;
            this.pvData = pvData;
        }
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
    }
    
    private static class BooleanArrayData implements PVBooleanArray {
        private int length = 0;
        private int capacity = 0;
        boolean[] value;
        Array array;

        private final static boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    
        BooleanArrayData(Array array) {
            this.array = array;
            value = EMPTY_BOOLEAN_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            boolean[] newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, boolean[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class ByteArrayData implements PVByteArray {
        private int length = 0;
        private int capacity = 0;
        byte[] value;
        Array array;

        private final static byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
        ByteArrayData(Array array) {
            this.array = array;
            value = EMPTY_BYTE_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            byte[] newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, byte[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class ShortArrayData implements PVShortArray {
        private int length = 0;
        private int capacity = 0;
        short[] value;
        Array array;

        private final static short[] EMPTY_SHORT_ARRAY = new short[0];
    
        ShortArrayData(Array array) {
            this.array = array;
            value = EMPTY_SHORT_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            short[] newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, short[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class IntArrayData implements PVIntArray {
        private int length = 0;
        private int capacity = 0;
        int[] value;
        Array array;

        private final static int[] EMPTY_INT_ARRAY = new int[0];
    
        IntArrayData(Array array) {
            this.array = array;
            value = EMPTY_INT_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            int[] newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, int[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class LongArrayData implements PVLongArray {
        private int length = 0;
        private int capacity = 0;
        long[] value;
        Array array;

        private final static long[] EMPTY_LONG_ARRAY = new long[0];
    
        LongArrayData(Array array) {
            this.array = array;
            value = EMPTY_LONG_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            long[] newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, long[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class FloatArrayData implements PVFloatArray {
        private int length = 0;
        private int capacity = 0;
        float[] value;
        Array array;

        private final static float[] EMPTY_FLOAT_ARRAY = new float[0];
    
        FloatArrayData(Array array) {
            this.array = array;
            value = EMPTY_FLOAT_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            float[] newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, float[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class DoubleArrayData implements PVDoubleArray {
        private int length = 0;
        private int capacity = 0;
        double[] value;
        Array array;

        private final static double[] EMPTY_DOUBLE_ARRAY = new double[0];
    
        DoubleArrayData(Array array) {
            this.array = array;
            value = EMPTY_DOUBLE_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            double[] newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, double[]to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
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
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class StringArrayData implements PVStringArray {
        private int length = 0;
        private int capacity = 0;
        String[] value;
        Array array;

        private final static String[] EMPTY_STRING_ARRAY = new String[0];
    
        StringArrayData(Array array) {
            this.array = array;
            value = EMPTY_STRING_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            String[] newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, String[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }
        public int put(int offset, int len, String[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }

    }
    
    private static class EnumArrayData implements PVEnumArray {
        private int length = 0;
        private int capacity = 0;
        PVEnum[] value;
        Array array;

        private final static PVEnum[] EMPTY_PVENUM_ARRAY = new PVEnum[0];
    
        EnumArrayData(Array array) {
            this.array = array;
            value = EMPTY_PVENUM_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            PVEnum[] newarray = new PVEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, PVEnum[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }
        public int put(int offset, int len, PVEnum[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }

        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class StructureArrayData implements PVStructureArray {
        private int length = 0;
        private int capacity = 0;
        PVStructure[] value;
        Array array;

        private final static PVStructure[] EMPTY_PVS_ARRAY = new PVStructure[0];
    
        StructureArrayData(Array array) {
            this.array = array;
            value = EMPTY_PVS_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            PVStructure[] newarray = new PVStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, PVStructure[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }
        public int put(int offset, int len, PVStructure[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
    
    private static class ArrayArrayData implements PVArrayArray {
        private int length = 0;
        private int capacity = 0;
        PVArray[] value;
        Array array;

        private final static PVArray[] EMPTY_PVA_ARRAY = new PVArray[0];
    
        ArrayArrayData(Array array) {
            this.array = array;
            value = EMPTY_PVA_ARRAY;
        }
        public Field getField() { return array;}
        public int getLength(){ return length;}
        public int getCapacity(){ return capacity;}
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        public void setCapacity(int len) {
            if(len<=capacity) return;
            PVArray[] newarray = new PVArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        public int get(int offset, int len, PVArray[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }
        public int put(int offset, int len, PVArray[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
    
        public String toString() {
            return DatabaseExample.convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel);
        }
        
        public boolean isCapacityMutable() {
            return true;
        }
    }
}
