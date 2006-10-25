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
import org.epics.ioc.util.TimeUtility;

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
    
    private static class TestProcess implements RecordProcessRequestor {
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        
        private TestProcess(DBRecord record) {
            recordProcess = record.getRecordProcess();
            assertNotNull(recordProcess);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return "testProcess";
        }

        void test() {
            TimeStamp timeStamp = new TimeStamp();
            allDone = false;
            TimeUtility.set(timeStamp,System.currentTimeMillis());
            boolean gotIt =recordProcess.setRecordProcessRequestor(this);
            if(!gotIt) {
                System.out.println("could not become recordProcessor");
                return;
            }
            RequestResult requestResult = recordProcess.process(this,timeStamp);
            if(requestResult==RequestResult.active) {
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
            recordProcess.releaseRecordProcessRequestor(this);
        }
        
        void testPerform() {
            RequestResult requestResult = RequestResult.success;
            long startTime,endTime;
            int nwarmup = 1000;
            int ntimes = 100000;
            double microseconds;
            double processPerSecond;
            startTime = System.nanoTime();
            TimeStamp timeStamp = new TimeStamp();
            allDone = false;
            boolean gotIt =recordProcess.setRecordProcessRequestor(this);
            if(!gotIt) {
                System.out.printf("could not become recordProcessor");
            }
            for(int i=0; i< nwarmup + ntimes; i++) {
                allDone = false;
                TimeUtility.set(timeStamp,System.currentTimeMillis());
                requestResult = recordProcess.process(this,timeStamp);
                if(requestResult==RequestResult.active) {
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
            recordProcess.releaseRecordProcessRequestor(this);
            microseconds = (double)(endTime - startTime)/(double)ntimes/1000.0;
            processPerSecond = 1e6/microseconds;
            System.out.printf("time per process %f microseconds processPerSecond %f\n",
                microseconds,processPerSecond);
            recordProcess.releaseRecordProcessRequestor(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#getRecordProcessRequestorName()
         */
        public String getRecordProcessRequestorName() {
            return "testProcess";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void recordProcessComplete(RequestResult requestResult) {
            lock.lock();
            try {
                allDone = true;
                    waitDone.signal();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do 
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#ready()
         */
        public RequestResult ready() {
            System.out.println("Why did ready get called");
            return RequestResult.failure;
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
