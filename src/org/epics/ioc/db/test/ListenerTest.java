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
import org.epics.ioc.util.Requestor;
import org.epics.ioc.util.MessageType;

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
        DBD dbd = DBDFactory.getMasterDBD(); 
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        Requestor iocRequestor = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/db/test/listenerDBD.xml",iocRequestor);
        
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
                 "src/org/epics/ioc/db/test/listenerDB.xml",iocRequestor);
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        System.out.printf("%ntest put and listen exampleAi%n");
        new TestListener(iocdb,"exampleAi","value");
        new TestListener(iocdb,"exampleAi","input");
        new TestListener(iocdb,"exampleAi",null);
        testPut(iocdb,"exampleAi","rawValue",2.0);
        testPut(iocdb,"exampleAi","value",5.0);
        testPut(iocdb,"exampleAi","timeStamp",100.0);
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
        private boolean isProcessing = false;
        private boolean changeOccured;
        
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
                actualFieldName = dbData.getField().getFieldName();
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
         * @see org.epics.ioc.dbAccess.DBListener#beginProcess()
         */
        public void beginProcess() {
            System.out.printf("TestListener start processing pvName %s%n",pvName);
            isProcessing = true;
            changeOccured = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endProcess()
         */
        public void endProcess() {
          System.out.printf("TestListener end processing pvName %s changeOccured %b%n",pvName,changeOccured);
          isProcessing = false;
      }


        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.pv.PVStructure)
         */
        public void beginPut(PVStructure pvStructure) {
            DBData dbData = (DBData)pvStructure;
            String name = dbData.getRecord().getRecordName() + pvStructure.getFullFieldName();
            System.out.println("beginPut " + name);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.pv.PVStructure)
         */
        public void endPut(PVStructure pvStructure) {
            DBData dbData = (DBData)pvStructure;
            String name = dbData.getRecord().getRecordName() + pvStructure.getFullFieldName();
            System.out.println("endPut " + name);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(PVStructure pvStructure,DBData dbData) {
            if(pvStructure!=null)
            changeOccured = true;
            System.out.printf("%s isProcessing %b"
                    + " pvName %s actualFieldName %s%n",
                recordName,
                isProcessing,
                pvName,
                actualFieldName);
            String recordName = dbData.getRecord().getRecordName();
            String dbDataName = recordName + dbData.getFullFieldName();
            System.out.printf("    %s = %s%n",
                dbDataName,dbData.toString(2));
            if(pvStructure!=null) {
                String structureName = recordName + pvStructure.getFullFieldName();
                System.out.printf("    structure is %s%n",structureName);
            }
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
            System.out.printf("%nrecord %s not found%n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("%nfield %s not in record %s%n",fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        Type type = dbData.getField().getType();
        if(type.isNumeric()) {
            System.out.printf("%ntestPut recordName %s fieldName %s value %f%n",
                recordName,fieldName,value);
            convert.fromDouble(dbData,value);
            return;
        }
        if(type!=Type.pvStructure) {
            System.out.printf("%ntestPut recordName %s fieldName %s cant handle%n",
                fieldName,recordName);
            return;
        }
        PVStructure structure = (PVStructure)dbData;
        DBRecord dbRecord = dbData.getRecord();
        PVData[] fields = structure.getFieldPVDatas();
        dbRecord.beginProcess();
        structure.beginPut();
        for(PVData field : fields) {
            Type fieldType = field.getField().getType();
            if(fieldType.isNumeric()) {
                System.out.printf("%ntestPut recordName %s fieldName %s value %f%n",
                        recordName,field.getField().getFieldName(),value);
                    convert.fromDouble(field,value);
            } else if (fieldType==Type.pvString) {
                String valueString = Double.toString(value);
                System.out.printf("%ntestPut recordName %s fieldName %s value %s%n",
                        recordName,field.getField().getFieldName(),valueString);
                PVString pvString = (PVString)field;
                pvString.put(valueString);
            }
            
        }
        structure.endPut();
        dbRecord.endProcess();
    }
    
    private static class Listener implements Requestor {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "ListenerTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
