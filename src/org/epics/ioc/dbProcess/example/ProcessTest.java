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
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.IOCFactory;
import org.epics.ioc.util.IOCMessageListener;
import org.epics.ioc.util.IOCMessageType;
import org.epics.ioc.util.TimeStamp;

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
        DBD dbd = DBDFactory.getMasterDBD();
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        IOCMessageListener iocMessageListener = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/support/menuStructureSupportDBD.xml",
                 iocMessageListener);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbProcess/example/exampleDBD.xml",
                 iocMessageListener);        
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/dbProcess/example/exampleDB.xml",iocMessageListener);
        if(!initOK) return;
        Map<String,DBRecord> recordMap = iocdbMaster.getRecordMap();
        Set<String> keys = recordMap.keySet();
        DBRecord dbRecord = iocdbMaster.findRecord("counter");
        assertNotNull(dbRecord);
        TestProcess testProcess = new TestProcess(dbRecord);
        for(String key: keys) {
            RecordProcess recordProcess = recordMap.get(key).getRecordProcess();
            recordProcess.setTrace(true);
        }
        testProcess.test();     
        for(String key: keys) {
            RecordProcess recordProcess = recordMap.get(key).getRecordProcess();
            recordProcess.setTrace(false);
        }
        System.out.println("starting performance test"); 
        testProcess.testPerform();
        System.out.printf("\nrecords\n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
        initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/dbProcess/example/loopDB.xml",iocMessageListener);
        if(!initOK) return;
        dbRecord = iocdbMaster.findRecord("root");
        assertNotNull(dbRecord);
        testProcess = new TestProcess(dbRecord);
        recordMap = iocdbMaster.getRecordMap();
        keys = recordMap.keySet();
        for(String key: keys) {
            RecordProcess recordProcess = recordMap.get(key).getRecordProcess();
            recordProcess.setTrace(true);
        }
        System.out.printf("%n%n");
        testProcess.test();
        System.out.printf("\nrecords\n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
    }
    
    private static class TestProcess implements ProcessRequestListener {
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        
        private TestProcess(DBRecord record) {
            recordProcess = record.getRecordProcess();
            assertNotNull(recordProcess);
        }
        
        void test() {
            allDone = false;
            ProcessReturn processReturn = recordProcess.process(this);
            if(processReturn==ProcessReturn.active) {
                lock.lock();
                try {
                    if(!allDone) {
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    return;
                } finally {
                    lock.unlock();
                }
            }
        }
        
        void testPerform() {
            ProcessReturn processReturn = ProcessReturn.noop;
            long startTime,endTime;
            int nwarmup = 1000;
            int ntimes = 100000;
            double microseconds;
            double processPerSecond;
            startTime = System.nanoTime();
            for(int i=0; i< nwarmup + ntimes; i++) {
                allDone = false;
                processReturn = recordProcess.process(this);
                if(processReturn==ProcessReturn.active) {
                    lock.lock();
                    try {
                        if(!allDone) waitDone.await();
                    } catch (InterruptedException ie) {
                        return;
                    } finally {
                        lock.unlock();
                    }
                }
                if(i==nwarmup) startTime = System.nanoTime();
            }
            endTime = System.nanoTime();
            microseconds = (double)(endTime - startTime)/(double)ntimes/1000.0;
            processPerSecond = 1e6/microseconds;
            System.out.printf("time per process %f microseconds processPerSecond %f\n",
                microseconds,processPerSecond);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete() {
            lock.lock();
            try {
                allDone = true;
                    waitDone.signal();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do 
        }
    }
    
    private static class Listener implements IOCMessageListener {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCMessageListener#message(java.lang.String, org.epics.ioc.util.IOCMessageType)
         */
        public void message(String message, IOCMessageType messageType) {
            System.out.println(message);
            
        }
    }
}
