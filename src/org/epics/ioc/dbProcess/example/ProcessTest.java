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
import org.epics.ioc.util.IOCMessageListener;
import org.epics.ioc.util.IOCMessageType;

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
        DBD dbd = DBDFactory.create("master",null); 
        IOCDB iocdbMaster = IOCDBFactory.create(dbd,"master");
        IOCDB iocdbAdd = IOCDBFactory.create(dbd,"add");
        IOCMessageListener iocMessageListener = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbProcess/example/menuStructureSupportDBD.xml",
                 iocMessageListener);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbProcess/example/exampleDBD.xml",
                 iocMessageListener);
        
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
            XMLToIOCDBFactory.convert(dbd,iocdbAdd,
                 "src/org/epics/ioc/dbProcess/example/exampleDB.xml",
                 iocMessageListener);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
        Map<String,DBRecord> recordMap = iocdbAdd.getRecordMap();
        Set<String> keys = recordMap.keySet();
        SupportCreation supportCreation = SupportCreationFactory.createSupportCreation(iocdbAdd);
        boolean createdLocal = ChannelAccessLocalFactory.create(iocdbAdd);
        if(!createdLocal) {
            System.out.printf("Did not create local channel access\n");
            return;
        }
        boolean gotSupport = supportCreation.createSupport();
        if(!gotSupport) {
            System.out.printf("Did not find all support\n");
            System.out.printf("\nrecords\n");
            for(String key: keys) {
                DBRecord record = recordMap.get(key);
                System.out.print(record.toString());
            }
            System.out.printf("%n%nsupport");
            Map<String,DBDSupport> supportMap = dbd.getSupportMap();
            keys = supportMap.keySet();
            for(String key: keys) {
                DBDSupport dbdSupport = supportMap.get(key);
                System.out.printf("%n%s",dbdSupport.toString());
            }
            return;
        }
        boolean readyForStart = supportCreation.initializeSupport();
        if(!readyForStart) {
            System.out.println("initializeSupport failed");
            return;
        }
        boolean ready = supportCreation.startSupport();
        if(!ready) {
            System.out.println("startSupport failed");
            return;
        }
        supportCreation = null;
        DBRecord dbRecord = null;
        iocdbAdd.mergeIntoMaster();
        iocdbAdd = null;
        dbRecord = iocdbMaster.findRecord("counter");
        assertNotNull(dbRecord);
        TestProcess testProcess = new TestProcess(dbRecord);
        for(String key: keys) {
//            RecordProcessSupport recordProcessSupport = 
                recordMap.get(key).getRecordProcess().getRecordProcessSupport();
//            recordProcessSupport.setTrace(true);
            
        }
        testProcess.test();
        testProcess.testPerform();
      System.out.printf("\nrecords\n");
      for(String key: keys) {
          DBRecord record = recordMap.get(key);
          System.out.print(record.toString());
      }
    }
    
    private static class TestProcess implements ProcessCompleteListener {
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
            if(processReturn==ProcessReturn.active || processReturn==ProcessReturn.alreadyActive) {
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
                if(processReturn==ProcessReturn.active || processReturn==ProcessReturn.alreadyActive) {
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
         * @see org.epics.ioc.dbProcess.ProcessCompleteListener#processComplete(org.epics.ioc.dbProcess.Support, org.epics.ioc.dbProcess.ProcessResult)
         */
        public void processComplete(Support support,ProcessResult result) {
            lock.lock();
            try {
                allDone = true;
                    waitDone.signal();
            } finally {
                lock.unlock();
            }
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
