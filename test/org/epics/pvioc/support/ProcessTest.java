/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.install.AfterStartFactory;
import org.epics.pvioc.install.AfterStartNode;
import org.epics.pvioc.install.AfterStartRequester;
import org.epics.pvioc.install.Install;
import org.epics.pvioc.install.InstallFactory;
import org.epics.pvioc.install.NewAfterStartRequester;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.util.RequestResult;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;


/**
 * JUnit test for RecordProcess.
 * @author mrk
 *
 */
public class ProcessTest extends TestCase {
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final Install install = InstallFactory.get();
    private static MessageType maxMessageType = MessageType.info;
 
   public static void testProcessDummy() { }
   /**
     * test PVAccess.
     */
    public static void DISABLED_testProcess() {
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${JAVAIOC}/xml/structures.xml", iocRequester,false,null,null,null);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        new NewAfterStartRequesterImpl(0);
        boolean ok = install.installRecords("test/org/epics/ioc/support/processTestPV.xml", iocRequester);
        assertTrue(ok);
        PVRecord[] pvRecords;
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
            RecordProcess recordProcess = record.getRecordProcess();
            recordProcess.setTrace(true);
        }
        testProcess.test(); 
        for(PVRecord record: pvRecords) {
            RecordProcess recordProcess = record.getRecordProcess();
            recordProcess.setTrace(false);
        } 
        System.out.println("starting performance test"); 
        testProcess.testPerform();
        ok = install.installRecords("test/org/epics/ioc/support/loopPV.xml", iocRequester);
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
            RecordProcess recordProcess = record.getRecordProcess();
            recordProcess.setTrace(true);
        }
        System.out.printf("%n%n");
        testProcess.test();
    }
    
    private static class NewAfterStartRequesterImpl implements NewAfterStartRequester, AfterStartRequester
    {
        private AfterStartNode afterStartNode;
        private AfterStart afterStart = null;
        private NewAfterStartRequesterImpl(int delay) {
            afterStartNode = AfterStartFactory.allocNode(this);
            AfterStartFactory.newAfterStartRegister(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.NewAfterStartRequester#callback(org.epics.pvioc.install.AfterStart)
         */
        public void callback(AfterStart afterStart) {
            this.afterStart = afterStart;
            afterStart.requestCallback(afterStartNode,true, ThreadPriority.middle);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStartRequester#callback(org.epics.pvioc.install.AfterStartNode)
         */
        public void callback(AfterStartNode node) {
            System.out.println("NewAfterStartRequester called");
            afterStart.done(node);
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
    
    private static class TestProcess implements RecordProcessRequester {
    	private ProcessToken processToken = null;
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private TimeStamp timeStamp = TimeStampFactory.create();
        
        private TestProcess(PVRecord pvRecord) {
            recordProcess = pvRecord.getRecordProcess();
            assertNotNull(recordProcess);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        @Override
        public void message(String message, MessageType messageType) {
            System.out.println(message + " messageType " + messageType.toString());
        }

        void test() {
            allDone = false;
            timeStamp.put(System.currentTimeMillis());
            processToken = recordProcess.requestProcessToken(this);
            if(processToken==null) {
                System.out.println("could not become recordProcessor");
                return;
            }
            recordProcess.queueProcessRequest(processToken);
            if(!allDone) {
                lock.lock();
                try {
                    while(!allDone) {
                        waitDone.await();
                    }
                } catch (InterruptedException ie) {
                    return;
                } finally {
                    lock.unlock();
                }
            }
            recordProcess.releaseProcessToken(processToken);
        }
        
		void testPerform() {
			processToken = recordProcess.requestProcessToken(this);
            if(processToken==null) {
                System.out.println("could not become recordProcessor");
                return;
            }
            long startTime,endTime;
            int nwarmup = 1000;
            int ntimes = 100000;
            double microseconds;
            double processPerSecond;
            startTime = System.nanoTime();
            for(int i=0; i< nwarmup + ntimes; i++) {
                allDone = false;
                timeStamp.put(System.currentTimeMillis());
                recordProcess.queueProcessRequest(processToken);
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
            microseconds = (double)(endTime - startTime)/(double)ntimes/1000.0;
            processPerSecond = 1e6/microseconds;
            System.out.printf("time per process %f microseconds processPerSecond %f\n",
                microseconds,processPerSecond);
            recordProcess.releaseProcessToken(processToken);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.RecordProcessRequester#getRecordProcessRequestorName()
         */
        public String getRequesterName() {
            return "testProcess";
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
         */
        @Override
		public void becomeProcessor() {
        	recordProcess.process(processToken,false,timeStamp);
		}
		/* (non-Javadoc)
		 * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
		 */
		@Override
		public void canNotProcess(String reason) {
			 throw new IllegalStateException("canNotProcess");
		}
		/* (non-Javadoc)
		 * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
		 */
		@Override
		public void lostRightToProcess() {
			throw new IllegalStateException("lostRightToProcess");
		}
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.RecordProcessRequester#processComplete(org.epics.pvioc.process.Support, org.epics.pvioc.process.ProcessResult)
         */
		@Override
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
         * @see org.epics.pvioc.process.RecordProcessRequester#requestResult(org.epics.pvioc.util.AlarmSeverity, java.lang.String, org.epics.pvioc.util.TimeStamp)
         */
		@Override
        public void recordProcessResult(RequestResult requestResult) {
        	// nothing to do 
        }
    }
    
    private static class PVDatabaseRequester implements Requester {
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "ProcessTest";
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println("ProcessTest: " + messageType + " " + message);
            
        }
    }
}
