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
        Set<String> keys = recordMap.keySet();
        ProcessDB processDB = ProcessDBFactory.createProcessDB(iocdb);
//        System.out.printf("\nrecords\n");
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        boolean createdLocal = ChannelAccessLocalFactory.create(iocdb);
        if(!createdLocal) {
            System.out.printf("Did not create local channel access\n");
            return;
        }
        boolean gotSupport = processDB.createSupport();
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
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            Support recordSupport = (Support)record.getSupport();
            if(recordSupport==null) {
                System.out.println(record.getRecordName() + " has no support");
            } else {
                recordSupport.initialize();
                SupportState supportState = recordSupport.getSupportState();
                if(supportState!=SupportState.readyForStart) {
                    System.out.println(
                            record.getRecordName()
                            + " initialize returned " + supportState.toString());
                } else {
                    recordSupport.start();
                    supportState = recordSupport.getSupportState();
                    if(supportState!=SupportState.ready) {
                        System.out.println(
                            record.getRecordName()
                            + " start returned " + supportState.toString());
                    }   
                }
            }
        }
        DBRecord dbRecord = null;
        dbRecord = iocdb.findRecord("counter");
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
}
