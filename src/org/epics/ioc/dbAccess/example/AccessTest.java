/**
 * 
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

public class AccessTest extends TestCase {
        
    public static void testXML() {
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
        testAccess(iocdb,"exampleAiLinear","status");
        testAccess(iocdb,"exampleAiLinear","timeStamp");
        testAccess(iocdb,"exampleAiLinear","value");
        testAccess(iocdb,"exampleAiLinear","rawValue");
        testAccess(iocdb,"exampleAiLinear","aiLinear.aiRaw.value");
        testAccess(iocdb,"exampleAiLinear","aiLinear.aiRaw.status");
        testAccess(iocdb,"exampleAiLinear","aiLinear.aiRaw.input");
        System.out.printf("\n");
        testAccess(iocdb,"examplePowerSupply","power");
        testAccess(iocdb,"examplePowerSupply","current");
        testAccess(iocdb,"examplePowerSupply","voltage");
        System.out.printf("\n");
        testAccess(iocdb,"examplePowerSupplyArray","status");
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        System.out.printf("\n");
        testAccess(iocdb,"allTypes","boolean");
        testAccess(iocdb,"allTypes","byte");
        testAccess(iocdb,"allTypes","short");
        testAccess(iocdb,"allTypes","int");
        testAccess(iocdb,"allTypes","long");
        testAccess(iocdb,"allTypes","float");
        testAccess(iocdb,"allTypes","double");
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
        testAccess(iocdb,"allTypes","allTypes.enumArray");
        testAccess(iocdb,"allTypes","allTypes.menuArray");
        testAccess(iocdb,"allTypes","allTypes.linkArray");
        testAccess(iocdb,"allTypes","allTypes.structArray");
        testAccess(iocdb,"allTypes","allTypes.arrayArray");
    }
    
    static void testAccess(IOCDB iocdb,String recordName,String fieldName) {
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
        DBStructure parent = dbData.getParent();
        DBRecord record = dbData.getRecord();
        String parentName = "none";
        Field field = dbData.getField();
        if(parent!=null) parentName = ((Structure)parent.getField()).getStructureName();
        System.out.printf("record %s fieldRequested %s fieldActual %s parent %s\n",
                record.getRecordName(),
                fieldName,field.getName(),
                parentName);
        System.out.printf("    value %s\n",dbData.toString(1));
        Property[] property = field.getPropertys();
        if(property.length>0) {
            System.out.printf("property {\n");
            for(Property prop : property) {
                System.out.printf("    name %s field %s\n",prop.getName(),prop.getFieldName());
            }
            System.out.printf("}\n");
            for(Property prop : property) {
                System.out.printf("for propertyName %s propertyFieldName %s\n",
                        prop.getName(),prop.getFieldName());
                dbData = dbAccess.getPropertyField(prop);
                if(dbData==null) {
                    System.out.printf("    field not found\n");
                    continue;
                }
                parent = dbData.getParent();
                record = dbData.getRecord();
                parentName = "none";
                field = dbData.getField();
                if(parent!=null) parentName = ((Structure)parent.getField()).getStructureName();
                System.out.printf("    fieldActual %s parent %s\n",
                        field.getName(),parentName);
                System.out.printf("    value %s\n",dbData.toString(1));
            }
        }
    }
}
