/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process.test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.XMLToDBDFactory;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.RecordProcessRequester;
import org.epics.ioc.util.IOCFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.TimeStamp;
import org.epics.ioc.util.TimeUtility;

/**
 * JUnit test for RecordProcess.
 * @author mrk
 *
 */
public class ProcessTest extends TestCase {
    /**
     * test PVAccess.
     */
    public static void testProcess() {
        DBD dbd = DBDFactory.getMasterDBD();
        IOCDB iocdbMaster = IOCDBFactory.getMaster();
        Requester parsingRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "example/exampleDBD.xml",
                 parsingRequester);
        Map<String,DBRecord> recordMap = null;
        Set<String> keys = null;
        boolean initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/process/test/processTestDB.xml",parsingRequester);
        if(!initOK) {
            System.out.printf("\nrecords\n");
            recordMap = iocdbMaster.getRecordMap();
            keys = recordMap.keySet();
            for(String key: keys) {
                DBRecord record = recordMap.get(key);
                System.out.print(record.toString());
            }
            return;
        }
        try {
            Thread.sleep(1000);
            System.out.println();
        } catch (InterruptedException e) {}
        recordMap = iocdbMaster.getRecordMap();
        keys = recordMap.keySet();
        DBRecord dbRecord = iocdbMaster.findRecord("counter");
        assertNotNull(dbRecord);
        Requester iocdbRequester = new IOCDBListener();
        iocdbMaster.addRequester(iocdbRequester);
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
//        System.out.printf("\nrecords\n");
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        initOK = IOCFactory.initDatabase(
            "src/org/epics/ioc/process/test/loopDB.xml",parsingRequester);
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
//        System.out.printf("\nrecords\n");
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
    }
    
    private static class TestProcess implements RecordProcessRequester {
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        
        private TestProcess(DBRecord record) {
            recordProcess = record.getRecordProcess();
            assertNotNull(recordProcess);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.SupportProcessRequester#getProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return "testProcess";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message + " messageType " + messageType.toString());
        }

        void test() {
            TimeStamp timeStamp = new TimeStamp();
            allDone = false;
            TimeUtility.set(timeStamp,System.currentTimeMillis());
            boolean gotIt =recordProcess.setRecordProcessRequester(this);
            if(!gotIt) {
                System.out.println("could not become recordProcessor");
                return;
            }
            recordProcess.process(this,false,timeStamp);
            if(!allDone) {
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
            recordProcess.releaseRecordProcessRequester(this);
        }
        
        void testPerform() {
            long startTime,endTime;
            int nwarmup = 1000;
            int ntimes = 100000;
            double microseconds;
            double processPerSecond;
            startTime = System.nanoTime();
            TimeStamp timeStamp = new TimeStamp();
            allDone = false;
            boolean gotIt =recordProcess.setRecordProcessRequester(this);
            if(!gotIt) {
                System.out.printf("could not become recordProcessor");
            }
            for(int i=0; i< nwarmup + ntimes; i++) {
                allDone = false;
                TimeUtility.set(timeStamp,System.currentTimeMillis());
                recordProcess.process(this,false,timeStamp);
                if(!allDone) {
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
            recordProcess.releaseRecordProcessRequester(this);
            microseconds = (double)(endTime - startTime)/(double)ntimes/1000.0;
            processPerSecond = 1e6/microseconds;
            System.out.printf("time per process %f microseconds processPerSecond %f\n",
                microseconds,processPerSecond);
            recordProcess.releaseRecordProcessRequester(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#getRecordProcessRequestorName()
         */
        public String getRequesterName() {
            return "testProcess";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#processComplete(org.epics.ioc.process.Support, org.epics.ioc.process.ProcessResult)
         */
        public void recordProcessComplete() {
            lock.lock();
            try {
                allDone = true;
                    waitDone.signal();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do 
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#ready()
         */
        public RequestResult ready() {
            System.out.println("Why did ready get called");
            return RequestResult.failure;
        }
    }
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "ProcessTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println("parseListener: " + message);
            
        }
    }
    
    private static class IOCDBListener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "ProcessTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println("IOCDBListener: " + messageType + " " + message);
            
        }
    }
}
