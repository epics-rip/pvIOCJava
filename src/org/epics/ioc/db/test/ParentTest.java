/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.XMLToIOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.XMLToDBDFactory;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * JUnit test for parent.
 * It shows the parent for each field.
 * @author mrk
 *
 */
public class ParentTest extends TestCase {
        
    /**
     * show the parent of various fields.
     */
    public static void testParent() {
        DBD dbd = DBDFactory.getMasterDBD();
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        Requester iocRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "example/exampleDBD.xml",iocRequester);
        
        //System.out.printf("%n%nstructures");
        //Map<String,DBDStructure> structureMap = dbd.getStructureMap();
        //Set<String> keys = structureMap.keySet();
        //for(String key: keys) {
        //DBDStructure dbdStructure = structureMap.get(key);
        //System.out.print(dbdStructure.toString());
        //}
        //System.out.printf("%n%nrecordTypes");
        //Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
        //keys = recordTypeMap.keySet();
        //for(String key: keys) {
        //DBDRecordType dbdRecordType = recordTypeMap.get(key);
        //System.out.print(dbdRecordType.toString());
        //}

        XMLToIOCDBFactory.convert(dbd,iocdb,
                 "example/exampleDB.xml",iocRequester);
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        showParent(iocdb,"ai","alarm.severity");
        showParent(iocdb,"ai","timeStamp");
        showParent(iocdb,"ai","value");
        showParent(iocdb,"ai","input.value");
        System.out.printf("%n");
        showParent(iocdb,"psEmbeded","power");
        showParent(iocdb,"psEmbeded","current");
        showParent(iocdb,"psEmbeded","voltage");
        System.out.printf("%n");
        showParent(iocdb,"powerSupplyArray","alarm.severity");
        showParent(iocdb,"powerSupplyArray","supply[0].power");
        showParent(iocdb,"powerSupplyArray","supply[0].power.value");
        showParent(iocdb,"powerSupplyArray","supply[0].current");
        showParent(iocdb,"powerSupplyArray","supply[0].voltage");
        showParent(iocdb,"powerSupplyArray","supply[0].voltage.input.value");
        System.out.printf("%n");
        showParent(iocdb,"powerSupplyArray","supply[1]");
        System.out.printf("%n");
        showParent(iocdb,"allTypesInitial","boolean");
        showParent(iocdb,"allTypesInitial","byte");
        showParent(iocdb,"allTypesInitial","short");
        showParent(iocdb,"allTypesInitial","int");
        showParent(iocdb,"allTypesInitial","long");
        showParent(iocdb,"allTypesInitial","float");
        showParent(iocdb,"allTypesInitial","double");
        showParent(iocdb,"allTypesInitial","booleanArray");
        showParent(iocdb,"allTypesInitial","byteArray");
        showParent(iocdb,"allTypesInitial","shortArray");
        showParent(iocdb,"allTypesInitial","intArray");
        showParent(iocdb,"allTypesInitial","longArray");
        showParent(iocdb,"allTypesInitial","floatArray");
        showParent(iocdb,"allTypesInitial","doubleArray");
        showParent(iocdb,"allTypesInitial","structArray");
        showParent(iocdb,"allTypesInitial","arrayArray");
        showParent(iocdb,"allTypesInitial","allTypes.boolean");
        showParent(iocdb,"allTypesInitial","allTypes.byte");
        showParent(iocdb,"allTypesInitial","allTypes.short");
        showParent(iocdb,"allTypesInitial","allTypes.int");
        showParent(iocdb,"allTypesInitial","allTypes.long");
        showParent(iocdb,"allTypesInitial","allTypes.float");
        showParent(iocdb,"allTypesInitial","allTypes.double");
        showParent(iocdb,"allTypesInitial","allTypes.booleanArray");
        showParent(iocdb,"allTypesInitial","allTypes.byteArray");
        showParent(iocdb,"allTypesInitial","allTypes.shortArray");
        showParent(iocdb,"allTypesInitial","allTypes.intArray");
        showParent(iocdb,"allTypesInitial","allTypes.longArray");
        showParent(iocdb,"allTypesInitial","allTypes.floatArray");
        showParent(iocdb,"allTypesInitial","allTypes.doubleArray");
        showParent(iocdb,"allTypesInitial","allTypes.structArray");
        showParent(iocdb,"allTypesInitial","allTypes.structArray[0]");
        showParent(iocdb,"allTypesInitial","allTypes.structArray[0].low");
        showParent(iocdb,"allTypesInitial","allTypes.arrayArray");
        showParent(iocdb,"allTypesInitial","allTypes.arrayArray[0]");
    }

    static void showParent(IOCDB iocdb,String recordName,String fieldName) {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        if(dbRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVRecord pvRecord = dbRecord.getPVRecord();
        PVField pvField = pvRecord.findProperty(fieldName);
        if(pvField==null){
            System.out.printf("field %s not in record %s%n",fieldName,recordName);
            return;
        }
        PVRecord record = pvField.getPVRecord();
        System.out.printf("fieldName %s actualField %s record %s fullName %s%n",
            fieldName,
            pvField.getField().getFieldName(),
            record.getRecordName(),
            record.getRecordName() + pvField.getFullFieldName());
        PVField parent = pvField.getParent();
        while(parent!=null) {
            record = parent.getPVRecord();
            System.out.printf("     parent %s\n",parent.getFullName());
            parent = parent.getParent();
        }
        
    }
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "ParentTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
