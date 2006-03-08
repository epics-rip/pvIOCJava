/**
 * 
 */
package org.epics.ioc.pvAccess.example;
import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
abstract class FieldBase implements Field {
    protected String name;
    FieldBase(String name){this.name = name;}
    public String getName() { return name; }
    public Property[] getProperties() { return null; }
    public Property getProperty(String propertyName) { return null; }
    abstract public PVType getPVType();
    public boolean isConstant() { return false; }
    @Override
    public String toString() { return name; }
}

class ByteField extends FieldBase implements Field {
    ByteField(String name){super(name);}
    public PVType getPVType() { return PVType.pvByte; }
}

class ByteData implements PVByte {
    byte value;
    Field field;
    ByteData(Field field) {this.field = field; value = 0;}
    public byte get() { return value; }
    public void put(byte value) { this.value = value;}
    public Field getField() { return field;}

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass()) return false;
        PVByte it = (PVByte)obj;
        if(it.get()==value) return true;
        return false;
    }
    @Override
    public String toString() { return String.format("%d",value);}
}

class ShortField extends FieldBase implements Field {
    ShortField(String name){super(name);}
    public PVType getPVType() { return PVType.pvShort; }
}

class ShortData implements PVShort {
    short value;
    Field field;
    ShortData(Field field) {this.field = field; value = 0;}
    public short get() { return value; }
    public void put(short value) { this.value = value;}
    public Field getField() { return field;}

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass()) return false;
        PVShort it = (PVShort)obj;
        if(it.get()==value) return true;
        return false;
    }
    @Override
    public String toString() { return String.format("%d",value);}
}

class IntField extends FieldBase implements Field {
    IntField(String name){super(name);}
    public PVType getPVType() { return PVType.pvInt; }
}

class IntData implements PVInt {
    int value;
    Field field;
    IntData(Field field) {this.field = field; value = 0;}
    public int get() { return value; }
    public void put(int value) { this.value = value;}
    public Field getField() { return field;}

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass()) return false;
        PVInt it = (PVInt)obj;
        if(it.get()==value) return true;
        return false;
    }
    @Override
    public String toString() { return String.format("%d",value);}
}

class LongField extends FieldBase implements Field {
    LongField(String name){super(name);}
    public PVType getPVType() { return PVType.pvLong; }
}

class LongData implements PVLong {
    long value;
    Field field;
    LongData(Field field) {this.field = field; value = 0;}
    public long get() { return value; }
    public void put(long value) { this.value = value;}
    public Field getField() { return field;}

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass()) return false;
        PVLong it = (PVLong)obj;
        if(it.get()==value) return true;
        return false;
    }
    @Override
    public String toString() { return String.format("%d",value);}
}

class FloatField extends FieldBase implements Field {
    FloatField(String name){super(name);}
    public PVType getPVType() { return PVType.pvFloat; }
}

class FloatData implements PVFloat {
    float value;
    Field field;
    FloatData(Field field) {this.field = field; value = 0;}
    public float get() { return value; }
    public void put(float value) { this.value = value;}
    public Field getField() { return field;}

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass()) return false;
        PVFloat it = (PVFloat)obj;
        if(it.get()==value) return true;
        return false;
    }
    @Override
    public String toString() { return String.format("%f",value);}
}

class DoubleField extends FieldBase implements Field {
    DoubleField(String name){super(name);}
    public PVType getPVType() { return PVType.pvDouble; }
}

class DoubleData implements PVDouble {
    double value;
    Field field;
    DoubleData(Field field) {this.field = field; value = 0;}
    public double get() { return value; }
    public void put(double value) { this.value = value;}
    public Field getField() { return field;}

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != this.getClass()) return false;
        PVDouble it = (PVDouble)obj;
        if(it.get()==value) return true;
        return false;
    }
    @Override
    public String toString() { return String.format("%f",value);}
}

abstract class ArrayFieldBase implements Array {
    protected String name;
    ArrayFieldBase(String name){this.name = name;}
    public String getName() { return name; }
    public Property[] getProperties() { return null; }
    public Property getProperty(String propertyName) { return null; }
    public PVType getPVType() {return PVType.pvArray;}
    public boolean isConstant() { return false; }
    abstract public PVType getElementType();
    @Override
    public String toString() { return name; }
}

class ByteArrayField extends ArrayFieldBase implements Array {
    ByteArrayField(String name){super(name);}
    public PVType getElementType() { return PVType.pvByte; }
}

class ByteArrayData implements PVByteArray {
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
        byte[] newarray = new byte[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        length = capacity = len;
    }
    public int get(int offset, int len, byte[]to, int toOffset) {
        int n = len;
        if(offset+len > length) n = length;
        System.arraycopy(value,offset,to,toOffset,n);
        return n;
    }
    public int put(int offset, int len, byte[]from, int fromOffset) {
        if(offset+len > length) setCapacity(offset+len);
        System.arraycopy(from,fromOffset,value,offset,len);
        return len;
    }
}

class ShortArrayField extends ArrayFieldBase implements Array {
    ShortArrayField(String name){super(name);}
    public PVType getElementType() { return PVType.pvShort; }
}

class ShortArrayData implements PVShortArray {
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
        short[] newarray = new short[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        length = capacity = len;
    }
    public int get(int offset, int len, short[]to, int toOffset) {
        int n = len;
        if(offset+len > length) n = length;
        System.arraycopy(value,offset,to,toOffset,n);
        return n;
    }
    public int put(int offset, int len, short[]from, int fromOffset) {
        if(offset+len > length) setCapacity(offset+len);
        System.arraycopy(from,fromOffset,value,offset,len);
        return len;
    }
}

class IntArrayField extends ArrayFieldBase implements Array {
    IntArrayField(String name){super(name);}
    public PVType getElementType() { return PVType.pvInt; }
}

class IntArrayData implements PVIntArray {
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
        int[] newarray = new int[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        length = capacity = len;
    }
    public int get(int offset, int len, int[]to, int toOffset) {
        int n = len;
        if(offset+len > length) n = length;
        System.arraycopy(value,offset,to,toOffset,n);
        return n;
    }
    public int put(int offset, int len, int[]from, int fromOffset) {
        if(offset+len > length) setCapacity(offset+len);
        System.arraycopy(from,fromOffset,value,offset,len);
        return len;
    }
}

class LongArrayField extends ArrayFieldBase implements Array {
    LongArrayField(String name){super(name);}
    public PVType getElementType() { return PVType.pvLong; }
}

class LongArrayData implements PVLongArray {
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
        long[] newarray = new long[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        length = capacity = len;
    }
    public int get(int offset, int len, long[]to, int toOffset) {
        int n = len;
        if(offset+len > length) n = length;
        System.arraycopy(value,offset,to,toOffset,n);
        return n;
    }
    public int put(int offset, int len, long[]from, int fromOffset) {
        if(offset+len > length) setCapacity(offset+len);
        System.arraycopy(from,fromOffset,value,offset,len);
        return len;
    }
}

class FloatArrayField extends ArrayFieldBase implements Array {
    FloatArrayField(String name){super(name);}
    public PVType getElementType() { return PVType.pvFloat; }
}

class FloatArrayData implements PVFloatArray {
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
        float[] newarray = new float[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        length = capacity = len;
    }
    public int get(int offset, int len, float[]to, int toOffset) {
        int n = len;
        if(offset+len > length) n = length;
        System.arraycopy(value,offset,to,toOffset,n);
        return n;
    }
    public int put(int offset, int len, float[]from, int fromOffset) {
        if(offset+len > length) setCapacity(offset+len);
        System.arraycopy(from,fromOffset,value,offset,len);
        return len;
    }
}

class DoubleArrayField extends ArrayFieldBase implements Array {
    DoubleArrayField(String name){super(name);}
    public PVType getElementType() { return PVType.pvDouble; }
}

class DoubleArrayData implements PVDoubleArray {
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
        double[] newarray = new double[len];
        if(length>0) System.arraycopy(value,0,newarray,0,length);
        value = newarray;
        length = capacity = len;
    }
    public int get(int offset, int len, double[]to, int toOffset) {
        int n = len;
        if(offset+len > length) n = length;
        System.arraycopy(value,offset,to,toOffset,n);
        return n;
    }
    public int put(int offset, int len, double[]from, int fromOffset) {
        if(offset+len > length) setCapacity(offset+len);
        System.arraycopy(from,fromOffset,value,offset,len);
        return len;
    }
}

public class DatabaseExample {
    String name;
    public DatabaseExample(String name) {
        this.name = name;
    }
    public PVData createData(String name,PVType type) {
    	switch(type) {
    	case pvByte : {
            Field field = new ByteField(name); return new ByteData(field);
        }
    	case pvShort : {
            Field field = new ShortField(name); return new ShortData(field);
        }
    	case pvInt : {
            Field field = new IntField(name); return new IntData(field);
        }
    	case pvLong : {
            Field field = new LongField(name); return new LongData(field);
        }
    	case pvFloat : {
            Field field = new FloatField(name); return new FloatData(field);
        }
    	case pvDouble : {
            Field field = new DoubleField(name); return new DoubleData(field);
        }
    	default: {throw new Error ("type not implemented");}
    	}
    }
    public PVData createArrayData(String name,PVType type) {
    	switch(type) {
    	case pvByte : {
            Array array = new ByteArrayField(name); return new ByteArrayData(array);
        }
    	case pvShort : {
            Array array = new ShortArrayField(name); return new ShortArrayData(array);
        }
    	case pvInt : {
            Array array = new IntArrayField(name); return new IntArrayData(array);
        }
    	case pvLong : {
            Array array = new LongArrayField(name); return new LongArrayData(array);
        }
    	case pvFloat : {
            Array array = new FloatArrayField(name); return new FloatArrayData(array);
        }
    	case pvDouble : {
            Array array = new DoubleArrayField(name); return new DoubleArrayData(array);
        }
    	default: {throw new Error ("type not implemented");}
    	}
    }
}
