package org.epics.ioc.pvAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.pvAccess.*;

public class DatabaseExampleTest extends TestCase {
        
    public static void testBoolean() {
        DatabaseExample database = new DatabaseExample("test");
        PVBoolean booleanData = (PVBoolean)
            database.createData("boolean",Type.pvBoolean,null);
        boolean booleanValue = true;
        booleanData.put(booleanValue);
        assert(booleanData.get());
        Field field = booleanData.getField();
        assertEquals(field.getName(),"boolean");
        assertEquals(field.getType(),Type.pvBoolean);
        System.out.printf("%s type %s value %b %s\n",
            	field.getName(),
            	field.getType().toString(),
            	booleanData.get(),
                booleanData.toString());
        assertNull(field.getPropertys());
    }
        
    public static void testByte() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
        PVByte byteData = (PVByte)
            database.createData("byte",Type.pvByte,null);
        byte byteValue = -128;
        byteData.put(byteValue);
        assertEquals(byteValue,byteData.get());
        Field field = byteData.getField();
        assertEquals(field.getName(),"byte");
        assertEquals(field.getType(),Type.pvByte);
        System.out.printf("%s type %s value %d %s\n",
            	field.getName(),
            	field.getType().toString(),
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
        assertNull(field.getPropertys());
    }

    public static void testShort() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
        PVShort shortData = (PVShort)
            database.createData("short",Type.pvShort,null);
        short shortValue = 127;
        shortData.put(shortValue);
        assertEquals(shortValue,shortData.get());
        Field field = shortData.getField();
        assertEquals(field.getName(),"short");
        assertEquals(field.getType(),Type.pvShort);
        System.out.printf("%s type %s value %d %s\n",
            	field.getName(),
            	field.getType().toString(),
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
        assertNull(field.getPropertys());
    }

    public static void testInt() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
        PVInt intData = (PVInt)
            database.createData("int",Type.pvInt,null);
        int intValue = 64;
        intData.put(intValue);
        assertEquals(intValue,intData.get());
        Field field = intData.getField();
        assertEquals(field.getName(),"int");
        assertEquals(field.getType(),Type.pvInt);
        System.out.printf("%s type %s value %d %s\n",
            	field.getName(),
            	field.getType().toString(),
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
        assertNull(field.getPropertys());
    }

    public static void testLong() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
        PVLong longData = (PVLong)
            database.createData("long",Type.pvLong,null);
        long longValue = -64;
        longData.put(longValue);
        assertEquals(longValue,longData.get());
        Field field = longData.getField();
        assertEquals(field.getName(),"long");
        assertEquals(field.getType(),Type.pvLong);
        System.out.printf("%s type %s value %d %s\n",
            	field.getName(),
            	field.getType().toString(),
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
        assertNull(field.getPropertys());
    }

    public static void testFloat() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
        PVFloat floatData = (PVFloat)
            database.createData("float",Type.pvFloat,null);
        float floatValue = 32.0F;
        floatData.put(floatValue);
        assertEquals(floatValue,floatData.get());
        Field field = floatData.getField();
        assertEquals(field.getName(),"float");
        assertEquals(field.getType(),Type.pvFloat);
        System.out.printf("%s type %s value %f %s\n",
            	field.getName(),
            	field.getType().toString(),
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
        assertNull(field.getPropertys());
    }

    public static void testDouble() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
        PVDouble doubleData = (PVDouble)
            database.createData("double",Type.pvDouble,null);
        double doubleValue = 32.0;
        doubleData.put(doubleValue);
        assertEquals(doubleValue,doubleData.get());
        Field field = doubleData.getField();
        assertEquals(field.getName(),"double");
        assertEquals(field.getType(),Type.pvDouble);
        System.out.printf("%s type %s value %f %s\n",
            	field.getName(),
            	field.getType().toString(),
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
        assertNull(field.getPropertys());
    }

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
        System.out.printf("%s type %s value %s %s\n",
            	field.getName(),
            	field.getType().toString(),
            	stringData.get(),
                stringData.toString());
        assertNull(field.getPropertys());
    }

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
        System.out.printf("%s type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                enumData.toString());
    }

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
                    displayLimit.getField());
        PVDouble valueData = (PVDouble)database.createData(
            "value",Type.pvDouble,property);
        // discard data now obtained via valueData
        property = null;
        structFieldData = displayLimit.getFieldPVData();
        // set displayLimits
        assert(structFieldData.length==2);
        double value = 0.0;
        for( PVData data : structFieldData) {
            Field field = data.getField();
            assert(field.getType()==Type.pvDouble);
            PVDouble doubleData = (PVDouble)data;
            doubleData.put(value);
            value += 10.0;
        }

        Field valueField = valueData.getField();
        property = valueField.getPropertys();
        System.out.printf("\n%s type %s properties {",
             valueField.getName(),
             valueField.getType().toString());
        for(Property prop: property) {
            Field propField = prop.getField();
            System.out.printf("{name = %s fieldName = %s} ",
                prop.getName(), propField.getName());
        }
        System.out.printf(" }\n");
        Structure structure = (Structure)displayLimit.getField();
        System.out.printf("%s type %s structure name %s\n%s\n\n",
             structure.getName(),
             structure.getType().toString(),
             structure.getStructureName(),
             displayLimit.toString());
    }

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
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                booleanArrayData.toString());
        assertNull(field.getPropertys());
    }

    public static void testByteArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
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
        byte[]readback = new byte[arrayValue.length];
        int numback = byteArrayData.get(0,arrayValue.length,readback,0);
        assertEquals(numback,readback.length);
        for(int i=0; i < readback.length; i++) {
            assertEquals(arrayValue[i],readback[i]);
        }
        assertEquals(array.getElementType(),Type.pvByte);
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                byteArrayData.toString());
        assertNull(field.getPropertys());

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

    public static void testShortArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
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
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                shortArrayData.toString());
        assertNull(field.getPropertys());

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

    public static void testIntArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
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
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                intArrayData.toString());
        assertNull(field.getPropertys());

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

    public static void testLongArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
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
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                longArrayData.toString());
        assertNull(field.getPropertys());

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

    public static void testFloatArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
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
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                floatArrayData.toString());
        assertNull(field.getPropertys());

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

    public static void testDoubleArray() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getPVConvert();
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
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                doubleArrayData.toString());
        assertNull(field.getPropertys());

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
        String[]readback = new String[arrayValue.length];
        int retLength = stringArrayData.get(0,arrayValue.length,readback,0);
        assertEquals(retLength,readback.length);
        assertEquals(readback[0],arrayValue[0]);
        assertEquals(readback[1],arrayValue[1]);
        assertEquals(readback[2],arrayValue[2]);
        System.out.printf("%s type %s element type %s %s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                stringArrayData.toString());
        assertNull(field.getPropertys());
    }

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
        System.out.printf("\n%s type %s element type %s\n%s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                enumArrayData.toString());
        assertNull(field.getPropertys());
    }

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
        System.out.printf("\n%s type %s element type %s\n%s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                structureArrayData.toString());
        assertNull(field.getPropertys());
    }

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
        System.out.printf("\n%s type %s element type %s\n%s\n",
            	field.getName(),
            	field.getType().toString(),
                array.getElementType().toString(),
                arrayArrayData.toString());
        assertNull(field.getPropertys());
    }
}

