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

import org.epics.ioc.database.PVDatabase;
import org.epics.ioc.database.PVDatabaseFactory;
import org.epics.ioc.database.PVRecord;
import org.epics.ioc.install.Install;
import org.epics.ioc.install.InstallFactory;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.xml.XMLToPVDatabaseFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.Requester;


/**
 * JUnit test for RecordProcess.
 * @author mrk
 *
 */
public class SimpleProcessTest extends TestCase {
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final Install install = InstallFactory.get();
    private static MessageType maxMessageType = MessageType.info;
    /**
     * test PVAccess.
     */
    public static void testProcess() {
        Requester iocRequester = new RequesterForTesting("simpleProcessTest");
        XMLToPVDatabaseFactory.convert(masterPVDatabase,"${JAVAIOC}/xml/structures.xml", iocRequester,false,null,null,null);
        if(maxMessageType!=MessageType.info&&maxMessageType!=MessageType.warning) return;
        boolean ok = install.installRecords("src/org/epics/ioc/support/test/simpleTestPV.xml", iocRequester);
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
        PVRecord pvRecord = masterPVDatabase.findRecord("simple");
        assertNotNull(pvRecord);
        Requester pvDatabaseRequester = new PVDatabaseRequester();
        masterPVDatabase.addRequester(pvDatabaseRequester);
        TestProcess testProcess = new TestProcess(pvRecord);
        for(PVRecord record: pvRecords) {
            RecordProcess recordProcess = pvRecord.getRecordProcess();
            recordProcess.setTrace(true);
        }
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
    
    private static class TestProcess implements RecordProcessRequester {
    	private ProcessToken processToken = null;
        private RecordProcess recordProcess = null;
        private Lock lock = new ReentrantLock();
        private Condition waitDone = lock.newCondition();
        private boolean allDone = false;
        private TimeStamp timeStamp = TimeStampFactory.create();
        private long startTime = 0;
        
        private TestProcess(PVRecord pvRecord) {
            recordProcess = pvRecord.getRecordProcess();
            assertNotNull(recordProcess);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
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
            startTime = System.nanoTime();
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
        
		void showTime(String message) {
			long endTime = System.nanoTime();
			double diff = endTime - startTime;
			diff /= 1e9;
			System.out.println(message + " time " + diff + " seconds");
		}
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#getRecordProcessRequestorName()
         */
        public String getRequesterName() {
            return "testProcess";
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
         */
        @Override
		public void becomeProcessor() {
        	showTime("becomeProcessor");
        	recordProcess.process(processToken,false,timeStamp);
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
		 */
		@Override
		public void canNotProcess(String reason) {
			showTime("canNotProcess "+ reason);
			 throw new IllegalStateException("canNotProcess");
		}
		/* (non-Javadoc)
		 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
		 */
		@Override
		public void lostRightToProcess() {
			throw new IllegalStateException("lostRightToProcess");
		}
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#processComplete(org.epics.ioc.process.Support, org.epics.ioc.process.ProcessResult)
         */
		@Override
        public void recordProcessComplete() {
			showTime("recordProcessComplete");
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
		@Override
        public void recordProcessResult(RequestResult requestResult) {
			showTime("recordProcessResult " + requestResult);
        }
    }
}
