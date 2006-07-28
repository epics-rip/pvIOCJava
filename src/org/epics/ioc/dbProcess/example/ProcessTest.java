/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.locks.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;

/**
 * JUnit test for RecordProcess.
 * @author mrk
 *
 */
public class ProcessTest extends TestCase {
    /**
     * test DBAccess.
     */
    public static void testProcess() {
        DBD dbd = DBDFactory.create("test"); 
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        System.out.printf("reading menuStructureSupport\n");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbProcess/example/menuStructureSupportDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
        System.out.printf("reading exampleDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbProcess/example/exampleDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
//        System.out.printf("\n\nstructures");
//        Map<String,DBDStructure> structureMap = dbd.getStructureMap();
//        Set<String> keys = structureMap.keySet();
//        for(String key: keys) {
//            DBDStructure dbdStructure = structureMap.get(key);
//            System.out.print(dbdStructure.toString());
//        }
//        System.out.printf("\n\nrecordTypes");
//        Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
//        keys = recordTypeMap.keySet();
//        for(String key: keys) {
//            DBDRecordType dbdRecordType = recordTypeMap.get(key);
//            System.out.print(dbdRecordType.toString());
//        }
        System.out.printf("reading exampleDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/dbProcess/example/exampleDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        Set<String> recordKey = recordMap.keySet();
        ProcessDB processDB = ProcessFactory.createProcessDB(iocdb);
        boolean gotSupport = processDB.createSupport();
//        System.out.printf("\nrecords\n");
//        for(String key: recordKey) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        if(!gotSupport) {
            System.out.printf("Did not find all support\n");
            System.out.printf("\nrecords\n");
            recordMap = iocdb.getRecordMap();
            recordKey = recordMap.keySet();
            for(String key: recordKey) {
                DBRecord record = recordMap.get(key);
                System.out.print(record.toString());
            }
            return;
        }
        DBRecord dbRecord = null;
        dbRecord = iocdb.findRecord("counter");
        assertNotNull(dbRecord);
        TestProcess testProcess = new TestProcess(dbRecord);
        testProcess.test();
//        testProcess.testPerform();
    }
    
    private static class TestProcess implements ProcessListener {
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        
        TestProcess(DBRecord record) {
            recordProcess = record.getRecordProcess();
            assertNotNull(recordProcess);
        }
        
        void test() {
            RequestProcessReturn requestReturn;
            ProcessReturn processReturn = ProcessReturn.noop;
            requestReturn = recordProcess.requestProcess(this);
            if(requestReturn==RequestProcessReturn.success) {
                processReturn = recordProcess.process(this);
            }
            if(requestReturn==RequestProcessReturn.listenerAdded
            ||processReturn==ProcessReturn.active) {
                lock.lock();
                try {
                    waitDone.await();
                } catch (InterruptedException ie) {
                    return;
                }
            }
            System.out.printf("requestReturn %s processReturn %s\n",
                    requestReturn.toString(),processReturn.toString());
        }
        
        void testPerform() {
            RequestProcessReturn requestReturn;
            ProcessReturn processReturn = ProcessReturn.noop;
            long startTime,endTime;
            int ntimes = 1000000;
            double microseconds;
            double processPerSecond;
            startTime = System.nanoTime();
            for(int i=0; i<ntimes; i++) {
                processReturn = ProcessReturn.noop;
                requestReturn = recordProcess.requestProcess(this);
                if(requestReturn==RequestProcessReturn.success) {
                    processReturn = recordProcess.process(this);
                }
//                System.out.printf("requestReturn %s processReturn %s\n",
//                    requestReturn.toString(),processReturn.toString());
                if(requestReturn==RequestProcessReturn.listenerAdded
                ||processReturn==ProcessReturn.active) {
                    lock.lock();
                    try {
                        waitDone.await();
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
            endTime = System.nanoTime();
            microseconds = (double)(endTime - startTime)/(double)ntimes/1000.0;
            processPerSecond = 1e6/microseconds;
            System.out.printf("time per process %f microseconds processPerSecond %f\n",
                microseconds,processPerSecond);
        }

        public void processComplete(ProcessReturn result) {
            if(result==ProcessReturn.done) {
                waitDone.signal();
            }
        }
    }
}
