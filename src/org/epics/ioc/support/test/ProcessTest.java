/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.pvData.test.RequesterForTesting;
import org.epics.pvData.xml.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;


import org.epics.ioc.ca.*;


/**
 * JUnit test for RecordProcess.
 * @author mrk
 *
 */
public class ProcessTest extends TestCase {
    private static PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static SupportDatabase masterSupportDatabase = SupportDatabaseFactory.get(masterPVDatabase);
    private static MessageType maxMessageType = MessageType.info;
    /**
     * test PVAccess.
     */
    public static void testProcess() {
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${PVDATA}/xml/structures.xml", iocRequester);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${JAVAIOC}/xml/structures.xml", iocRequester);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        boolean ok = IOCFactory.initDatabase("src/org/epics/ioc/support/test/processTestPV.xml", iocRequester);
        PVRecord[] pvRecords;
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
        pvRecords = masterPVDatabase.getRecords();
        PVRecord pvRecord = masterPVDatabase.findRecord("counter");
        assertNotNull(pvRecord);
        Requester pvDatabaseRequester = new PVDatabaseRequester();
        masterPVDatabase.addRequester(pvDatabaseRequester);
        TestProcess testProcess = new TestProcess(pvRecord);
        for(PVRecord record: pvRecords) {
            RecordProcess recordProcess = masterSupportDatabase.getRecordSupport(record).getRecordProcess();
            recordProcess.setTrace(true);
        }
        testProcess.test(); 
        for(PVRecord record: pvRecords) {
            RecordProcess recordProcess = masterSupportDatabase.getRecordSupport(record).getRecordProcess();
            recordProcess.setTrace(false);
        } 
        System.out.println("starting performance test"); 
        testProcess.testPerform();
        

        
        ok = IOCFactory.initDatabase("src/org/epics/ioc/support/test/loopPV.xml", iocRequester);
        if(!ok) return;
        
//        System.out.printf("\nrecords\n");
//        pvRecords = masterPVDatabase.getRecords();
//        for(PVRecord record: pvRecords) {
//            System.out.println(record.toString());
//        }
        
        pvRecord = masterPVDatabase.findRecord("root");
        assertNotNull(pvRecord);
        testProcess = new TestProcess(pvRecord);
        pvRecords = masterPVDatabase.getRecords();
        for(PVRecord record: pvRecords) {
            RecordProcess recordProcess = masterSupportDatabase.getRecordSupport(record).getRecordProcess();
            recordProcess.setTrace(true);
        }
        System.out.printf("%n%n");
        testProcess.test();
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
    
    private static class TestProcess implements RecordProcessRequester {
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        
        private TestProcess(PVRecord record) {
            recordProcess = masterSupportDatabase.getRecordSupport(record).getRecordProcess();
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
            TimeStamp timeStamp = TimeStampFactory.create(0, 0);
            allDone = false;
            timeStamp.put(System.currentTimeMillis());
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
            TimeStamp timeStamp = TimeStampFactory.create(0, 0);
            allDone = false;
            boolean gotIt =recordProcess.setRecordProcessRequester(this);
            if(!gotIt) {
                System.out.printf("could not become recordProcessor");
            }
            for(int i=0; i< nwarmup + ntimes; i++) {
                allDone = false;
                timeStamp.put(System.currentTimeMillis());
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
    
    private static class PVDatabaseRequester implements Requester {
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
            System.out.println("ProcessTest: " + messageType + " " + message);
            
        }
    }
}
