/**
 * 
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

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
        showParent(iocdb,"exampleAi","status");
        showParent(iocdb,"exampleAi","timeStamp");
        showParent(iocdb,"exampleAi","value");
        showParent(iocdb,"exampleAi","rawValue");
        showParent(iocdb,"exampleAi","aiLinear.aiRaw.value");
        showParent(iocdb,"exampleAi","aiLinear.aiRaw.status");
        showParent(iocdb,"exampleAi","aiLinear.aiRaw.input");
        showParent(iocdb,"exampleAi","aiLinear.aiRaw.input.configStructureName");
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
        System.out.printf("\ntest put and listen exampleAi\n");
        new TestListener(iocdb,"exampleAi","value");
        new TestListener(iocdb,"exampleAi","aiLinear");
        new TestListener(iocdb,"exampleAi",null);
        testPut(iocdb,"exampleAi","rawValue",2.0);
        testPut(iocdb,"exampleAi","value",5.0);
        testPut(iocdb,"exampleAi","timeStamp",100.0);
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
