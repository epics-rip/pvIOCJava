/**
 * 
 */
package org.epics.ioc.pvAccess.example;
import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
class ByteField implements Field {
    private String name;
    ByteField(String name){this.name = name;}
    public String getName() { return name; }
    public Property[] getProperties() { return null; }
    public Property getProperty(String propertyName) { return null; }
    public PVType getPVType() { return PVType.pvByte; }
    public boolean isConstant() { return false; }
    @Override
    public String toString() { return name; }
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

public class DatabaseExample {
    String name;
    public DatabaseExample(String name) {
        this.name = name;
    }
    public PVData createData(String name,PVType type) {
    	switch(type) {
    	case pvByte : {Field field = new ByteField(name); return new ByteData(field); }
    	default: {throw new Error ("type not implemented");}
    	}
    }
}
