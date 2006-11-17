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
public class MonitorTest extends TestCase {
    private static Requestor iocRequestor = null;
    /**
     * test scan.
     */
    private static MessageType maxError = MessageType.info;
    public static void testMonitor() {
        iocRequestor = new Listener();
        DBD dbd = DBDFactory.getMasterDBD();
        XMLToDBDFactory.convert(dbd,
                "src/org/epics/ioc/support/menuStructureSupportDBD.xml",
                iocRequestor);
        XMLToDBDFactory.convert(dbd,
                "src/org/epics/ioc/dbProcess/example/exampleDBD.xml",
                iocRequestor);      
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/dbProcess/example/monitorDB.xml",
             iocRequestor);
        if(!initOK) return;
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        Map<String,DBRecord> recordMap = iocdbMaster.getRecordMap();
        Set<String> keys = recordMap.keySet();
        for(String key: keys) {
            RecordProcess recordProcess = 
                recordMap.get(key).getRecordProcess();
            recordProcess.setTrace(true);
        }
        PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
        String list = periodicScanner.toString();
        System.out.println(list);
        EventScanner eventScanner = ScannerFactory.getEventScanner();
        list = eventScanner.toString();
        System.out.println(list);
        
        DBRecord dbRecord = null;
        dbRecord = iocdbMaster.findRecord("counter");
        assertNotNull(dbRecord);
        DBData[] dbData = dbRecord.getFieldDBDatas();        
        int index = dbRecord.getFieldDBDataIndex("value");
        DBData counterValue = dbData[index];
        dbRecord = iocdbMaster.findRecord("monitor00");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData monitor00Value = dbData[index];
        dbRecord = iocdbMaster.findRecord("monitor01");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData monitor01Value = dbData[index];
        dbRecord = iocdbMaster.findRecord("monitor02");
        assertNotNull(dbRecord);
        dbData = dbRecord.getFieldDBDatas();        
        index = dbRecord.getFieldDBDataIndex("value");
        DBData monitor02Value = dbData[index];
        while(true) {
            try {
                Thread.sleep(2000);
                System.out.println("  counter " + counterValue.toString());
                System.out.println("monitor00 " + monitor00Value.toString());
                System.out.println("monitor01 " + monitor01Value.toString());
                System.out.println("monitor02 " + monitor02Value.toString());
                System.out.println();
            } catch (InterruptedException e) {
            }
        }
    }
       
    private static class Listener implements Requestor {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "MonitorTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            if(messageType.ordinal()>maxError.ordinal()) maxError = messageType;
        }
    }
}
