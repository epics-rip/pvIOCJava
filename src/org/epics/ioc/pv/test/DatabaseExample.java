/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv.test;
import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;
import org.epics.ioc.util.MessageType;

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
        FieldAttribute fieldAttribute = fieldCreate.createFieldAttribute();
    	switch(type) {
    	case pvBoolean : {
            Field field = fieldCreate.createField(name,type, property,fieldAttribute);
            return new BooleanData(field);
        }
    	case pvByte : {
            Field field = fieldCreate.createField(name,type, property,fieldAttribute);
            return new ByteData(field);
        }
    	case pvShort : {
            Field field = fieldCreate.createField(name,type, property,fieldAttribute);
            return new ShortData(field);
        }
    	case pvInt : {
            Field field = fieldCreate.createField(name,type, property,fieldAttribute);
            return new IntData(field);
        }
    	case pvLong : {
            Field field = fieldCreate.createField(name,type, property,fieldAttribute);
            return new LongData(field);
        }
    	case pvFloat : {
            Field field = fieldCreate.createField(name,type, property,fieldAttribute);
            return new FloatData(field);
        }
    	case pvDouble : {
            Field field = fieldCreate.createField(name,type,property,fieldAttribute);
            return new DoubleData(field);
        }
    	case pvString : {
            Field field = fieldCreate.createField(name,type,property,fieldAttribute);
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
        FieldAttribute fieldAttribute = fieldCreate.createFieldAttribute();
        Enum field = fieldCreate.createEnum(name,choicesMutable,property,fieldAttribute);
        return new EnumData(field);
    }

    /**
     * create a structure field.
     * @param name The field name.
     * @param structureName the structure name.
     * @param pvData the PVData interfaces for the fields of the structure.
     * These must have already been created.
     * @param property A propery array. This can be null.
     * @return The PVStructure interface for the newly created field.
     */
    public PVStructure createStructureData(String name, String structureName,
            PVData[] pvData, Property[] property)
    {
        FieldAttribute fieldAttribute = fieldCreate.createFieldAttribute();
        int length = 0;
        if(pvData!=null) length = pvData.length;
        Field[] field = new Field[length];
        for(int i =0; i < length; i++)  field[i] = pvData[i].getField();
        Structure structure = fieldCreate.createStructure(
            name,structureName,field,property,fieldAttribute);
        return new StructureData(structure,pvData);
    }
    
    /**
     * create a structure field.
     * @param name The field name.
     * @param structure The Structure that describes the PV.
     * @param pvData The array of PVData for the fields of the structure.
     * @param property A propery array. This can be null.
     * @return The PVStructure interface for the newly created field.
     */
    public PVStructure createStructureData(String name, Structure structure,
            PVData[] pvData, Property[] property)
    {
        FieldAttribute fieldAttribute = fieldCreate.createFieldAttribute();
        return new StructureData(structure,pvData);
    }
    /**
     * create a structure field.
     * @param name The field name.
     * @param property A propery array. This can be null.
     * @return The PVStructure interface for the newly created field.
     */
    public PVStructure createStructureData(String name, Property[] property)
    {
        FieldAttribute fieldAttribute = fieldCreate.createFieldAttribute();
        Field[] field = new Field[0];
        PVData[] pvData = new PVData[0];
        Structure structure = fieldCreate.createStructure(
            name,null,field,property,fieldAttribute);
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
        FieldAttribute fieldAttribute = fieldCreate.createFieldAttribute();
    	switch(type) {
    	case pvBoolean : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new BooleanArray(array);
        }
    	case pvByte : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new ByteArray(array);
        }
    	case pvShort : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new ShortArray(array);
        }
    	case pvInt : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new IntArray(array);
        }
    	case pvLong : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new LongArray(array);
        }
    	case pvFloat : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new FloatArray(array);
        }
    	case pvDouble : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new DoubleArray(array);
        }
        case pvString : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new StringArray(array);
        }
        case pvEnum : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new EnumArray(array);
        }
        case pvStructure : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new StructureArray(array);
        }
        case pvArray : {
            Array array = fieldCreate.createArray(name,type,property,fieldAttribute);
            return new ArrayArray(array);
        }
    	default: {throw new Error ("type not implemented");}
    	}
    }

    private String name;
    private static Convert convert = ConvertFactory.getConvert();
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    
    private static abstract class Data implements PVData {
        String supportName = null;
        PVStructure configureStructure = null;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        abstract public Field getField();
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getFullFieldName()
         */
        public String getFullFieldName() {
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getParent()
         */
        public PVData getParent() {
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#getRecord()
         */
        public PVRecord getPVRecord() {
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            // TODO Auto-generated method stub
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVData#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            // TODO Auto-generated method stub
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getSupportName()
         */
        public String getSupportName() {
            return supportName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#setSupportName(java.lang.String)
         */
        public String setSupportName(String name) {
            supportName = name;
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getConfigurationStructure()
         */
        public PVStructure getConfigurationStructure() {
            return configureStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            if(supportName!=null) return " supportName " + supportName;
            return "";
        }
        
    }

    private static class BooleanData extends Data implements PVBoolean {
        boolean value;
        Field field;
        BooleanData(Field field) {this.field = field; value = false;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBoolean#get()
         */
        public boolean get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBoolean#put(boolean)
         */
        public void put(boolean value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }

    private static class ByteData extends Data implements PVByte {
        byte value;
        Field field;
        ByteData(Field field) {this.field = field; value = 0;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByte#get()
         */
        public byte get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByte#put(byte)
         */
        public void put(byte value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class ShortData extends Data implements PVShort {
        short value;
        Field field;
        ShortData(Field field) {this.field = field; value = 0;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShort#get()
         */
        public short get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShort#put(short)
         */
        public void put(short value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class IntData extends Data implements PVInt {
        int value;
        Field field;
        IntData(Field field) {this.field = field; value = 0;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVInt#get()
         */
        public int get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVInt#put(int)
         */
        public void put(int value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class LongData extends Data implements PVLong {
        long value;
        Field field;
        LongData(Field field) {this.field = field; value = 0;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLong#get()
         */
        public long get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLong#put(long)
         */
        public void put(long value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class FloatData extends Data implements PVFloat {
        float value;
        Field field;
        FloatData(Field field) {this.field = field; value = 0;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloat#get()
         */
        public float get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloat#put(float)
         */
        public void put(float value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class DoubleData extends Data implements PVDouble {
        double value;
        Field field;
        DoubleData(Field field) {this.field = field; value = 0;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDouble#get()
         */
        public double get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDouble#put(double)
         */
        public void put(double value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class StringData extends Data implements PVString {
        String value;
        Field field;
        StringData(Field field) {this.field = field; value = null;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVString#get()
         */
        public String get() { return value; }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVString#put(java.lang.String)
         */
        public void put(String value) { this.value = value;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return field;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }

    private static class EnumData extends Data implements PVEnum {
        private int index;
        private String[] choice;
        private Enum field;

        private final static String[] EMPTY_STRING_ARRAY = new String[0];

        EnumData(Enum field) {
            this.field = field;
            index = 0;
            choice = EMPTY_STRING_ARRAY;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnum#getChoices()
         */
        public String[] getChoices() {
            return choice;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnum#getIndex()
         */
        public int getIndex() {
            return index;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnum#setChoices(java.lang.String[])
         */
        public boolean setChoices(String[] choice) {
            if(!field.isChoicesMutable()) return false;
            this.choice = choice;
            return true;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnum#setIndex(int)
         */
        public void setIndex(int index) {
            this.index = index;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() {
            return field;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }

    private static class StructureData extends Data implements PVStructure {
        private Structure structure;
        private PVData[] pvData;

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructure#getFieldPVDatas()
         */
        public PVData[] getFieldPVDatas() {
            return pvData;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() {
            return structure;
        }

        StructureData(Structure structure, PVData[] pvData) {
            super();
            this.structure = structure;
            this.pvData = pvData;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#beginPut()
         */
        public void beginPut() {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#endPut()
         */
        public void endPut() {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructure#replaceField(java.lang.String, org.epics.ioc.pv.PVData)
         */
        public boolean replaceStructureField(String fieldName, String structureName) {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class BooleanArray extends Data implements PVBooleanArray {
        private int length = 0;
        private int capacity = 0;
        boolean[] value;
        Array array;

        private final static boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    
        BooleanArray(Array array) {
            this.array = array;
            value = EMPTY_BOOLEAN_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            boolean[] newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBooleanArray#get(int, int, org.epics.ioc.pvAccess.BooleanArrayData)
         */
        public int get(int offset, int len, BooleanArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBooleanArray#put(int, int, org.epics.ioc.pvAccess.BooleanArrayData)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvBoolean;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class ByteArray extends Data implements PVByteArray {
        private int length = 0;
        private int capacity = 0;
        byte[] value;
        Array array;

        private final static byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
        ByteArray(Array array) {
            this.array = array;
            value = EMPTY_BYTE_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            byte[] newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByteArray#get(int, int, org.epics.ioc.pvAccess.ByteArrayData)
         */
        public int get(int offset, int len, ByteArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByteArray#put(int, int, org.epics.ioc.pvAccess.ByteArrayData)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvByte;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class ShortArray extends Data implements PVShortArray {
        private int length = 0;
        private int capacity = 0;
        short[] value;
        Array array;

        private final static short[] EMPTY_SHORT_ARRAY = new short[0];
    
        ShortArray(Array array) {
            this.array = array;
            value = EMPTY_SHORT_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            short[] newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShortArray#get(int, int, org.epics.ioc.pvAccess.ShortArrayData)
         */
        public int get(int offset, int len, ShortArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShortArray#put(int, int, org.epics.ioc.pvAccess.ShortArrayData)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvShort;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class IntArray extends Data implements PVIntArray {
        private int length = 0;
        private int capacity = 0;
        int[] value;
        Array array;

        private final static int[] EMPTY_INT_ARRAY = new int[0];
    
        IntArray(Array array) {
            this.array = array;
            value = EMPTY_INT_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            int[] newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVIntArray#get(int, int, org.epics.ioc.pvAccess.IntArrayData)
         */
        public int get(int offset, int len, IntArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVIntArray#put(int, int, org.epics.ioc.pvAccess.IntArrayData)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvInt;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class LongArray extends Data implements PVLongArray {
        private int length = 0;
        private int capacity = 0;
        long[] value;
        Array array;

        private final static long[] EMPTY_LONG_ARRAY = new long[0];
    
        LongArray(Array array) {
            this.array = array;
            value = EMPTY_LONG_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            long[] newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLongArray#get(int, int, org.epics.ioc.pvAccess.LongArrayData)
         */
        public int get(int offset, int len, LongArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLongArray#put(int, int, org.epics.ioc.pvAccess.LongArrayData)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvLong;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class FloatArray extends Data implements PVFloatArray {
        private int length = 0;
        private int capacity = 0;
        float[] value;
        Array array;

        private final static float[] EMPTY_FLOAT_ARRAY = new float[0];
    
        FloatArray(Array array) {
            this.array = array;
            value = EMPTY_FLOAT_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            float[] newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloatArray#get(int, int, org.epics.ioc.pvAccess.FloatArrayData)
         */
        public int get(int offset, int len, FloatArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloatArray#put(int, int, org.epics.ioc.pvAccess.FloatArrayData)
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
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvFloat;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class DoubleArray extends Data implements PVDoubleArray {
        private int length = 0;
        private int capacity = 0;
        double[] value;
        Array array;

        private final static double[] EMPTY_DOUBLE_ARRAY = new double[0];
    
        DoubleArray(Array array) {
            this.array = array;
            value = EMPTY_DOUBLE_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            double[] newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDoubleArray#get(int, int, org.epics.ioc.pvAccess.DoubleArrayData)
         */
        public int get(int offset, int len, DoubleArrayData data) {
            data.data = value;
            data.offset = offset;
            if(offset+len>length) {
                len = length - offset;
                if(len<0) len =0;
            }
            return len;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDoubleArray#put(int, int, org.epics.ioc.pvAccess.DoubleArrayData)
         */
        public int put(int offset, int len, double[] from, int fromOffset) {
            if(offset+len > length) {
                int newlength = offset + len;
                if(newlength>capacity) setCapacity(newlength);
                length = newlength;
           }
           System.arraycopy(from,fromOffset,value,offset,len);
           return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvDouble;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class StringArray extends Data implements PVStringArray {
        private int length = 0;
        private int capacity = 0;
        String[] value;
        Array array;

        private final static String[] EMPTY_STRING_ARRAY = new String[0];
    
        StringArray(Array array) {
            this.array = array;
            value = EMPTY_STRING_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            String[] newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStringArray#get(int, int, org.epics.ioc.pvAccess.StringArrayData)
         */
        public int get(int offset, int len, StringArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStringArray#put(int, int, org.epics.ioc.pvAccess.StringArrayData)
         */
        public int put(int offset, int len, String[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvString;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class EnumArray extends Data implements PVEnumArray {
        private int length = 0;
        private int capacity = 0;
        PVEnum[] value;
        Array array;

        private final static PVEnum[] EMPTY_PVENUM_ARRAY = new PVEnum[0];
    
        EnumArray(Array array) {
            this.array = array;
            value = EMPTY_PVENUM_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            PVEnum[] newarray = new PVEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnumArray#get(int, int, org.epics.ioc.pvAccess.EnumArrayData)
         */
        public int get(int offset, int len, EnumArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnumArray#put(int, int, org.epics.ioc.pvAccess.EnumArrayData)
         */
        public int put(int offset, int len, PVEnum[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvEnum;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class StructureArray extends Data implements PVStructureArray {
        private int length = 0;
        private int capacity = 0;
        PVStructure[] value;
        Array array;

        private final static PVStructure[] EMPTY_PVS_ARRAY = new PVStructure[0];
    
        StructureArray(Array array) {
            this.array = array;
            value = EMPTY_PVS_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            PVStructure[] newarray = new PVStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructureArray#get(int, int, org.epics.ioc.pvAccess.StructureArrayData)
         */
        public int get(int offset, int len, StructureArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructureArray#put(int, int, org.epics.ioc.pvAccess.StructureArrayData)
         */
        public int put(int offset, int len,PVStructure[] from,int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvStructure;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
    
    private static class ArrayArray extends Data implements PVArrayArray {
        private int length = 0;
        private int capacity = 0;
        PVArray[] value;
        Array array;

        private final static PVArray[] EMPTY_PVA_ARRAY = new PVArray[0];
    
        ArrayArray(Array array) {
            this.array = array;
            value = EMPTY_PVA_ARRAY;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#getField()
         */
        public Field getField() { return array;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength(){ return length;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity(){ return capacity;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(len<=capacity) return;
            PVArray[] newarray = new PVArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArrayArray#get(int, int, org.epics.ioc.pvAccess.ArrayArrayData)
         */
        public int get(int offset, int len, ArrayArrayData data) {
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArrayArray#put(int, int, org.epics.ioc.pvAccess.ArrayArrayData)
         */
        public int put(int offset, int len, PVArray[] from, int fromOffset) {
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getElementType()
         */
        public Type getElementType() {return Type.pvArray;}
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return DatabaseExample.convert.getString(this,indentLevel)
                + super.toString(indentLevel);
        }
    }
}
