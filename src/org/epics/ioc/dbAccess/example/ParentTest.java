/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;

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
        System.out.printf("reading powerSupplyDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/powerSupplyDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading allTypesDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/allTypesDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        //System.out.printf("\n\nstructures");
        //Map<String,DBDStructure> structureMap = dbd.getStructureMap();
        //Set<String> keys = structureMap.keySet();
        //for(String key: keys) {
        //DBDStructure dbdStructure = structureMap.get(key);
        //System.out.print(dbdStructure.toString());
        //}
        //System.out.printf("\n\nrecordTypes");
        //Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
        //keys = recordTypeMap.keySet();
        //for(String key: keys) {
        //DBDRecordType dbdRecordType = recordTypeMap.get(key);
        //System.out.print(dbdRecordType.toString());
        //}
        System.out.printf("reading exampleAiLinearDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/exampleAiLinearDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading examplePowerSupplyDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/examplePowerSupplyDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading examplePowerSupplyArrayDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/examplePowerSupplyArrayDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading exampleAllTypeDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/exampleAllTypeDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
//        System.out.printf("\nrecords\n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        showParent(iocdb,"exampleAiLinear","status");
        showParent(iocdb,"exampleAiLinear","timeStamp");
        showParent(iocdb,"exampleAiLinear","value");
        showParent(iocdb,"exampleAiLinear","rawValue");
        showParent(iocdb,"exampleAiLinear","aiLinear.aiRaw.value");
        showParent(iocdb,"exampleAiLinear","aiLinear.aiRaw.status");
        showParent(iocdb,"exampleAiLinear","aiLinear.aiRaw.input");
        showParent(iocdb,"exampleAiLinear","aiLinear.aiRaw.input.configStructureName");
        System.out.printf("\n");
        showParent(iocdb,"examplePowerSupply","power");
        showParent(iocdb,"examplePowerSupply","current");
        showParent(iocdb,"examplePowerSupply","voltage");
        System.out.printf("\n");
        showParent(iocdb,"examplePowerSupplyArray","status");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        System.out.printf("\n");
        showParent(iocdb,"examplePowerSupplyArray","powerSupply[1]");
        System.out.printf("\n");
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
        showParent(iocdb,"allTypes","allTypes.menuArray");
        showParent(iocdb,"allTypes","allTypes.linkArray");
        showParent(iocdb,"allTypes","allTypes.structArray");
        showParent(iocdb,"allTypes","allTypes.arrayArray");
    }

    static void showParent(IOCDB iocdb,String recordName,String fieldName) {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(!dbAccess.setField(fieldName)){
            System.out.printf("field %s of record %s not found\n",fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        DBRecord record = dbData.getRecord();
        System.out.printf("fieldName %s actualField %s record %s\n",
            fieldName,dbData.getField().getName(),record.getRecordName());
        DBStructure parent = dbData.getParent();
        while(parent!=null) {
            record = parent.getRecord();
            System.out.printf("     parent %s record %s\n",
                    parent.getField().getName(),record.getRecordName());
            parent = parent.getParent();
        }
        
    }
    
}
