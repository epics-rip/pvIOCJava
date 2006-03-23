/**
 * 
 */
package org.epics.ioc.pvAccess.example;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Enum;

/**
 * @author mrk
 *
 */

public class DatabaseExample {
    public DatabaseExample(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

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

    public PVEnum createEnumData(String name,
    boolean choicesMutable, Property[] property)
    {
        Enum field = FieldFactory.createEnumField(name,choicesMutable,property);
        return new EnumData(field);
    }

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
    static Convert convert = ConvertFactory.getPVConvert();

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
    }

    private static class EnumData implements PVEnum {
        private int index;
        private String[] choice;
        private Enum field;

        EnumData(Enum field) {this.field = field; index = 0; choice = null;}

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
    }
    
    private static class BooleanArrayData implements PVBooleanArray {
        private int length = 0;
        private int capacity = 0;
        boolean[] value;
        Array array;
    
        BooleanArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class ByteArrayData implements PVByteArray {
        private int length = 0;
        private int capacity = 0;
        byte[] value;
        Array array;
    
        ByteArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class ShortArrayData implements PVShortArray {
        private int length = 0;
        private int capacity = 0;
        short[] value;
        Array array;
    
        ShortArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class IntArrayData implements PVIntArray {
        private int length = 0;
        private int capacity = 0;
        int[] value;
        Array array;
    
        IntArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class LongArrayData implements PVLongArray {
        private int length = 0;
        private int capacity = 0;
        long[] value;
        Array array;
    
        LongArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class FloatArrayData implements PVFloatArray {
        private int length = 0;
        private int capacity = 0;
        float[] value;
        Array array;
    
        FloatArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class DoubleArrayData implements PVDoubleArray {
        private int length = 0;
        private int capacity = 0;
        double[] value;
        Array array;
    
        DoubleArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class StringArrayData implements PVStringArray {
        private int length = 0;
        private int capacity = 0;
        String[] value;
        Array array;
    
        StringArrayData(Array array) {this.array = array; value = null;}
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

    }
    
    private static class EnumArrayData implements PVEnumArray {
        private int length = 0;
        private int capacity = 0;
        PVEnum[] value;
        Array array;
    
        EnumArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class StructureArrayData implements PVStructureArray {
        private int length = 0;
        private int capacity = 0;
        PVStructure[] value;
        Array array;
    
        StructureArrayData(Array array) {this.array = array; value = null;}
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
    }
    
    private static class ArrayArrayData implements PVArrayArray {
        private int length = 0;
        private int capacity = 0;
        PVArray[] value;
        Array array;
    
        ArrayArrayData(Array array) {this.array = array; value = null;}
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
    }
}
