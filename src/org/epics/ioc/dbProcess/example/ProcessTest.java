/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
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
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/menuStructureSupportDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
        System.out.printf("reading aiDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/aiDBD.xml");
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
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbProcess/example/exampleDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
//      System.out.printf("\nrecords\n");
//      Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//      Set<String> recordKey = recordMap.keySet();
//      for(String key: recordKey) {
//          DBRecord record = recordMap.get(key);
//          System.out.print(record.toString());
//      }
        ProcessDB processDB = ProcessFactory.createProcessDB(iocdb);
        String[] recordNames = {"ai1","ai2"};
        DBRecord dbRecord = null;
        RecordSupport recordSupport = null;
        RecordProcess recordProcess = null;
        for(int i=0; i<recordNames.length; i++) {
            String recordName = recordNames[i];
            int indNext = i +1;
            if(indNext>=recordNames.length) indNext = 0;
            String nextRecord = recordNames[indNext];
            dbRecord = iocdb.findRecord(recordName);
            assertNotNull(dbRecord);
            recordSupport = new TestRecordSupport(processDB,dbRecord,iocdb.findRecord(nextRecord));
            recordSupport.initialize(0);
            recordProcess = ProcessFactory.createRecordProcess(dbRecord);
            recordProcess.setRecordSupport(recordSupport);
            processDB.addRecordProcess(recordProcess);
        }
        for(String recordName : recordNames) {
            recordProcess = processDB.findRecordProcess(recordName);
            recordSupport = recordProcess.getRecordSupport();
            recordSupport.initialize(1);
        }
        for(String recordName : recordNames) {
            System.out.printf("\nprocess %s\n",recordName);
            recordProcess = processDB.findRecordProcess(recordName);
            recordProcess.requestProcess(null);
        }
    }
    
    static private class TestRecordSupport implements RecordSupport, ProcessComplete {

        public void complete(ProcessReturn linkedResult) {
            assertNotNull(recordProcess);
            switch(linkedResult) {
                case noop:          result = ProcessReturn.noop; break;
                case done:          result = ProcessReturn.done; break;
                case abort:         result = ProcessReturn.abort; break;
                case active:        result = ProcessReturn.done; break;
                case alreadyActive: result = ProcessReturn.done; break;
            }
            result = ProcessReturn.done;
            System.out.printf("%s TestRecordSupport.complete linkedResult %s"
                    + " will return %s\n",
                    dbRecord.getRecordName(),
                    linkedResult.toString(),
                    result.toString());
            recordProcess.recordSupportDone(result);
            result = ProcessReturn.noop;
            recordProcess = null;
        }

        public void destroy() {
            processDB = null;
            dbRecord = null;
            linkedRecord = null;
            recordProcess = null;
            linkedRecordProcess = null;
        }

        public void initialize(int pass) {
            if(pass==1) {
                if(linkedRecord!=null) {
                    linkedRecordProcess = processDB.findRecordProcess(
                        linkedRecord.getRecordName());
                }
            }
        }

        public ProcessReturn process(RecordProcess recordProcess) {
            System.out.printf("%s TestRecordSupport.process",
                dbRecord.getRecordName());
            if(linkedRecordProcess==null) {
                result = ProcessReturn.noop;
            } else {
                if(recordProcess.requestProcess(linkedRecordProcess,this)) {
                    result = ProcessReturn.active;
                    this.recordProcess =  recordProcess;
                } else {
                    System.out.printf(" requestProcess returned false");
                    result = ProcessReturn.done;
                }
            }
            System.out.printf(" returning %s\n",result.toString());
            return result;
        }
        
        TestRecordSupport(ProcessDB processDB,DBRecord dbRecord,DBRecord linkedRecord) {
            this.processDB = processDB;
            this.dbRecord = dbRecord;
            this.linkedRecord = linkedRecord;
        }
        
        private ProcessDB processDB;
        private DBRecord dbRecord;
        private DBRecord linkedRecord;
        private RecordProcess recordProcess = null;
        private RecordProcess linkedRecordProcess = null;
        private ProcessReturn result = ProcessReturn.noop;
    }
}
