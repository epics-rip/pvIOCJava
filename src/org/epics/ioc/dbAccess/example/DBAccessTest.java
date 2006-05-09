/**
 * 
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

import java.util.*;
import java.net.*;
public class DBAccessTest extends TestCase {
        
    public static void testXML() {
        DBD dbd = DBDFactory.create("test");
//        System.out.printf("parsing dbd file\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/accessDBD.xml");
        } catch (MalformedURLException e) {
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

        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
//        System.out.printf("parsing iocdb file\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/accessDB.xml");
        } catch (MalformedURLException e) {
            System.out.println("Exception: " + e);
        }
        

//        System.out.printf("\nrecords\n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        
        System.out.printf("\n");
        // Now test access
        testAccess(iocdb,"exampleAi","status");
        testAccess(iocdb,"exampleAi","timeStamp");
        testAccess(iocdb,"exampleAi","value");
        testAccess(iocdb,"exampleAi","rawValue");
        testAccess(iocdb,"exampleAi","aiLinear.aiRaw.value");
        testAccess(iocdb,"exampleAi","aiLinear.aiRaw.status");
        testAccess(iocdb,"exampleAi","aiLinear.aiRaw.input");
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
        testAccess(iocdb,"examplePowerSupplyArray","powerSupply[1]");
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
