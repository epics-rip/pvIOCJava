/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.dbd.*;
import org.epics.ioc.db.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * JUnit test for PVAccess.
 * @author mrk
 *
 */
public class AccessTest extends TestCase {
        
    /**
     * test PVAccess.
     */
    public static void testAccess() {
        DBD dbd = DBDFactory.getMasterDBD(); 
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        Requester iocRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "example/exampleDBD.xml",iocRequester);
              
//        System.out.printf("%n%nstructures");
//        Map<String,DBDStructure> structureMap = dbd.getStructureMap();
//        Set<String> keys = structureMap.keySet();
//        for(String key: keys) {
//            DBDStructure dbdStructure = structureMap.get(key);
//            System.out.print(dbdStructure.toString());
//        }
//        System.out.printf("%n%nrecordTypes");
//        Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
//        keys = recordTypeMap.keySet();
//        for(String key: keys) {
//            DBDRecordType dbdRecordType = recordTypeMap.get(key);
//            System.out.print(dbdRecordType.toString());
//        }
        
          XMLToIOCDBFactory.convert(dbd,iocdb,
                "example/exampleDB.xml",iocRequester);
               
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> recordKey = recordMap.keySet();
//        for(String key: recordKey) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
          
        testAccess(iocdb,"ai","value");
        testAccess(iocdb,"ai","input.value");
        System.out.printf("%n");
        testAccess(iocdb,"psEmbeded","power.value");
        testAccess(iocdb,"psEmbeded","current.value");
        testAccess(iocdb,"psEmbeded","voltage.value");
        System.out.printf("%n");
        testAccess(iocdb,"psLinked","power.value");
        testAccess(iocdb,"psLinked","current.value");
        testAccess(iocdb,"psLinked","voltage.value");
        System.out.printf("%n");
        testAccess(iocdb,"powerSupplyArray","alarm");
        testAccess(iocdb,"powerSupplyArray","timeStamp");
        testAccess(iocdb,"powerSupplyArray","supply[0].power.value");
        testAccess(iocdb,"powerSupplyArray","supply[0].current.value");
        testAccess(iocdb,"powerSupplyArray","supply[0].voltage.value");
        testAccess(iocdb,"powerSupplyArray","supply[1].power.value");
        testAccess(iocdb,"powerSupplyArray","supply[1].current.value");
        testAccess(iocdb,"powerSupplyArray","supply[1].voltage.value");
        System.out.printf("%n");
        testAccess(iocdb,"allTypesInitial","boolean");
        testAccess(iocdb,"allTypesInitial","byte");
        testAccess(iocdb,"allTypesInitial","short");
        testAccess(iocdb,"allTypesInitial","int");
        testAccess(iocdb,"allTypesInitial","long");
        testAccess(iocdb,"allTypesInitial","float");
        testAccess(iocdb,"allTypesInitial","double");
        testAccess(iocdb,"allTypesInitial","string");
        testAccess(iocdb,"allTypesInitial","booleanArray");
        testAccess(iocdb,"allTypesInitial","byteArray");
        testAccess(iocdb,"allTypesInitial","shortArray");
        testAccess(iocdb,"allTypesInitial","intArray");
        testAccess(iocdb,"allTypesInitial","longArray");
        testAccess(iocdb,"allTypesInitial","floatArray");
        testAccess(iocdb,"allTypesInitial","doubleArray");
        testAccess(iocdb,"allTypesInitial","stringArray");
        testAccess(iocdb,"allTypesInitial","structArray");
        testAccess(iocdb,"allTypesInitial","arrayArray");
        testAccess(iocdb,"allTypesInitial","allTypes.boolean");
        testAccess(iocdb,"allTypesInitial","allTypes.byte");
        testAccess(iocdb,"allTypesInitial","allTypes.short");
        testAccess(iocdb,"allTypesInitial","allTypes.int");
        testAccess(iocdb,"allTypesInitial","allTypes.long");
        testAccess(iocdb,"allTypesInitial","allTypes.float");
        testAccess(iocdb,"allTypesInitial","allTypes.double");
        testAccess(iocdb,"allTypesInitial","allTypes.string");
        testAccess(iocdb,"allTypesInitial","allTypes.booleanArray");
        testAccess(iocdb,"allTypesInitial","allTypes.byteArray");
        testAccess(iocdb,"allTypesInitial","allTypes.shortArray");
        testAccess(iocdb,"allTypesInitial","allTypes.intArray");
        testAccess(iocdb,"allTypesInitial","allTypes.longArray");
        testAccess(iocdb,"allTypesInitial","allTypes.floatArray");
        testAccess(iocdb,"allTypesInitial","allTypes.doubleArray");
        testAccess(iocdb,"allTypesInitial","allTypes.stringArray");
        testAccess(iocdb,"allTypesInitial","allTypes.structArray");
        testAccess(iocdb,"allTypesInitial","allTypes.arrayArray");
        testAccess(iocdb,"allTypesInitial","allTypes.arrayArray[3]");
    }
    
    static void testAccess(IOCDB iocdb,String recordName,String fieldName) {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        if(dbRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVRecord pvRecord = dbRecord.getPVRecord();
        PVField pvField = pvRecord.findProperty(fieldName);
        if(pvField==null) {
            System.out.printf("field %s of record %s not found%n",fieldName,recordName);
            return;
        }
        PVField parent = pvField.getParent();
        PVRecord record = pvField.getPVRecord();
        String parentName = "none";
        Field field = pvField.getField();
        if(parent!=null) parentName = parent.getFullFieldName();
        System.out.printf("record %s fieldRequested %s fieldActual %s parent %s%n",
                record.getRecordName(),
                fieldName,field.getFieldName(),
                parentName);
        System.out.printf("    value %s%n",pvField.toString(1));
        PVField[] pvFields = pvField.getPropertys();
        if(pvFields==null) return;
            System.out.printf("    property {%n");
            for(PVField pvf: pvFields) {
                String propertyName = pvf.getFullFieldName();
                System.out.printf("        name %s field %s%n",propertyName,pvf.toString(3));
            }
            System.out.printf("        }%n");
    }
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "AccessTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
