/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import junit.framework.TestCase;

import java.util.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.util.*;

/**
 * JUnit test for scan test.
 * @author mrk
 *
 */
public class ScanTest extends TestCase {
    private static IOCMessageListener iocMessageListener = null;
    /**
     * test scan.
     */
    private static IOCMessageType maxError = IOCMessageType.info;
    public static void testScan() {
        iocMessageListener = new Listener();
        DBD dbd = DBDFactory.getMasterDBD();
        XMLToDBDFactory.convert(dbd,
                "src/org/epics/ioc/support/menuStructureSupportDBD.xml",
                iocMessageListener);
        XMLToDBDFactory.convert(dbd,
                "src/org/epics/ioc/dbProcess/example/exampleDBD.xml",
                iocMessageListener);
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        boolean initOK = IOCFactory.initDatabase("src/org/epics/ioc/dbProcess/example/scanDB.xml",iocMessageListener);
        if(!initOK) return;
        
//        Map<String,DBRecord> recordMap  recordMap = iocdbAdd.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            RecordProcess recordProcess = 
//                recordMap.get(key).getRecordProcess();
//            recordProcess.setTrace(true);
//        }
        PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
        String list = periodicScanner.toString();
        System.out.println(list);
        EventScanner eventScanner = ScannerFactory.getEventScanner();
        list = eventScanner.toString();
        System.out.println(list);
        
        DBRecord dbRecord = null;
        dbRecord = iocdbMaster.findRecord("counterPush");
        assertNotNull(dbRecord);
        DBData[] dbData = dbRecord.getFieldDBDatas();        
        int index = dbRecord.getFieldDBDataIndex("value");
        DBData counterPushValue = dbData[index];
        dbRecord = iocdbMaster.findRecord("doubleReceive09");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData doubleReceive09Value = dbData[index];
        dbRecord = iocdbMaster.findRecord("counterEvent0");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData counterEvent0Value = dbData[index];
        dbRecord = iocdbMaster.findRecord("counterEvent1");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData counterEvent1Value = dbData[index];
        for(int i=0; i<5; i++) {
            try {
                Thread.sleep(1000);
                System.out.println("    counterPush " + counterPushValue.toString());
                System.out.println("doubleReceive09 " + doubleReceive09Value.toString());
                System.out.println("  counterEvent0 " + counterEvent0Value.toString());
                System.out.println("  counterEvent1 " + counterEvent1Value.toString());
                System.out.println();
            } catch (InterruptedException e) {
            }
        }
        initOK = IOCFactory.initDatabase("src/org/epics/ioc/dbProcess/example/scanDB.xml",iocMessageListener);
        if(!initOK) {
            System.out.println("IOCFactory.initDatabase failed");
        }
        initOK = IOCFactory.initDatabase("src/org/epics/ioc/dbProcess/example/scanAddDB.xml",iocMessageListener);
        if(!initOK) {
            System.out.println("IOCFactory.initDatabase failed");
        }
        String[] recordList = iocdbMaster.recordList(".*");
        System.out.print("record list");
        for(int i=0; i<recordList.length; i++) {
            if((i+1)%5 == 0) {
                System.out.println();
                System.out.print("    ");
            } else {
                System.out.print(" ");
            }
            System.out.print(recordList[i]);
        }
        System.out.println();
        dbRecord = iocdbMaster.findRecord("counter");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData counterValue = dbData[index];
        dbRecord = iocdbMaster.findRecord("double02");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData double02Value = dbData[index];
        list = periodicScanner.toString();
        System.out.println(list);
        while(true) {
            try {
                Thread.sleep(1000);
                System.out.println("    counterPush " + counterPushValue.toString());
                System.out.println("doubleReceive09 " + doubleReceive09Value.toString());
                System.out.println("  counterEvent0 " + counterEvent0Value.toString());               
                System.out.println("  counterEvent1 " + counterEvent1Value.toString());
                System.out.println(" counter " + counterValue.toString());
                System.out.println("double02 " + double02Value.toString());
                System.out.println();
            } catch (InterruptedException e) {
            }
        }
    }
       
    private static class Listener implements IOCMessageListener {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCMessageListener#message(java.lang.String, org.epics.ioc.util.IOCMessageType)
         */
        public void message(String message, IOCMessageType messageType) {
            System.out.println(message);
            if(messageType.ordinal()>maxError.ordinal()) maxError = messageType;
        }
    }
}
