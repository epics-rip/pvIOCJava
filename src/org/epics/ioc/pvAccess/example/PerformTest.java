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
public class PerformTest extends TestCase {
        

    private static final int arraySize = 1000;
    /**
     * test copy array of double.
     */
    public static void testDoubleArrayCopy() {
        DatabaseExample database = new DatabaseExample("test");
        Convert convert = ConvertFactory.getConvert();
        PVDoubleArray from = (PVDoubleArray)
            database.createArrayData("from",Type.pvDouble,null);
        PVDoubleArray to = (PVDoubleArray)
            database.createArrayData("to",Type.pvDouble,null);
        PVLongArray toLong = (PVLongArray)
        database.createArrayData("toLong",Type.pvLong,null);
        double[] data = new double[arraySize];
        double[] toData = new double[arraySize];
        for(int i=0; i<arraySize; i++) data[i] = i;
        int nput = from.put(0,data.length,data,0);
        assertEquals(nput,arraySize);
        long startTime,endTime;
        int ntimes = 10000;
        double perArray, perElement;
        startTime = System.nanoTime();
        for(int i=0; i<ntimes; i++) {
            System.arraycopy(data,0,toData,0,arraySize);
        }
        endTime = System.nanoTime();
        perArray = (double)(endTime - startTime)/(double)ntimes/1000.0;
        perElement = perArray/(double)arraySize;
        System.out.printf("data to toData perArray %f perElement %f microseconds\n",perArray,perElement);
        startTime = System.nanoTime();
        for(int i=0; i<ntimes; i++) {
            convert.copyArray(from,0,to,0,arraySize);
        }
        endTime = System.nanoTime();
        perArray = (double)(endTime - startTime)/(double)ntimes/1000.0;
        perElement = perArray/(double)arraySize;
        System.out.printf("double to double perArray %f perElement %f microseconds\n",perArray,perElement);
        startTime = System.nanoTime();
        for(int i=0; i<ntimes; i++) {
            convert.copyArray(from,0,toLong,0,arraySize);
        }
        endTime = System.nanoTime();
        perArray = (double)(endTime - startTime)/(double)ntimes/1000.0;
        perElement = perArray/(double)arraySize;
        System.out.printf("double to long perArray %f perElement %f microseconds\n",perArray,perElement);
    }

}

