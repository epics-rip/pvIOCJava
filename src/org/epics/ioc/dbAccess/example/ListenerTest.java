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
 * JUnit test for DBListener.
 * @author mrk
 *
 */
public class ListenerTest extends TestCase {
        
    /**
     * test DBListener.
     */
    public static void testListener() {
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
        System.out.printf("\n");
        new TestListener(iocdb,"exampleAiLinear","value");
        new TestListener(iocdb,"exampleAiLinear","aiLinear");
        new TestListener(iocdb,"exampleAiLinear",null);
        testPut(iocdb,"exampleAiLinear","rawValue",2.0);
        testPut(iocdb,"exampleAiLinear","value",5.0);
        testPut(iocdb,"exampleAiLinear","timeStamp",100.0);
        System.out.printf("\ntest put and listen examplePowerSupply\n");
        new TestListener(iocdb,"examplePowerSupply","power");
        new TestListener(iocdb,"examplePowerSupply","current");
        new TestListener(iocdb,"examplePowerSupply","voltage");
        new TestListener(iocdb,"examplePowerSupply","powerSupply");
        new TestListener(iocdb,"examplePowerSupply",null);
        testPut(iocdb,"examplePowerSupply","current",25.0);
        testPut(iocdb,"examplePowerSupply","voltage",2.0);
        testPut(iocdb,"examplePowerSupply","power",50.0);
        testPut(iocdb,"examplePowerSupply","timeStamp",100.0);
        System.out.printf("\ntest put and listen examplePowerSupplyArray\n");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0]");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1].power");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1].current");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1]");
        new TestListener(iocdb,"examplePowerSupplyArray",null);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].current",25.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage",2.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].power",50.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].current",2.50);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage",1.00);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].power",2.50);
        testPut(iocdb,"examplePowerSupplyArray","timeStamp",100.0);
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
        System.out.printf("dbData %s record %s\n",
            dbData.getField().getName(),record.getRecordName());
        DBStructure parent = dbData.getParent();
        while(parent!=null) {
            record = parent.getRecord();
            System.out.printf("     parent %s record %s\n",
                    parent.getField().getName(),record.getRecordName());
            parent = parent.getParent();
        }
        
    }
    
    private static class TestListener implements DBListener{
        public void newData(DBData dbData) {
            System.out.printf("TestListener recordName %s",recordName);
            if(fieldName!=null) {
                System.out.printf(" fieldName %s",fieldName);
            }
            System.out.printf(" actualField %s value %s\n",
                dbData.getField().getName(), dbData.toString());
        }

        TestListener(IOCDB iocdb,String recordName,String fieldName) {
            this.recordName = recordName;
            this.fieldName = fieldName;
            DBAccess dbAccess = iocdb.createAccess(recordName);
            if(dbAccess==null) {
                System.out.printf("record %s not found\n",recordName);
                return;
            }
            DBData dbData;
            if(fieldName==null || fieldName.length()==0) {
                dbData = dbAccess.getDbRecord();
                this.fieldName = null;
            } else {
                if(!dbAccess.setField(fieldName)){
                    System.out.printf("field %s of record %s not found\n",fieldName,recordName);
                    return;
                }
                dbData = dbAccess.getField();
            }
            dbData.addListener(this);
            Property[] property = dbData.getField().getPropertys();
            for(Property prop : property) {
                dbData = dbAccess.getPropertyField(prop);
                dbData.addListener(this);
            }
        }
        private String recordName;
        private String fieldName;
    }
    
    static void testPut(IOCDB iocdb,String recordName,String fieldName,double value) {
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
        Type type = dbData.getField().getType();
        if(type.isNumeric()) {
            System.out.printf("testPut recordName %s fieldName %s value %f\n",
                recordName,fieldName,value);
            convert.fromDouble(dbData,value);
            return;
        }
        if(type!=Type.pvStructure) {
            System.out.printf("testPut recordName %s fieldName %s cant handle\n",
                fieldName,recordName);
            return;
        }
        DBStructure structure = (DBStructure)dbData;
        DBData[] fields = structure.getFieldDBDatas();
        for(DBData field : fields) {
            if(field.getField().getType().isNumeric()) {
                System.out.printf("testPut recordName %s fieldName %s value %f\n",
                        recordName,field.getField().getName(),value);
                    convert.fromDouble(field,value);
            }
        }
    }
    
    private static Convert convert = ConvertFactory.getConvert();
}
