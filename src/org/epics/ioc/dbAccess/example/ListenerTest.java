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
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * test DBListener.
     */
    public static void testListener() {
        DBD dbd = DBDFactory.create("master",null); 
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase",null);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/listenerDBD.xml");
        
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
                 "src/org/epics/ioc/dbAccess/example/listenerDB.xml");
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        System.out.printf("%ntest put and listen exampleAiLinear%n");
        new TestListener(iocdb,"exampleAiLinear","value");
        new TestListener(iocdb,"exampleAiLinear","aiLinear");
        new TestListener(iocdb,"exampleAiLinear",null);
        testPut(iocdb,"exampleAiLinear","rawValue",2.0);
        testPut(iocdb,"exampleAiLinear","value",5.0);
        testPut(iocdb,"exampleAiLinear","timeStamp",100.0);
        System.out.printf("%ntest put and listen examplePowerSupply%n");
        new TestListener(iocdb,"examplePowerSupply","power");
        new TestListener(iocdb,"examplePowerSupply","current");
        new TestListener(iocdb,"examplePowerSupply","voltage");
        new TestListener(iocdb,"examplePowerSupply","powerSupply");
        new TestListener(iocdb,"examplePowerSupply",null);
        testPut(iocdb,"examplePowerSupply","current",25.0);
        testPut(iocdb,"examplePowerSupply","voltage",2.0);
        testPut(iocdb,"examplePowerSupply","power",50.0);
        testPut(iocdb,"examplePowerSupply","timeStamp",100.0);
        System.out.printf("%ntest masterListener examplePowerSupply%n");
        testPut(iocdb,"examplePowerSupply","powerSupply",0.5);
        System.out.printf("%ntest put and listen examplePowerSupplyArray%n");
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
    
    private static class TestListener implements DBListener{ 
        private RecordListener listener;
        private String recordName;
        private String pvName = null;
        private String actualFieldName = null;
        private boolean synchronousData = false;
        
        TestListener(IOCDB iocdb,String recordName,String pvName) {
            this.recordName = recordName;
            this.pvName = pvName;
            DBAccess dbAccess = iocdb.createAccess(recordName);
            if(dbAccess==null) {
                System.out.printf("record %s not found%n",recordName);
                return;
            }
            DBData dbData;
            if(pvName==null || pvName.length()==0) {
                dbData = dbAccess.getDbRecord();
            } else {
                if(dbAccess.setField(pvName)!=AccessSetResult.thisRecord){
                    System.out.printf("name %s not in record %s%n",pvName,recordName);
                    return;
                }
                dbData = dbAccess.getField();
                actualFieldName = dbData.getField().getName();
            }
            listener = dbData.getRecord().createListener(this);
            dbData.addListener(listener);
            if(dbData.getField().getType()!=Type.pvStructure) {
                Property[] property = dbData.getField().getPropertys();
                for(Property prop : property) {
                    dbData = dbAccess.getPropertyField(prop);
                    dbData.addListener(listener);
                }
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#beginSynchronous()
         */
        public void beginSynchronous() {
            System.out.printf("TestListener start synchronous data pvName %s%n",pvName);
            synchronousData = true;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
          System.out.printf("TestListener end synchronous data pvName %s%n",pvName);
          synchronousData = false;
      }


        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData dbData) {
            System.out.printf("TestListener recordName %s is Synchronous %b"
                    + " pvName %s actualFieldName %s%n",
                recordName,
                synchronousData,
                pvName,
                actualFieldName);
            String dbDataName = dbData.getField().getName();
            DBData parent = dbData.getParent();
            while(parent!=dbData.getRecord()) {
                dbDataName = parent.getField().getName() + "." + dbDataName;
                parent = parent.getParent();
            }
            System.out.printf("    dbDataName %s value %s%n",
                dbDataName,dbData.toString());    
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
         */
        public void unlisten(RecordListener listener) {
            // Nothing to do.
        }
        
    }
    
    static void testPut(IOCDB iocdb,String recordName,String fieldName,double value) {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s%n",fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        Type type = dbData.getField().getType();
        if(type.isNumeric()) {
            System.out.printf("testPut recordName %s fieldName %s value %f%n",
                recordName,fieldName,value);
            convert.fromDouble(dbData,value);
            return;
        }
        if(type!=Type.pvStructure) {
            System.out.printf("testPut recordName %s fieldName %s cant handle%n",
                fieldName,recordName);
            return;
        }
        DBStructure structure = (DBStructure)dbData;
        DBRecord dbRecord = structure.getRecord();
        DBData[] fields = structure.getFieldDBDatas();
        dbRecord.beginSynchronous();
        for(DBData field : fields) {
            Type fieldType = field.getField().getType();
            if(fieldType.isNumeric()) {
                System.out.printf("testPut recordName %s fieldName %s value %f%n",
                        recordName,field.getField().getName(),value);
                    convert.fromDouble(field,value);
            } else if (fieldType==Type.pvString) {
                String valueString = Double.toString(value);
                System.out.printf("testPut recordName %s fieldName %s value %s%n",
                        recordName,field.getField().getName(),valueString);
                DBString dbString = (DBString)field;
                dbString.put(valueString);
            }
            
        }
        dbRecord.endSynchronous();
    }
}
