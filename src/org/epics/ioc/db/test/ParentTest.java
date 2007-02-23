/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;
import org.epics.ioc.db.*;
import org.epics.ioc.util.Requestor;
import org.epics.ioc.util.MessageType;

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
        Requestor iocRequestor = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/db/test/parentDBD.xml",iocRequestor);
        
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
                 "src/org/epics/ioc/db/test/parentDB.xml",iocRequestor);
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        showParent(iocdb,"exampleAi","status");
        showParent(iocdb,"exampleAi","timeStamp");
        showParent(iocdb,"exampleAi","value");
        showParent(iocdb,"exampleAi","rawValue");
        showParent(iocdb,"exampleAi","input.aiRaw.value");
        showParent(iocdb,"exampleAi","input.aiRaw.status");
        showParent(iocdb,"exampleAi","input.aiRaw.input");
        showParent(iocdb,"exampleAi","input.aiRaw.input.configurationStructureName");
        System.out.printf("%n");
        showParent(iocdb,"examplePowerSupply","power");
        showParent(iocdb,"examplePowerSupply","current");
        showParent(iocdb,"examplePowerSupply","voltage");
        System.out.printf("%n");
        showParent(iocdb,"examplePowerSupplyArray","status");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        System.out.printf("%n");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[1]");
        System.out.printf("%n");
        showParent(iocdb,"allTypes","boolean");
        showParent(iocdb,"allTypes","byte");
        showParent(iocdb,"allTypes","short");
        showParent(iocdb,"allTypes","int");
        showParent(iocdb,"allTypes","long");
        showParent(iocdb,"allTypes","float");
        showParent(iocdb,"allTypes","double");
        showParent(iocdb,"allTypes","enum");
        showParent(iocdb,"allTypes","menu");
        showParent(iocdb,"allTypes","displayLimit");
        showParent(iocdb,"allTypes","link");
        showParent(iocdb,"allTypes","booleanArray");
        showParent(iocdb,"allTypes","byteArray");
        showParent(iocdb,"allTypes","shortArray");
        showParent(iocdb,"allTypes","intArray");
        showParent(iocdb,"allTypes","longArray");
        showParent(iocdb,"allTypes","floatArray");
        showParent(iocdb,"allTypes","doubleArray");
        showParent(iocdb,"allTypes","enumArray");
        showParent(iocdb,"allTypes","menuArray");
        showParent(iocdb,"allTypes","linkArray");
        showParent(iocdb,"allTypes","structArray");
        showParent(iocdb,"allTypes","arrayArray");
        showParent(iocdb,"allTypes","allTypes.boolean");
        showParent(iocdb,"allTypes","allTypes.byte");
        showParent(iocdb,"allTypes","allTypes.short");
        showParent(iocdb,"allTypes","allTypes.int");
        showParent(iocdb,"allTypes","allTypes.long");
        showParent(iocdb,"allTypes","allTypes.float");
        showParent(iocdb,"allTypes","allTypes.double");
        showParent(iocdb,"allTypes","allTypes.enum");
        showParent(iocdb,"allTypes","allTypes.menu");
        showParent(iocdb,"allTypes","allTypes.displayLimit");
        showParent(iocdb,"allTypes","allTypes.link");
        showParent(iocdb,"allTypes","allTypes.booleanArray");
        showParent(iocdb,"allTypes","allTypes.byteArray");
        showParent(iocdb,"allTypes","allTypes.shortArray");
        showParent(iocdb,"allTypes","allTypes.intArray");
        showParent(iocdb,"allTypes","allTypes.longArray");
        showParent(iocdb,"allTypes","allTypes.floatArray");
        showParent(iocdb,"allTypes","allTypes.doubleArray");
        showParent(iocdb,"allTypes","allTypes.enumArray");
        showParent(iocdb,"allTypes","allTypes.enumArray[1]");
        showParent(iocdb,"allTypes","allTypes.menuArray");
        showParent(iocdb,"allTypes","allTypes.menuArray[0]");
        showParent(iocdb,"allTypes","allTypes.linkArray");
        showParent(iocdb,"allTypes","allTypes.linkArray[0]");
        showParent(iocdb,"allTypes","allTypes.structArray");
        showParent(iocdb,"allTypes","allTypes.structArray[0]");
        showParent(iocdb,"allTypes","allTypes.structArray[0].low");
        showParent(iocdb,"allTypes","allTypes.arrayArray");
        showParent(iocdb,"allTypes","allTypes.arrayArray[0]");
    }

    static void showParent(IOCDB iocdb,String recordName,String fieldName) {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        if(pvAccess.findField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s%n",fieldName,recordName);
            return;
        }
        PVData pvData = pvAccess.getField();
        PVRecord record = pvData.getPVRecord();
        System.out.printf("fieldName %s actualField %s record %s fullName %s%n",
            fieldName,
            pvData.getField().getFieldName(),
            record.getRecordName(),
            record.getRecordName() + pvData.getFullFieldName());
        PVData parent = pvData.getParent();
        while(parent!=null) {
            record = parent.getPVRecord();
            System.out.printf("     parent %s record %s%n",
                    parent.getField().getFieldName(),record.getRecordName());
            parent = parent.getParent();
        }
        
    }
    
    private static class Listener implements Requestor {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "ParentTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
