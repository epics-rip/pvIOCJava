/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.pvAccess.*;

/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class DatabaseExampleTest extends TestCase {
        
    /**
     * test boolean.
     */
    public static void testBoolean() {
        DatabaseExample database = new DatabaseExample("test");
        PVBoolean booleanData = (PVBoolean)
            database.createData("boolean",Type.pvBoolean,null);
        boolean booleanValue = true;
        booleanData.put(booleanValue);
        assertTrue(booleanData.get());
        assertNull(booleanData.setSupportName("booleanSupport"));
        Field field = booleanData.getField();
        assertEquals(field.getName(),"boolean");
        assertEquals(field.getType(),Type.pvBoolean);
        System.out.printf("%s\nvalue %b %s\n",
            	field.toString(),
            	booleanData.get(),
                booleanData.toString());
        assertEquals(field.getPropertys().length,0);
    }
        
    /**
     * test byte.
     */
    public static void testByte() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVByte byteData = (PVByte)
            database.createData("byte",Type.pvByte,null);
        byte byteValue = -128;
        byteData.put(byteValue);
        assertEquals(byteValue,byteData.get());
        Field field = byteData.getField();
        assertEquals(field.getName(),"byte");
        assertEquals(field.getType(),Type.pvByte);
        System.out.printf("%s\nvalue %d %s\n",
            	field.toString(),
            	byteData.get(),
                byteData.toString());
        assertEquals(byteValue,convert.toByte(byteData));
        short shortValue = byteValue;
        assertEquals(shortValue,convert.toShort(byteData));
        int intValue = byteValue;
        assertEquals(intValue,convert.toInt(byteData));
        long longValue = byteValue;
        assertEquals(longValue,convert.toLong(byteData));
        float floatValue = byteValue;
        assertEquals(floatValue,convert.toFloat(byteData));
        double doubleValue = byteValue;
        assertEquals(doubleValue,convert.toDouble(byteData));
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test short.
     */
    public static void testShort() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVShort shortData = (PVShort)
            database.createData("short",Type.pvShort,null);
        short shortValue = 127;
        shortData.put(shortValue);
        assertEquals(shortValue,shortData.get());
        Field field = shortData.getField();
        assertEquals(field.getName(),"short");
        assertEquals(field.getType(),Type.pvShort);
        System.out.printf("%s\nvalue %d %s\n",
            	field.toString(),
            	shortData.get(),
                shortData.toString());
        byte byteValue = (byte)shortValue;
        assertEquals(byteValue,convert.toByte(shortData));
        assertEquals(shortValue,convert.toShort(shortData));
        int intValue = shortValue;
        assertEquals(intValue,convert.toInt(shortData));
        long longValue = shortValue;
        assertEquals(longValue,convert.toLong(shortData));
        float floatValue = shortValue;
        assertEquals(floatValue,convert.toFloat(shortData));
        double doubleValue = shortValue;
        assertEquals(doubleValue,convert.toDouble(shortData));
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test int.
     */
    public static void testInt() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVInt intData = (PVInt)
            database.createData("int",Type.pvInt,null);
        int intValue = 64;
        intData.put(intValue);
        assertEquals(intValue,intData.get());
        Field field = intData.getField();
        assertEquals(field.getName(),"int");
        assertEquals(field.getType(),Type.pvInt);
        System.out.printf("%s\nvalue %d %s\n",
            	field.toString(),
            	intData.get(),
                intData.toString());
        byte byteValue = (byte)intValue;
        assertEquals(byteValue,convert.toByte(intData));
        short shortValue = (short)intValue;
        assertEquals(shortValue,convert.toShort(intData));
        assertEquals(intValue,convert.toInt(intData));
        long longValue = intValue;
        assertEquals(longValue,convert.toLong(intData));
        float floatValue = intValue;
        assertEquals(floatValue,convert.toFloat(intData));
        double doubleValue = intValue;
        assertEquals(doubleValue,convert.toDouble(intData));
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test long.
     */
    public static void testLong() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVLong longData = (PVLong)
            database.createData("long",Type.pvLong,null);
        long longValue = -64;
        longData.put(longValue);
        assertEquals(longValue,longData.get());
        Field field = longData.getField();
        assertEquals(field.getName(),"long");
        assertEquals(field.getType(),Type.pvLong);
        System.out.printf("%s\nvalue %d %s\n",
            	field.toString(),
            	longData.get(),
                longData.toString());
        byte byteValue = (byte)longValue;
        assertEquals(byteValue,convert.toByte(longData));
        short shortValue = (short)longValue;
        assertEquals(shortValue,convert.toShort(longData));
        int intValue = (int)longValue;
        assertEquals(intValue,convert.toInt(longData));
        assertEquals(longValue,convert.toLong(longData));
        float floatValue = longValue;
        assertEquals(floatValue,convert.toFloat(longData));
        double doubleValue = longValue;
        assertEquals(doubleValue,convert.toDouble(longData));
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test float.
     */
    public static void testFloat() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVFloat floatData = (PVFloat)
            database.createData("float",Type.pvFloat,null);
        float floatValue = 32.0F;
        floatData.put(floatValue);
        assertEquals(floatValue,floatData.get());
        Field field = floatData.getField();
        assertEquals(field.getName(),"float");
        assertEquals(field.getType(),Type.pvFloat);
        System.out.printf("%s\nvalue %f %s\n",
            	field.toString(),
            	floatData.get(),
                floatData.toString());
        byte byteValue = (byte)floatValue;
        assertEquals(byteValue,convert.toByte(floatData));
        short shortValue = (short)floatValue;
        assertEquals(shortValue,convert.toShort(floatData));
        int intValue = (int)floatValue;
        assertEquals(intValue,convert.toInt(floatData));
        long longValue = (long)floatValue;
        assertEquals(longValue,convert.toLong(floatData));
        assertEquals(floatValue,convert.toFloat(floatData));
        double doubleValue = floatValue;
        assertEquals(doubleValue,convert.toDouble(floatData));
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test double.
     */
    public static void testDouble() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVDouble doubleData = (PVDouble)
            database.createData("double",Type.pvDouble,null);
        double doubleValue = 32.0;
        doubleData.put(doubleValue);
        assertEquals(doubleValue,doubleData.get());
        Field field = doubleData.getField();
        assertEquals(field.getName(),"double");
        assertEquals(field.getType(),Type.pvDouble);
        System.out.printf("%s\nvalue %f %s\n",
            	field.toString(),
            	doubleData.get(),
                doubleData.toString());
        byte byteValue = (byte)doubleValue;
        assertEquals(byteValue,convert.toByte(doubleData));
        short shortValue = (short)doubleValue;
        assertEquals(shortValue,convert.toShort(doubleData));
        int intValue = (int)doubleValue;
        assertEquals(intValue,convert.toInt(doubleData));
        long longValue = (long)doubleValue;
        assertEquals(longValue,convert.toLong(doubleData));
        float floatValue = (float)doubleValue;
        assertEquals(floatValue,convert.toFloat(doubleData));
        assertEquals(doubleValue,convert.toDouble(doubleData));
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test string.
     */
    public static void testString() {
        DatabaseExample database = new DatabaseExample("test");
        PVString stringData = (PVString)
            database.createData("string",Type.pvString,null);
        String stringValue = "string";
        stringData.put(stringValue);
        assertEquals(stringValue,stringData.get());
        Field field = stringData.getField();
        assertEquals(field.getName(),"string");
        assertEquals(field.getType(),Type.pvString);
        System.out.printf("%s\nvalue %s %s\n",
            	field.toString(),
            	stringData.get(),
                stringData.toString());
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test enum.
     */
    public static void testEnum() {
        DatabaseExample database = new DatabaseExample("test");
        PVEnum enumData = database.createEnumData("enum",true,null);
        String[] choice = new String[] {"state0","state1"};
        enumData.setChoices(choice);
        Field field = enumData.getField();
        assertEquals(field.getName(),"enum");
        assertEquals(field.getType(),Type.pvEnum);
        String[] readback = enumData.getChoices();
        assertEquals(readback[0],choice[0]);
        assertEquals(readback[1],choice[1]);
        enumData.setIndex(1);
        assertEquals(1,enumData.getIndex());
        System.out.printf("%s\nvalue %s\n",
            	field.toString(),
                enumData.toString());
    }

    /**
     * test structure and property.
     */
    public static void testStructureAndProperty() {
        DatabaseExample database = new DatabaseExample("test");
        // value has property displayLimit
        PVDouble low = (PVDouble)database.createData(
            "low",Type.pvDouble,null);
        PVDouble high = (PVDouble)database.createData(
            "high",Type.pvDouble,null);
        PVData[] structFieldData = new PVData[] {low,high};
        PVStructure displayLimit = database.createStructureData(
            "displayLimit","DisplayLimit",structFieldData,null);
        // discard data now obtained via displayLimit
        low = null; high = null; structFieldData = null;
        Property[] property = new Property[1];
        property[0] = FieldFactory.createProperty("displayLimit",
            "displayLimit");
        PVDouble valueData = (PVDouble)database.createData(
            "value",Type.pvDouble,property);
        // discard data now obtained via valueData
        property = null;
        structFieldData = displayLimit.getFieldPVDatas();
        // set displayLimits
        assertTrue(structFieldData.length==2);
        double value = 0.0;
        for( PVData data : structFieldData) {
            Field field = data.getField();
            assertTrue(field.getType()==Type.pvDouble);
            PVDouble doubleData = (PVDouble)data;
            doubleData.put(value);
            value += 10.0;
        }

        Field valueField = valueData.getField();
        System.out.printf("%s\nvalue %f %s\n",
             valueField.toString(),
             valueData.get(),
             valueData.toString());
        Structure structure = (Structure)displayLimit.getField();
        System.out.printf("%s\nvalue %s\n",
             structure.toString(),
             displayLimit.toString());
        
        // now create a structure with names and fields specified later
        PVStructure displayLimitDelay = database.createStructureData(
                "displayLimitDelay",null);
        structure = (Structure)displayLimitDelay.getField();
        Field field = structure.getField("junk");
        assertNull(field);
        int index = structure.getFieldIndex("junk");
        assertTrue(index<0);
        Field[] fields = structure.getFields();
        assertTrue(fields.length==0);
        String[] names = structure.getFieldNames();
        assertTrue(names.length==0);
        String structureName = structure.getStructureName();
        assertNull(structureName);
        System.out.printf("\n%s\nvalue %s\n",
             structure.toString(),
             displayLimitDelay.toString());
    }

    /**
     * test array of boolean.
     */
    public static void testBooleanArray() {
        DatabaseExample database = new DatabaseExample("test");
        PVBooleanArray booleanArrayData = (PVBooleanArray)
            database.createArrayData("booleanArray",Type.pvBoolean,null);
        int len;
        boolean[] arrayValue = new boolean[] {true,false,true};
        int nput = booleanArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = booleanArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(booleanArrayData.getCapacity(),arrayValue.length);
        Field field = booleanArrayData.getField();
        assertEquals(field.getName(),"booleanArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvBoolean);
        System.out.printf("%s\nvalue %s\n",
            	array.toString(),
                booleanArrayData.toString());
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test array of byte.
     */
    public static void testByteArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVByteArray byteArrayData = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int len;
        byte[] arrayValue = new byte[] {3,4,5};
        int nput = byteArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = byteArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(byteArrayData.getCapacity(),arrayValue.length);
        Field field = byteArrayData.getField();
        assertEquals(field.getName(),"byteArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        ByteArrayData data = new ByteArrayData();
        int numback = byteArrayData.get(0,arrayValue.length,data);
        byte[] readback = data.data;
        assertEquals(data.offset,0);
        assertEquals(numback,readback.length);
        for(int i=0; i < readback.length; i++) {
            assertEquals(arrayValue[i],readback[i]);
        }
        assertEquals(array.getElementType(),Type.pvByte);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                byteArrayData.toString());
        assertEquals(field.getPropertys().length,0);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVShortArray shortArrayData = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVIntArray intArrayData = (PVIntArray)
            database.createArrayData("intArray",Type.pvInt,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVLongArray longArrayData = (PVLongArray)
            database.createArrayData("longArray",Type.pvLong,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVFloatArray floatArrayData = (PVFloatArray)
            database.createArrayData("floatArray",Type.pvFloat,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVDoubleArray doubleArrayData = (PVDoubleArray)
            database.createArrayData("doubleArray",Type.pvDouble,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    /**
     * test array of short.
     */
    public static void testShortArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVShortArray shortArrayData = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        int len;
        short[] arrayValue = new short[] {3,4,5};
        int nput = shortArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = shortArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(shortArrayData.getCapacity(),arrayValue.length);
        Field field = shortArrayData.getField();
        assertEquals(field.getName(),"shortArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvShort);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                shortArrayData.toString());
        assertEquals(field.getPropertys().length,0);

        PVByteArray byteArrayData = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVIntArray intArrayData = (PVIntArray)
            database.createArrayData("intArray",Type.pvInt,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVLongArray longArrayData = (PVLongArray)
            database.createArrayData("longArray",Type.pvLong,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVFloatArray floatArrayData = (PVFloatArray)
            database.createArrayData("floatArray",Type.pvFloat,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVDoubleArray doubleArrayData = (PVDoubleArray)
            database.createArrayData("doubleArray",Type.pvDouble,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    /**
     * test array of int.
     */
    public static void testIntArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVIntArray intArrayData = (PVIntArray)
            database.createArrayData("intArray",Type.pvInt,null);
        int len;
        int[] arrayValue = new int[] {3,4,5};
        int nput = intArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = intArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(intArrayData.getCapacity(),arrayValue.length);
        Field field = intArrayData.getField();
        assertEquals(field.getName(),"intArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvInt);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                intArrayData.toString());
        assertEquals(field.getPropertys().length,0);

        PVByteArray byteArrayData = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVShortArray shortArrayData = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVLongArray longArrayData = (PVLongArray)
            database.createArrayData("longArray",Type.pvLong,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVFloatArray floatArrayData = (PVFloatArray)
            database.createArrayData("floatArray",Type.pvFloat,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVDoubleArray doubleArrayData = (PVDoubleArray)
            database.createArrayData("doubleArray",Type.pvDouble,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    /**
     * test array of long.
     */
    public static void testLongArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVLongArray longArrayData = (PVLongArray)
            database.createArrayData("longArray",Type.pvLong,null);
        int len;
        long[] arrayValue = new long[] {3,4,5};
        int nput = longArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = longArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(longArrayData.getCapacity(),arrayValue.length);
        Field field = longArrayData.getField();
        assertEquals(field.getName(),"longArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvLong);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                longArrayData.toString());
        assertEquals(field.getPropertys().length,0);

        PVByteArray byteArrayData = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVShortArray shortArrayData = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVIntArray intArrayData = (PVIntArray)
            database.createArrayData("intArray",Type.pvInt,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVFloatArray floatArrayData = (PVFloatArray)
            database.createArrayData("floatArray",Type.pvFloat,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        PVDoubleArray doubleArrayData = (PVDoubleArray)
            database.createArrayData("doubleArray",Type.pvDouble,null);
        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    /**
     * test array of float.
     */
    public static void testFloatArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVFloatArray floatArrayData = (PVFloatArray)
            database.createArrayData("floatArray",Type.pvFloat,null);
        int len;
        float[] arrayValue = new float[] {3.0F,4.0F,5.0F};
        int nput = floatArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = floatArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(floatArrayData.getCapacity(),arrayValue.length);
        Field field = floatArrayData.getField();
        assertEquals(field.getName(),"floatArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvFloat);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                floatArrayData.toString());
        assertEquals(field.getPropertys().length,0);

        PVByteArray byteArrayData = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        PVShortArray shortArrayData = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        PVIntArray intArrayData = (PVIntArray)
            database.createArrayData("intArray",Type.pvInt,null);
        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        PVLongArray longArrayData = (PVLongArray)
            database.createArrayData("longArray",Type.pvLong,null);
        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        PVDoubleArray doubleArrayData = (PVDoubleArray)
            database.createArrayData("doubleArray",Type.pvDouble,null);
        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);
    }

    /**
     * test array of double.
     */
    public static void testDoubleArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVDoubleArray doubleArrayData = (PVDoubleArray)
            database.createArrayData("doubleArray",Type.pvDouble,null);
        int len;
        double[] arrayValue = new double[] {3.0,4.0,5.0};
        int nput = doubleArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = doubleArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(doubleArrayData.getCapacity(),arrayValue.length);
        Field field = doubleArrayData.getField();
        assertEquals(field.getName(),"doubleArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvDouble);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                doubleArrayData.toString());
        assertEquals(field.getPropertys().length,0);

        PVByteArray byteArrayData = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        PVShortArray shortArrayData = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        PVIntArray intArrayData = (PVIntArray)
            database.createArrayData("intArray",Type.pvInt,null);
        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        PVLongArray longArrayData = (PVLongArray)
            database.createArrayData("longArray",Type.pvLong,null);
        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        PVFloatArray floatArrayData = (PVFloatArray)
            database.createArrayData("floatArray",Type.pvFloat,null);
        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);
    }

    /**
     * test array of string.
     */
    public static void testStringArray() {
        DatabaseExample database = new DatabaseExample("test");
        PVStringArray stringArrayData = (PVStringArray)
            database.createArrayData("stringArray",Type.pvString,null);
        int len;
        String[] arrayValue = new String[] {"string0",null,"string2"};
        int nput = stringArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = stringArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(stringArrayData.getCapacity(),arrayValue.length);
        Field field = stringArrayData.getField();
        assertEquals(field.getName(),"stringArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvString);
        StringArrayData data = new StringArrayData();
        int retLength = stringArrayData.get(0,arrayValue.length,data);
        String[]readback = data.data;
        assertEquals(data.offset,0);
        assertEquals(retLength,readback.length);
        assertEquals(readback[0],arrayValue[0]);
        assertEquals(readback[1],arrayValue[1]);
        assertEquals(readback[2],arrayValue[2]);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                stringArrayData.toString());
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test array of enum.
     */
    public static void testEnumArray() {
        DatabaseExample database = new DatabaseExample("test");
        PVEnumArray enumArrayData = (PVEnumArray)
            database.createArrayData("enumArray",Type.pvEnum,null);
        int len;
        PVEnum[] arrayValue = new PVEnum[3];
        arrayValue[0] = database.createEnumData("enum0",true,null);
        String[]choice = new String[] {"state0Choice0","state0Choice1"};
        arrayValue[0].setChoices(choice);
        arrayValue[2] = database.createEnumData("enum2",true,null);
        choice = new String[] {"state2Choice0","state2Choice1"};
        arrayValue[2].setChoices(choice);
        int nput = enumArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = enumArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(enumArrayData.getCapacity(),arrayValue.length);
        Field field = enumArrayData.getField();
        assertEquals(field.getName(),"enumArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvEnum);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                enumArrayData.toString());
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test array of structure.
     */
    public static void testStructureArray() {
        DatabaseExample database = new DatabaseExample("test");
        PVStructureArray structureArrayData = (PVStructureArray)
            database.createArrayData("structureArray",Type.pvStructure,null);
        int len;
        PVStructure[] arrayValue = new PVStructure[3];
        for(int i = 0; i < arrayValue.length; i+=2) {
            PVDouble low = (PVDouble)database.createData(
                "low",Type.pvDouble,null);
            low.put(-10.0 * i);
            PVDouble high = (PVDouble)database.createData(
                "high",Type.pvDouble,null);
            high.put(10.0 * i);
            PVData[] structFieldData = new PVData[] {low,high};
            arrayValue[i] = database.createStructureData(
                "displayLimit","DisplayLimit",structFieldData,null);
        }
        int nput = structureArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = structureArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(structureArrayData.getCapacity(),arrayValue.length);
        Field field = structureArrayData.getField();
        assertEquals(field.getName(),"structureArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvStructure);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                structureArrayData.toString());
        assertEquals(field.getPropertys().length,0);
    }

    /**
     * test array of array.
     */
    public static void testArrayArray() {
        DatabaseExample database = new DatabaseExample("test");
        PVArrayArray arrayArrayData = (PVArrayArray)
            database.createArrayData("arrayArray",Type.pvArray,null);
        int len;
        PVArray[] arrayValue = new PVArray[3];
        for(int i = 0; i < arrayValue.length; i+=2) {
            PVDoubleArray doubleArrayData = (PVDoubleArray)
                database.createArrayData("doubleArray",Type.pvDouble,null);
            double[] value = new double[] {1.0*i,2.0*i,3.0*i};
            doubleArrayData.put(0,arrayValue.length,value,0);
            arrayValue[i] = doubleArrayData;
        }
        int nput = arrayArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = arrayArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(arrayArrayData.getCapacity(),arrayValue.length);
        Field field = arrayArrayData.getField();
        assertEquals(field.getName(),"arrayArray");
        assertEquals(field.getType(),Type.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),Type.pvArray);
        System.out.printf("%s\nvalue %s\n",
                array.toString(),
                arrayArrayData.toString());
        assertEquals(field.getPropertys().length,0);
    }
    
    /**
     * Test structure copy.
     */
    public static void testStructureCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        // value has property displayLimit
        PVDouble low = (PVDouble)database.createData(
            "low",Type.pvDouble,null);
        low.put(-10.0);
        PVDouble high = (PVDouble)database.createData(
            "high",Type.pvDouble,null);
        high.put(10.0);
        PVData[] structFieldData = new PVData[] {low,high};
        PVStructure displayLimit = database.createStructureData(
            "displayLimit","DisplayLimit",structFieldData,null);
        Structure structure = (Structure)displayLimit.getField();
        low = (PVDouble)database.createData("low",Type.pvDouble,null);
        high = (PVDouble)database.createData("high",Type.pvDouble,null);
        structFieldData = new PVData[] {low,high};
        PVStructure displayLimit1 = database.createStructureData(
                "displayLimit1",structure,structFieldData,null);
        convert.copyStructure(displayLimit,displayLimit1);
        System.out.printf("\ntestStructureCopy %s%s\n",
                displayLimit.toString(),displayLimit1.toString());
    }
    
    /**
     * Test BooleanArray copy.
     */
    public static void testBooleanArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVBooleanArray sourceArray = (PVBooleanArray)
            database.createArrayData("sourceArray",Type.pvBoolean,null);
        boolean[] arrayValue = new boolean[] {true,false,true};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestBooleanArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVBooleanArray booleanArray = (PVBooleanArray)
            database.createArrayData("booleanArray",Type.pvBoolean,null);
        int ncopy = convert.copyArray(sourceArray,0,booleanArray,0,nput);
        System.out.printf("booleanArray ncopy %d %s\n",ncopy,booleanArray.toString());
    }
    
    /**
     * Test ByteArray copy.
     */
    public static void testByteArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVByteArray sourceArray = (PVByteArray)
            database.createArrayData("sourceArray",Type.pvByte,null);
        byte[] arrayValue = new byte[] {-127,0,127};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestByteArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());        
    }
    
    /**
     * Test ShortArray copy.
     */
    public static void testShortArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVShortArray sourceArray = (PVShortArray)
            database.createArrayData("sourceArray",Type.pvShort,null);
        short[] arrayValue = new short[] {-32767,0,32767};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestShortArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());        
    }
    
    /**
     * Test IntArray Copy.
     */
    public static void testIntArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVIntArray sourceArray = (PVIntArray)
            database.createArrayData("sourceArray",Type.pvInt,null);
        int[] arrayValue = new int[] {-100000,0,100000};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestIntArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());        
    }
    
    /**
     * Test LongArray copy.
     */
    public static void testLongArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVLongArray sourceArray = (PVLongArray)
            database.createArrayData("sourceArray",Type.pvLong,null);
        long[] arrayValue = new long[] {-100,0,100};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestLongArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());        
    }
    
    /**
     * Test FloatArray copy.
     */
    public static void testFloatArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVFloatArray sourceArray = (PVFloatArray)
            database.createArrayData("sourceArray",Type.pvFloat,null);
        float[] arrayValue = new float[] {-127,0,127};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestFloatArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());        
    }
    
    /**
     * Test DoubleArray copy.
     */
    public static void testDoubleArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVDoubleArray sourceArray = (PVDoubleArray)
            database.createArrayData("sourceArray",Type.pvDouble,null);
        double[] arrayValue = new double[] {-127,0,127};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestDoubleArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());        
    }
    
    /**
     * Test StringArray copy.
     */
    public static void testStringArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVStringArray sourceArray = (PVStringArray)
            database.createArrayData("sourceArray",Type.pvString,null);
        String[] arrayValue = new String[] {"-127","0","127"};
        int nput = sourceArray.put(0,arrayValue.length,arrayValue,0);
        System.out.printf("\ntestStringArrayCopy nput %d sourceArray %s\n",
            nput,sourceArray.toString());
        
        PVStringArray stringArray = (PVStringArray)
        database.createArrayData("stringArray",Type.pvString,null);
        
        PVByteArray byteArray = (PVByteArray)
            database.createArrayData("byteArray",Type.pvByte,null);
        int ncopy = convert.copyArray(sourceArray,0,byteArray,0,nput);
        System.out.printf("byteArray ncopy %d %s\n",ncopy,byteArray.toString());
        ncopy = convert.copyArray(byteArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d %s\n",ncopy,stringArray.toString());
    
        PVShortArray shortArray = (PVShortArray)
            database.createArrayData("shortArray",Type.pvShort,null);
        ncopy = convert.copyArray(sourceArray,0,shortArray,0,nput);
        System.out.printf("shortArray ncopy %d %s\n",ncopy, shortArray.toString());
        ncopy = convert.copyArray(shortArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d %s\n",ncopy,stringArray.toString());

        PVIntArray intArray = (PVIntArray)
        database.createArrayData("intArray",Type.pvInt,null);
        ncopy = convert.copyArray(sourceArray,0,intArray,0,nput);
        System.out.printf("intArray ncopy %d %s\n",ncopy,intArray.toString()); 
        ncopy = convert.copyArray(intArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d %s\n",ncopy,stringArray.toString());
        
        PVLongArray longArray = (PVLongArray)
        database.createArrayData("longArray",Type.pvLong,null);
        ncopy = convert.copyArray(sourceArray,0,longArray,0,nput);
        System.out.printf("longArray ncopy %d %s\n",ncopy,longArray.toString());
        ncopy = convert.copyArray(longArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d %s\n",ncopy,stringArray.toString());
        
        PVFloatArray floatArray = (PVFloatArray)
        database.createArrayData("floatArray",Type.pvFloat,null);
        ncopy = convert.copyArray(sourceArray,0,floatArray,0,nput);
        System.out.printf("floatArray ncopy %d %s\n",ncopy,floatArray.toString());
        ncopy = convert.copyArray(floatArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d %s\n",ncopy,stringArray.toString());
        
        PVDoubleArray doubleArray = (PVDoubleArray)
        database.createArrayData("doubleArray",Type.pvDouble,null);
        ncopy = convert.copyArray(sourceArray,0,doubleArray,0,nput);
        System.out.printf("doubleArray ncopy %d %s\n",ncopy,doubleArray.toString());  
        ncopy = convert.copyArray(doubleArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d %s\n",ncopy,stringArray.toString());
    }
    /**
     * Test StructureArray copy.
     */
    public static void testStructureArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVStructureArray structureArray = (PVStructureArray)
            database.createArrayData("structureArray",Type.pvStructure,null);
        PVStructure[] arrayValue = new PVStructure[3];
        for(int i = 0; i < arrayValue.length; i+=2) {
            PVDouble low = (PVDouble)database.createData(
                "low",Type.pvDouble,null);
            low.put(-10.0 * (i+1));
            PVDouble high = (PVDouble)database.createData(
                "high",Type.pvDouble,null);
            high.put(10.0 * (i+1));
            PVData[] structFieldData = new PVData[] {low,high};
            arrayValue[i] = database.createStructureData(
                "displayLimit","DisplayLimit",structFieldData,null);
        }
        int nput = structureArray.put(0,arrayValue.length,arrayValue,0);
        PVStructureArray structureArray1 = (PVStructureArray)
            database.createArrayData("structureArray1",Type.pvStructure,null);
        int ncopy = convert.copyArray(structureArray,0,structureArray1,0,nput);
        System.out.printf("\ntestStructureArrayCopy ncopy %d %s%s\n",
            ncopy,structureArray.toString(),structureArray1.toString());
        PVStringArray stringArray = (PVStringArray) database.createArrayData(
             "stringArray",Type.pvString,null);
        ncopy = convert.copyArray(structureArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d\n%s\n",ncopy,stringArray.toString());
        
    }
    /**
     * Test ArrayArray copy.
     */
    public static void testArrayArrayCopy() {
        Convert convert = ConvertFactory.getConvert();
        DatabaseExample database = new DatabaseExample("test");
        PVArrayArray arrayArray = (PVArrayArray)
            database.createArrayData("arrayArray",Type.pvArray,null);
        PVArray[] arrayValue = new PVArray[3];
        for(int i = 0; i < arrayValue.length; i+=2) {
            PVDoubleArray doubleArrayData = (PVDoubleArray)
                database.createArrayData("doubleArray",Type.pvDouble,null);
            double[] value = new double[] {1.0*(i+1),2.0*(i+1),3.0*(i+1)};
            doubleArrayData.put(0,arrayValue.length,value,0);
            arrayValue[i] = doubleArrayData;
        }
        int nput = arrayArray.put(0,arrayValue.length,arrayValue,0);
        PVArrayArray arrayArray1 = (PVArrayArray)
        database.createArrayData("arrayArray1",Type.pvArray,null);
        int ncopy = convert.copyArray(arrayArray,0,arrayArray1,0,nput);
        System.out.printf("\ntestArrayArrayCopy ncopy %d %s%s\n",
                ncopy,arrayArray.toString(),arrayArray1.toString());
        PVStringArray stringArray = (PVStringArray) database.createArrayData(
             "stringArray",Type.pvString,null);
        ncopy = convert.copyArray(arrayArray,0,stringArray,0,nput);
        System.out.printf("stringArray ncopy %d\n%s\n",ncopy,stringArray.toString());
        
    }
}

