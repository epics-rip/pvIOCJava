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
        new TestListener(iocdb,"exampleAi","priority");
        new TestListener(iocdb,"exampleAi","input");
        new TestListener(iocdb,"exampleAi",null);
        new TestListener(iocdb,"exampleAi","input.aiRaw.input");
        testPut(iocdb,"exampleAi","priority",2.0);
        testPut(iocdb,"exampleAi","rawValue",2.0);
        testPut(iocdb,"exampleAi","value",5.0);
        testPut(iocdb,"exampleAi","timeStamp",100.0);
        testPut(iocdb,"exampleAi","input.aiRaw.input",1.0);
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
        if(type==Type.pvEnum) {
            System.out.printf("%ntestPut enum index recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            int index = (int)value;
            PVEnum pvEnum = (PVEnum)dbData;
            pvEnum.setIndex(index);
            System.out.printf("%ntestPut enum choices recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            String[] choice = pvEnum.getChoices();
            pvEnum.setChoices(choice);
            return;
        }
        if(type==Type.pvMenu) {
            System.out.printf("%ntestPut menu index recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            int index = (int)value;
            PVEnum pvEnum = (PVEnum)dbData;
            pvEnum.setIndex(index);
            return;
        }
        if(type==Type.pvLink) {
            System.out.printf("%ntestPut link configurationStructure recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            PVLink pvLink = (PVLink)dbData;
            PVStructure configStructure = pvLink.getConfigurationStructure();
            boolean boolResult = pvLink.setConfigurationStructure(configStructure);
            if(!boolResult) {
                System.out.println("setConfigurationStructure failed");
                return;
            }
            System.out.println("change supportName");
            String supportName = pvLink.getSupportName();
            String result = pvLink.setSupportName(supportName);
            if(result!=null) {
                System.out.println("setSupportName failed " + result);
                return;
            }
            System.out.println("setConfigurationStructure");
            boolResult = pvLink.setConfigurationStructure(configStructure);
            if(!boolResult) {
                System.out.println("setConfigurationStructure failed");
                return;
            }
            return;
        }
        if(type!=Type.pvStructure) {
            System.out.printf("%ntestPut recordName %s fieldName %s cant handle%n",
                fieldName,recordName);
            return;
        }
        PVStructure structure = (PVStructure)dbData;
        DBRecord dbRecord = dbData.getDBRecord();
        PVData[] fields = structure.getFieldPVDatas();
        System.out.printf("%ntestPut begin structure put %s%n",
                recordName + dbData.getFullFieldName());
        dbRecord.beginProcess();
        structure.beginPut();
        for(PVData field : fields) {
            Type fieldType = field.getField().getType();
            if(fieldType.isNumeric()) {
                System.out.printf("testPut recordName %s fieldName %s value %f%n",
                        recordName,field.getField().getFieldName(),value);
                    convert.fromDouble(field,value);
            } else if (fieldType==Type.pvString) {
                String valueString = Double.toString(value);
                System.out.printf("testPut recordName %s fieldName %s value %s%n",
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
