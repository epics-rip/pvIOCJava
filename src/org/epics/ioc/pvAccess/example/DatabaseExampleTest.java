package org.epics.ioc.pvAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.pvAccess.*;

public class DatabaseExampleTest extends TestCase {
    private static DatabaseExample database = null;
    private static PVConvert convert;
    private static PVByte   byteData;
    private static PVShort  shortData;
    private static PVInt    intData;
    private static PVLong   longData;
    private static PVFloat  floatData;
    private static PVDouble doubleData;
    private static PVByteArray   byteArrayData;
    private static PVShortArray  shortArrayData;
    private static PVIntArray    intArrayData;
    private static PVLongArray   longArrayData;
    private static PVFloatArray  floatArrayData;
    private static PVDoubleArray doubleArrayData;

    private static void createDatabase() {
        DatabaseExample database = new DatabaseExample("test");
        convert = PVConvertFactory.getPVConvert();
        byteData = (PVByte)database.createData("byte",PVType.pvByte);
        shortData = (PVShort)database.createData("short",PVType.pvShort);
        intData = (PVInt)database.createData("int",PVType.pvInt);
        longData = (PVLong)database.createData("long",PVType.pvLong);
        floatData = (PVFloat)database.createData("float",PVType.pvFloat);
        doubleData = (PVDouble)database.createData("double",PVType.pvDouble);
        byteArrayData = (PVByteArray)database.createArrayData(
            "byteArray",PVType.pvByte);
        shortArrayData = (PVShortArray)database.createArrayData(
            "shortArray",PVType.pvShort);
        intArrayData = (PVIntArray)database.createArrayData(
            "intArray",PVType.pvInt);
        longArrayData = (PVLongArray)database.createArrayData(
            "longArray",PVType.pvLong);
        floatArrayData = (PVFloatArray)database.createArrayData(
            "floatArray",PVType.pvFloat);
        doubleArrayData = (PVDoubleArray)database.createArrayData(
            "doubleArray",PVType.pvDouble);
    }

    public static void testByte() {
        if(database==null) createDatabase();
        byte byteValue = -128;
        byteData.put(byteValue);
        assertEquals(byteValue,byteData.get());
        Field field = byteData.getField();
        assertEquals(field.getName(),"byte");
        assertEquals(field.getPVType(),PVType.pvByte);
        System.out.printf("%s type %s value %d\n",
            	field.getName(),
            	field.getPVType().toString(),
            	byteData.get());
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
        assertNull(field.getProperties());
    }

    public static void testShort() {
        if(database==null) createDatabase();
        short shortValue = 127;
        shortData.put(shortValue);
        assertEquals(shortValue,shortData.get());
        Field field = shortData.getField();
        assertEquals(field.getName(),"short");
        assertEquals(field.getPVType(),PVType.pvShort);
        System.out.printf("%s type %s value %d\n",
            	field.getName(),
            	field.getPVType().toString(),
            	shortData.get());
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
        assertNull(field.getProperties());
    }

    public static void testInt() {
        if(database==null) createDatabase();
        int intValue = 64;
        intData.put(intValue);
        assertEquals(intValue,intData.get());
        Field field = intData.getField();
        assertEquals(field.getName(),"int");
        assertEquals(field.getPVType(),PVType.pvInt);
        System.out.printf("%s type %s value %d\n",
            	field.getName(),
            	field.getPVType().toString(),
            	intData.get());
        byte byteValue = (byte)intValue;
        assertEquals(intValue,convert.toByte(intData));
        short shortValue = (short)intValue;
        assertEquals(shortValue,convert.toShort(intData));
        assertEquals(intValue,convert.toInt(intData));
        long longValue = intValue;
        assertEquals(longValue,convert.toLong(intData));
        float floatValue = intValue;
        assertEquals(floatValue,convert.toFloat(intData));
        double doubleValue = intValue;
        assertEquals(doubleValue,convert.toDouble(intData));
        assertNull(field.getProperties());
    }

    public static void testLong() {
        if(database==null) createDatabase();
        long longValue = -64;
        longData.put(longValue);
        assertEquals(longValue,longData.get());
        Field field = longData.getField();
        assertEquals(field.getName(),"long");
        assertEquals(field.getPVType(),PVType.pvLong);
        System.out.printf("%s type %s value %d\n",
            	field.getName(),
            	field.getPVType().toString(),
            	longData.get());
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
        assertNull(field.getProperties());
    }

    public static void testFloat() {
        if(database==null) createDatabase();
        float floatValue = 32.0F;
        floatData.put(floatValue);
        assertEquals(floatValue,floatData.get());
        Field field = floatData.getField();
        assertEquals(field.getName(),"float");
        assertEquals(field.getPVType(),PVType.pvFloat);
        System.out.printf("%s type %s value %f\n",
            	field.getName(),
            	field.getPVType().toString(),
            	floatData.get());
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
        assertNull(field.getProperties());
    }

    public static void testDouble() {
        if(database==null) createDatabase();
        double doubleValue = 32.0;
        doubleData.put(doubleValue);
        assertEquals(doubleValue,doubleData.get());
        Field field = doubleData.getField();
        assertEquals(field.getName(),"double");
        assertEquals(field.getPVType(),PVType.pvDouble);
        System.out.printf("%s type %s value %f\n",
            	field.getName(),
            	field.getPVType().toString(),
            	doubleData.get());
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
        assertNull(field.getProperties());
    }

    public static void testByteArray() {
        int len;
        if(database==null) createDatabase();
        byte[] arrayValue = new byte[] {3,4,5};
        int nput = byteArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = byteArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(byteArrayData.getCapacity(),arrayValue.length);
        Field field = byteArrayData.getField();
        assertEquals(field.getName(),"byteArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvByte);
        System.out.printf("%s type %s element type %s\n",
            	field.getName(),
            	field.getPVType().toString(),
                array.getElementType().toString());
        assertNull(field.getProperties());

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromByteArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toByteArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    public static void testShortArray() {
        int len;
        if(database==null) createDatabase();
        short[] arrayValue = new short[] {3,4,5};
        int nput = shortArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = shortArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(shortArrayData.getCapacity(),arrayValue.length);
        Field field = shortArrayData.getField();
        assertEquals(field.getName(),"shortArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvShort);
        System.out.printf("%s type %s element type %s\n",
            	field.getName(),
            	field.getPVType().toString(),
                array.getElementType().toString());
        assertNull(field.getProperties());

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

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromShortArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toShortArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    public static void testIntArray() {
        int len;
        if(database==null) createDatabase();
        int[] arrayValue = new int[] {3,4,5};
        int nput = intArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = intArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(intArrayData.getCapacity(),arrayValue.length);
        Field field = intArrayData.getField();
        assertEquals(field.getName(),"intArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvInt);
        System.out.printf("%s type %s element type %s\n",
            	field.getName(),
            	field.getPVType().toString(),
                array.getElementType().toString());
        assertNull(field.getProperties());

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

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

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromIntArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toIntArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    public static void testLongArray() {
        int len;
        if(database==null) createDatabase();
        long[] arrayValue = new long[] {3,4,5};
        int nput = longArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = longArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(longArrayData.getCapacity(),arrayValue.length);
        Field field = longArrayData.getField();
        assertEquals(field.getName(),"longArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvLong);
        System.out.printf("%s type %s element type %s\n",
            	field.getName(),
            	field.getPVType().toString(),
                array.getElementType().toString());
        assertNull(field.getProperties());

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

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

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(floatArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(floatArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);

        arrayValue[0] = 0; arrayValue[1] = 1; arrayValue[2] = 2;
        convert.fromLongArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0; arrayValue[1] = 0; arrayValue[2] = 0;
        convert.toLongArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0 && arrayValue[1]==1 && arrayValue[2]==2);
    }

    public static void testFloatArray() {
        int len;
        if(database==null) createDatabase();
        float[] arrayValue = new float[] {3.0F,4.0F,5.0F};
        int nput = floatArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = floatArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(floatArrayData.getCapacity(),arrayValue.length);
        Field field = floatArrayData.getField();
        assertEquals(field.getName(),"floatArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvFloat);
        System.out.printf("%s type %s element type %s\n",
            	field.getName(),
            	field.getPVType().toString(),
                array.getElementType().toString());
        assertNull(field.getProperties());

        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);

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

        arrayValue[0] = 0.0F; arrayValue[1] = 1.0F; arrayValue[2] = 2.0F;
        convert.fromFloatArray(doubleArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0F; arrayValue[1] = 0.0F; arrayValue[2] = 0.0F;
        convert.toFloatArray(doubleArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0F && arrayValue[1]==1.0F && arrayValue[2]==2.0F);
    }

    public static void testDoubleArray() {
        int len;
        if(database==null) createDatabase();
        double[] arrayValue = new double[] {3.0,4.0,5.0};
        int nput = doubleArrayData.put(0,arrayValue.length,arrayValue,0);
        assertEquals(nput,arrayValue.length);
        len = doubleArrayData.getLength();
        assertEquals(len,arrayValue.length);
        assertEquals(doubleArrayData.getCapacity(),arrayValue.length);
        Field field = doubleArrayData.getField();
        assertEquals(field.getName(),"doubleArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvDouble);
        System.out.printf("%s type %s element type %s\n",
            	field.getName(),
            	field.getPVType().toString(),
                array.getElementType().toString());
        assertNull(field.getProperties());

        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(byteArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(byteArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(shortArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(shortArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(intArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(intArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

        arrayValue[0] = 0.0; arrayValue[1] = 1.0; arrayValue[2] = 2.0;
        convert.fromDoubleArray(longArrayData,0,len,arrayValue,0);
        arrayValue[0] = 0.0; arrayValue[1] = 0.0; arrayValue[2] = 0.0;
        convert.toDoubleArray(longArrayData,0,len,arrayValue,0);
        assertTrue(arrayValue[0]==0.0 && arrayValue[1]==1.0 && arrayValue[2]==2.0);

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
}

