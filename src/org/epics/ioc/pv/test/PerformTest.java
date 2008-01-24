/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv.test;

import junit.framework.TestCase;

import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVDataFactory;
import org.epics.ioc.pv.PVDoubleArray;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVLongArray;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;

/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PerformTest extends TestCase {
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static PVDataCreate dataCreate = PVDataFactory.getPVDataCreate();
    private static Convert convert = ConvertFactory.getConvert();
    private static final int arraySize = 1000;
    /**
     * test copy array of double.
     */
    public static void testDoubleArrayCopy() {
        Field fieldFrom = fieldCreate.createArray("from",Type.pvDouble,null);
        Field fieldTo = fieldCreate.createArray("to",Type.pvDouble,null);
        Field fieldLong = fieldCreate.createArray("long",Type.pvLong,null);
        Field[] fields = new Field[]{fieldFrom,fieldTo,fieldLong};
        Structure structure = fieldCreate.createStructure("test", "test", fields);
        PVRecord pvRecord = dataCreate.createPVRecord("test", structure);
        PVField[] pvDatas = pvRecord.getPVFields();
        PVDoubleArray from = (PVDoubleArray)pvDatas[0];
        PVDoubleArray to = (PVDoubleArray)pvDatas[1];
        PVLongArray toLong = (PVLongArray)pvDatas[2];
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
        System.out.printf("data to toData perArray %f perElement %f microseconds%n",perArray,perElement);
        startTime = System.nanoTime();
        for(int i=0; i<ntimes; i++) {
            convert.copyArray(from,0,to,0,arraySize);
        }
        endTime = System.nanoTime();
        perArray = (double)(endTime - startTime)/(double)ntimes/1000.0;
        perElement = perArray/(double)arraySize;
        System.out.printf("double to double perArray %f perElement %f microseconds%n",perArray,perElement);
        startTime = System.nanoTime();
        for(int i=0; i<ntimes; i++) {
            convert.copyArray(from,0,toLong,0,arraySize);
        }
        endTime = System.nanoTime();
        perArray = (double)(endTime - startTime)/(double)ntimes/1000.0;
        perElement = perArray/(double)arraySize;
        System.out.printf("double to long perArray %f perElement %f microseconds%n",perArray,perElement);
    }
    
    public static void testCurentTime() {
        long startTime,endTime;
        int ntimes = 10000;
        startTime = System.nanoTime();
        for(int i=0; i<ntimes; i++) {
            long time = System.currentTimeMillis();
        }
        endTime = System.nanoTime();
        double perCall = (double)(endTime - startTime)/(double)ntimes/1000.0;
        System.out.printf("currentTimeMillis %f microseconds%n",perCall);
    }

}

