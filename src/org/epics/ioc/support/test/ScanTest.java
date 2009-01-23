/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.test;

import junit.framework.TestCase;

import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.IOCFactory;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.ScannerFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.Requester;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.xml.XMLToPVDatabaseFactory;


/**
 * JUnit test for scan test.
 * @author mrk
 *
 */
public class ScanTest extends TestCase {
    private static PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static MessageType maxMessageType = MessageType.info;
    /**
     * test scan.
     */
    public static void testScan() {
        PVRecord[] pvRecords = null;
        Requester iocRequester = new RequesterForTesting("scanTest");
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${PVDATA}/xml/structures.xml", iocRequester);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${JAVAIOC}/xml/structures.xml", iocRequester);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        boolean ok = IOCFactory.initDatabase("src/org/epics/ioc/support/test/scanPV.xml", iocRequester);
        if(!ok) {
            System.out.printf("\nrecords\n");
            pvRecords = masterPVDatabase.getRecords();
            for(PVRecord pvRecord: pvRecords) {
                System.out.print(pvRecord.toString());
            }
            return;
        }
        try {
            Thread.sleep(1000);
            System.out.println();
        } catch (InterruptedException e) {}
        
//        System.out.printf("\nrecords\n");
//        pvRecords = masterPVDatabase.getRecords();
//        for(PVRecord pvRecord: pvRecords) {
//            System.out.println(pvRecord.toString());
//        }
        
        PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
        String list = periodicScanner.toString();
        System.out.println(list);
        EventScanner eventScanner = ScannerFactory.getEventScanner();
        list = eventScanner.toString();
        System.out.println(list);
        
        PVRecord pvRecord = null;
        pvRecord = masterPVDatabase.findRecord("counterPush");
        assertNotNull(pvRecord);
        Structure structure = (Structure)pvRecord.getField();
        PVField[] pvData = pvRecord.getPVFields();        
        int index = structure.getFieldIndex("value");
        PVField counterPushValue = pvData[index];
        pvRecord = masterPVDatabase.findRecord("doubleReceive09");
        assertNotNull(pvRecord);
        pvRecord = pvRecord.getPVRecord();
        structure = (Structure)pvRecord.getField();
        pvData = pvRecord.getPVFields();        
        index = structure.getFieldIndex("value");
        PVField doubleReceive09Value = pvData[index];
        pvRecord = masterPVDatabase.findRecord("counterEvent0");
        assertNotNull(pvRecord);
        pvRecord = pvRecord.getPVRecord();
        structure = (Structure)pvRecord.getField();
        pvData = pvRecord.getPVFields();        
        index = structure.getFieldIndex("value");
        PVField counterEvent0Value = pvData[index];
        pvRecord = masterPVDatabase.findRecord("counterEvent1");
        assertNotNull(pvRecord);
        pvRecord = pvRecord.getPVRecord();
        structure = (Structure)pvRecord.getField();
        pvData = pvRecord.getPVFields();        
        index = structure.getFieldIndex("value");
        PVField counterEvent1Value = pvData[index];
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
        ok = IOCFactory.initDatabase("src/org/epics/ioc/support/test/scanAddPV.xml", iocRequester);
        if(!ok) return;
       
        String[] recordList = masterPVDatabase.recordList(".*");
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
        pvRecord = masterPVDatabase.findRecord("counter");
        assertNotNull(pvRecord);
        pvRecord = pvRecord.getPVRecord();
        structure = (Structure)pvRecord.getField();
        pvData = pvRecord.getPVFields();        
        index = structure.getFieldIndex("value");
        PVField counterValue = pvData[index];
        pvRecord = masterPVDatabase.findRecord("double02");
        assertNotNull(pvRecord);
        pvRecord = pvRecord.getPVRecord();
        structure = (Structure)pvRecord.getField();
        pvData = pvRecord.getPVFields();        
        index = structure.getFieldIndex("value");
        PVField double02Value = pvData[index];
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
    
    private static class RequesterForTesting implements Requester {
        private String requesterName = null;
        
        RequesterForTesting(String requesterName) {
            this.requesterName = requesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return requesterName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            if(messageType.ordinal()>maxMessageType.ordinal()) maxMessageType = messageType;
        }
    }

}
