package org.epics.ioc.pvAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.pvAccess.*;

public class DatabaseExampleTest extends TestCase {
    private static DatabaseExample database = null;
    private static PVConvert convert;
    private static PVByte byteData;
    private static PVShort shortData;
    private static PVByteArray byteArrayData;
    private static byte[] byteArray;
    private static short[] shortArray;
    private static int[] intArray;
    private static long[] longArray;
    private static float[] floatArray;
    private static double[] doubleArray;

    private static void createDatabase() {
        DatabaseExample database = new DatabaseExample("test");
        convert = PVConvertFactory.getPVConvert();
        byteData = (PVByte)database.createData("byte",PVType.pvByte);
        shortData = (PVShort)database.createData("short",PVType.pvShort);
        byteArrayData = (PVByteArray)database.createArrayData(
            "byteArray",PVType.pvByte);
        byteArray = new byte[] {0,1,2};
        shortArray = new short[] {0,1,2};
        intArray = new int[] {0,1,2};
        longArray = new long[] {0,1,2};
        floatArray = new float[] {0.0F,1.0F,2.0F};
        doubleArray = new doubDatabaseExampleTest.javale[] {0.0,1.0,2.0};
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
        int intValue = byteValue;
        assertEquals(intValue,convert.toInt(byteData));
        assertNull(field.getProperties());
    }

    public static void testShort() {
        if(database==null) createDatabase();
        short shortValue = -128;
        shortData.put(shortValue);
        assertEquals(shortValue,shortData.get());
        Field field = shortData.getField();
        assertEquals(field.getName(),"short");
        assertEquals(field.getPVType(),PVType.pvShort);
        System.out.printf("%s type %s value %d\n",
            	field.getName(),
            	field.getPVType().toString(),
            	shortData.get());
        int intValue = shortValue;
        assertEquals(intValue,convert.toInt(shortData));
        assertNull(field.getProperties());
    }

    public static void testByteArray() {
        int len;
        if(database==null) createDatabase();
        byte[] byteArrayValue = new byte[] {3,4,5};
        int nput = byteArrayData.put(0,byteArrayValue.length,byteArrayValue,0);
        assertEquals(nput,byteArrayValue.length);
        len = byteArrayData.getLength();
        assertEquals(len,byteArrayValue.length);
        assertEquals(byteArrayData.getCapacity(),byteArrayValue.length);
        Field field = byteArrayData.getField();
        assertEquals(field.getName(),"byteArray");
        assertEquals(field.getPVType(),PVType.pvArray);
        Array array = (Array)field;
        assertEquals(array.getElementType(),PVType.pvByte);
        System.out.printf("%s type %s\n",
            	field.getName(),
            	field.getPVType().toString());
        assertNull(field.getProperties());
        convert.fromByteArray(byteArrayData,0,len,byteArray,0);
        assertTrue(byteArray[0]==0 && byteArray[1]==1 && byteArray[2]==2);
        convert.fromShortArray(byteArrayData,0,len,shortArray,0);
        assertTrue(shortArray[0]==0 && shortArray[1]==1 && shortArray[2]==2);
        convert.fromIntArray(byteArrayData,0,len,intArray,0);
        assertTrue(intArray[0]==0 && intArray[1]==1 && intArray[2]==2);
        convert.fromLongArray(byteArrayData,0,len,longArray,0);
        assertTrue(longArray[0]==0 && longArray[1]==1 && longArray[2]==2);
        convert.fromFloatArray(byteArrayData,0,len,floatArray,0);
        assertTrue(floatArray[0]==0.0F && floatArray[1]==1.0F && floatArray[2]==2.0F);
        convert.fromDoubleArray(byteArrayData,0,len,doubleArray,0);
        assertTrue(doubleArray[0]==0.0 && doubleArray[1]==1.0 && doubleArray[2]==2.0);
    }
}

