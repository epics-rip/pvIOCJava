package org.epics.ioc.pvAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.pvAccess.*;

public class DatabaseExampleTest extends TestCase {

	public static void testDatabase() {
		DatabaseExample database = new DatabaseExample("test");
		PVByte byteData = (PVByte)database.createData("byte",PVType.pvByte);
		byte byteValue = -128;
		byteData.put(byteValue);
		assertEquals(byteValue,byteData.get());
		Field field = byteData.getField();
		assertEquals(field.getName(),"byte");
		assertEquals(field.getPVType(),PVType.pvByte);
		System.out.printf("%s type %s value %d",
				field.getName(),
				field.getPVType().toString(),
				byteData.get());
	    PVConvert convert = 	PVConvertFactory.getPVConvert();
	    int intValue = byteValue;
	    assertEquals(intValue,convert.toInt(byteData));
	    assertNull(field.getProperties());
	}
}

