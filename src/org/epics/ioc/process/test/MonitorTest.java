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
            PVData[] pvData = dbRecord.getFieldPVDatas();
            Structure structure = (Structure)dbRecord.getField();
            int index = structure.getFieldIndex("value");
            new Monitor((DBData)pvData[index]);
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
    
    private static class Monitor implements DBListener {
        private String recordName;
        private DBData dbData;
        private RecordListener recordListener = null;
        
        private Monitor(DBData data) {
            dbData = data;
            DBRecord dbRecord = data.getRecord();
            recordName = dbRecord.getRecordName();
            recordListener = dbRecord.createListener(this);
            dbRecord.addListener(recordListener);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginProcess()
         */
        public void beginProcess() {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.pv.PVStructure)
         */
        public void beginPut(PVStructure pvStructure) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.pv.PVLink)
         */
        public void configurationStructurePut(PVLink pvLink) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBData)
         */
        public void dataPut(DBData data) {
            if(data!=dbData) return;
            System.out.println(data.toString() + " " + recordName);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endProcess()
         */
        public void endProcess() {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.pv.PVStructure)
         */
        public void endPut(PVStructure pvStructure) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumChoicesPut(PVEnum pvEnum) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumIndexPut(PVEnum pvEnum) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#structurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.db.DBData)
         */
        public void structurePut(PVStructure pvStructure, DBData data) {
            if(data!=dbData) return;
            System.out.println(data.toString() + " " + recordName);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBData)
         */
        public void supportNamePut(DBData dbData) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#unlisten(org.epics.ioc.db.RecordListener)
         */
        public void unlisten(RecordListener listener) {
            // nothing to do
        }
    }
}
