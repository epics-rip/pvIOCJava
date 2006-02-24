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
    	default: {throw new Error ("type not implemented");}
    	}
    }
    public PVData createArrayData(String name,PVType type) {
    	switch(type) {
    	case pvByte : {
            Array array = new ByteArrayField(name); return new ByteArrayData(array);
        }
    	default: {throw new Error ("type not implemented");}
    	}
    }
}
