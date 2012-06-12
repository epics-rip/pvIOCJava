/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support;

import junit.framework.TestCase;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.install.Install;
import org.epics.pvioc.install.InstallFactory;
import org.epics.pvioc.util.EventScanner;
import org.epics.pvioc.util.PeriodicScanner;
import org.epics.pvioc.util.ScannerFactory;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;


/**
 * JUnit test for scan test.
 * @author mrk
 *
 */
public class ScanTest extends TestCase {
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final Install install = InstallFactory.get();
    private static MessageType maxMessageType = MessageType.info;
    /**
     * test scan.
     */
    public static void testScan() {
        PVRecord[] pvRecords = null;
        Requester iocRequester = new RequesterForTesting("scanTest");
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${JAVAIOC}/xml/structures.xml", iocRequester,false,null,null,null);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        boolean ok = install.installRecords("test/org/epics/pvioc/support/scanPV.xml", iocRequester);
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
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        Structure structure = pvStructure.getStructure();
        PVField[] pvData = pvStructure.getPVFields();        
        int index = structure.getFieldIndex("value");
        PVField counterPushValue = pvData[index];
        pvRecord = masterPVDatabase.findRecord("doubleReceive09");
        assertNotNull(pvRecord);
        pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        structure = pvStructure.getStructure();
        pvData = pvStructure.getPVFields();        
        index = structure.getFieldIndex("value");
        PVField doubleReceive09Value = pvData[index];
        pvRecord = masterPVDatabase.findRecord("counterEvent0");
        assertNotNull(pvRecord);
        pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        structure = pvStructure.getStructure();
        pvData = pvStructure.getPVFields();       
        index = structure.getFieldIndex("value");
        PVField counterEvent0Value = pvData[index];
        pvRecord = masterPVDatabase.findRecord("counterEvent1");
        pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        assertNotNull(pvRecord);
        structure = pvStructure.getStructure();
        pvData = pvStructure.getPVFields();           
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
        ok = install.installRecords("test/org/epics/pvioc/support/scanAddPV.xml", iocRequester);
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
        pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        structure = pvStructure.getStructure();
        pvData = pvStructure.getPVFields();            
        index = structure.getFieldIndex("value");
        PVField counterValue = pvData[index];
        pvRecord = masterPVDatabase.findRecord("double02");
        assertNotNull(pvRecord);
        pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        structure = pvStructure.getStructure();
        pvData = pvStructure.getPVFields();
        index = structure.getFieldIndex("value");
        PVField double02Value = pvData[index];
        list = periodicScanner.toString();
        System.out.println(list);
        int counter = 0;
        while(true) {
        	counter++;
        	if(counter>10) break;
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
         * @see org.epics.pvioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return requesterName;
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            if(messageType.ordinal()>maxMessageType.ordinal()) maxMessageType = messageType;
        }
    }

}
