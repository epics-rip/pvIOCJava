/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

/**
 * JUnit test for DBAccess.
 * @author mrk
 *
 */
public class AccessTest extends TestCase {
        
    /**
     * test DBAccess.
     */
    public static void testAccess() {
        DBD dbd = DBDFactory.create("test"); 
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/accessDBD.xml");
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
              
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
        
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "src/org/epics/ioc/dbAccess/example/accessDB.xml");
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> recordKey = recordMap.keySet();
//        for(String key: recordKey) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        testAccess(iocdb,"exampleAiLinear","status");
        testAccess(iocdb,"exampleAiLinear","timeStamp");
        testAccess(iocdb,"exampleAiLinear","value");
        testAccess(iocdb,"exampleAiLinear","rawValue");
        testAccess(iocdb,"exampleAiLinear","aiLinear.aiRaw.value");
        testAccess(iocdb,"exampleAiLinear","aiLinear.aiRaw.status");
        testAccess(iocdb,"exampleAiLinear","aiLinear.aiRaw.input");
        testAccess(iocdb,"exampleAiLinear","aiLinear.timeStamp");
        testAccess(iocdb,"exampleAiLinear","aiLinear.rawValue");
        System.out.printf("%n");
        testAccess(iocdb,"examplePowerSupply","power");
        testAccess(iocdb,"examplePowerSupply","power.status");
        testAccess(iocdb,"examplePowerSupply","power.timeStamp");
        testAccess(iocdb,"examplePowerSupply","current");
        testAccess(iocdb,"examplePowerSupply","current.status");
        testAccess(iocdb,"examplePowerSupply","current.timeStamp");
        testAccess(iocdb,"examplePowerSupply","voltage");
        System.out.printf("%n");
        testAccess(iocdb,"examplePowerSupplyLinked","power");
        testAccess(iocdb,"examplePowerSupplyLinked","power.status");
        testAccess(iocdb,"examplePowerSupplyLinked","power.timeStamp");
        testAccess(iocdb,"examplePowerSupplyLinked","current");
        testAccess(iocdb,"examplePowerSupplyLinked","current.status");
        testAccess(iocdb,"examplePowerSupplyLinked","current.timeStamp");
        testAccess(iocdb,"examplePowerSupplyLinked","current.rawValue");
        testAccess(iocdb,"examplePowerSupplyLinked","current.rawValue.status");
        testAccess(iocdb,"examplePowerSupplyLinked","voltage");
        System.out.printf("%n");
        testAccess(iocdb,"examplePowerSupplyArray","status");
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        System.out.printf("%n");
        testAccess(iocdb,"allTypes","boolean");
        testAccess(iocdb,"allTypes","byte");
        testAccess(iocdb,"allTypes","short");
        testAccess(iocdb,"allTypes","int");
        testAccess(iocdb,"allTypes","long");
        testAccess(iocdb,"allTypes","float");
        testAccess(iocdb,"allTypes","double");
        testAccess(iocdb,"allTypes","string");
        testAccess(iocdb,"allTypes","enum");
        testAccess(iocdb,"allTypes","menu");
        testAccess(iocdb,"allTypes","displayLimit");
        testAccess(iocdb,"allTypes","link");
        testAccess(iocdb,"allTypes","booleanArray");
        testAccess(iocdb,"allTypes","byteArray");
        testAccess(iocdb,"allTypes","shortArray");
        testAccess(iocdb,"allTypes","intArray");
        testAccess(iocdb,"allTypes","longArray");
        testAccess(iocdb,"allTypes","floatArray");
        testAccess(iocdb,"allTypes","doubleArray");
        testAccess(iocdb,"allTypes","stringArray");
        testAccess(iocdb,"allTypes","enumArray");
        testAccess(iocdb,"allTypes","menuArray");
        testAccess(iocdb,"allTypes","linkArray");
        testAccess(iocdb,"allTypes","structArray");
        testAccess(iocdb,"allTypes","arrayArray");
        testAccess(iocdb,"allTypes","allTypes.boolean");
        testAccess(iocdb,"allTypes","allTypes.byte");
        testAccess(iocdb,"allTypes","allTypes.short");
        testAccess(iocdb,"allTypes","allTypes.int");
        testAccess(iocdb,"allTypes","allTypes.long");
        testAccess(iocdb,"allTypes","allTypes.float");
        testAccess(iocdb,"allTypes","allTypes.double");
        testAccess(iocdb,"allTypes","allTypes.string");
        testAccess(iocdb,"allTypes","allTypes.enum");
        testAccess(iocdb,"allTypes","allTypes.menu");
        testAccess(iocdb,"allTypes","allTypes.displayLimit");
        testAccess(iocdb,"allTypes","allTypes.link");
        testAccess(iocdb,"allTypes","allTypes.booleanArray");
        testAccess(iocdb,"allTypes","allTypes.byteArray");
        testAccess(iocdb,"allTypes","allTypes.shortArray");
        testAccess(iocdb,"allTypes","allTypes.intArray");
        testAccess(iocdb,"allTypes","allTypes.longArray");
        testAccess(iocdb,"allTypes","allTypes.floatArray");
        testAccess(iocdb,"allTypes","allTypes.doubleArray");
        testAccess(iocdb,"allTypes","allTypes.stringArray");
        testAccess(iocdb,"allTypes","allTypes.enumArray");
        testAccess(iocdb,"allTypes","allTypes.menuArray");
        testAccess(iocdb,"allTypes","allTypes.linkArray");
        testAccess(iocdb,"allTypes","allTypes.structArray");
        testAccess(iocdb,"allTypes","allTypes.arrayArray");
    }
    
    static void testAccess(IOCDB iocdb,String recordName,String fieldName) {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        DBData dbData;
        AccessSetResult result = dbAccess.setField(fieldName);
        if(result==AccessSetResult.notFound) {
            System.out.printf("field %s of record %s not found%n",fieldName,recordName);
            return;
        }
        if(result==AccessSetResult.otherRecord) {
            String otherRecord = dbAccess.getOtherRecord();
            String otherField = dbAccess.getOtherField();
            System.out.printf("field %s is in other record with name %s field %s%n",
                fieldName,otherRecord,otherField);
            testAccess(iocdb,otherRecord,otherField);
            return;
        }
        dbData = dbAccess.getField();
        if(dbData==null) {
            System.out.printf("field %s of record %s not found%n",fieldName,recordName);
            return;
        }
        DBData parent = dbData.getParent();
        DBRecord record = dbData.getRecord();
        String parentName = "none";
        Field field = dbData.getField();
        if(parent!=null) parentName = ((Structure)parent.getField()).getStructureName();
        System.out.printf("record %s fieldRequested %s fieldActual %s parent %s%n",
                record.getRecordName(),
                fieldName,field.getName(),
                parentName);
        System.out.printf("    value %s%n",dbData.toString(1));
        Property[] property = field.getPropertys();
        if(property.length>0) {
            System.out.printf("    property {%n");
            for(Property prop : property) {
                System.out.printf("        name %s field %s%n",prop.getName(),prop.getFieldName());
                DBData propData = dbAccess.getPropertyField(prop);
                if(propData!=null) {
                    System.out.printf("            value %s%n",propData.toString(3));
                } else {
                    System.out.printf("            value not found%n");
                }
            }
            System.out.printf("        }%n");
        }
    }
}
