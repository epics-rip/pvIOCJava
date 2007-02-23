/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process.test;

import junit.framework.TestCase;

import java.util.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.db.*;
import org.epics.ioc.db.test.DBListenerForTesting;
import org.epics.ioc.process.*;
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
                "dbd/menuStructureSupport.xml",
                iocRequestor);
        XMLToDBDFactory.convert(dbd,
                "src/org/epics/ioc/process/test/exampleDBD.xml",
                iocRequestor);      
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/process/test/monitorDB.xml",
             iocRequestor);
        if(!initOK) return;
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        Map<String,DBRecord> recordMap = iocdbMaster.getRecordMap();
        Set<String> keys = recordMap.keySet();
        for(String key: keys) {
            RecordProcess recordProcess = 
                recordMap.get(key).getRecordProcess();
//            recordProcess.setTrace(true);
        }
        PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
        String list = periodicScanner.toString();
        System.out.println(list);
        EventScanner eventScanner = ScannerFactory.getEventScanner();
        list = eventScanner.toString();
        System.out.println(list);
        String[] recordNames = new String[] {
                "counter",
                "monitorChange",
                "monitorDeltaChange",
                "monitorPercentageChange",
                "notifyChange",
                "notifyDeltaChange",
                "notifyPercentageChange"
        };
        for(int i=0; i<recordNames.length; i++) {
            DBRecord dbRecord = iocdbMaster.findRecord(recordNames[i]);
            assertNotNull(dbRecord);
            PVRecord pvRecord = dbRecord.getPVRecord();
            PVData[] pvData = pvRecord.getFieldPVDatas();
            Structure structure = (Structure)pvRecord.getField();
            int index = structure.getFieldIndex("value");
            new DBListenerForTesting(iocdbMaster,
                    pvRecord.getRecordName(),
                    pvData[index].getField().getFieldName(),
                    false,false);
        }
        while(true) {
            try {
                Thread.sleep(1000);
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
