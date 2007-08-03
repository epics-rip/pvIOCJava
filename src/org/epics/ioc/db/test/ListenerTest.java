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
import org.epics.ioc.util.Requester;
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
        Requester iocRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/db/test/listenerDBD.xml",iocRequester);
        
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
                 "src/org/epics/ioc/db/test/listenerDB.xml",iocRequester);
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        System.out.printf("%ntest put and listen exampleAi%n");
        new DBListenerForTesting(iocdb,"exampleAi","value");
        new DBListenerForTesting(iocdb,"exampleAi","priority");
        new DBListenerForTesting(iocdb,"exampleAi","input");
        new DBListenerForTesting(iocdb,"exampleAi",null);
        new DBListenerForTesting(iocdb,"exampleAi","input.aiRaw.input");
        testPut(iocdb,"exampleAi","priority",2.0);
        testPut(iocdb,"exampleAi","rawValue",2.0);
        testPut(iocdb,"exampleAi","value",5.0);
        testPut(iocdb,"exampleAi","timeStamp",100.0);
        testPut(iocdb,"exampleAi","input.aiRaw.input",1.0);
        System.out.printf("%ntest put and listen examplePowerSupply%n");
        new DBListenerForTesting(iocdb,"examplePowerSupply","power");
        new DBListenerForTesting(iocdb,"examplePowerSupply","current");
        new DBListenerForTesting(iocdb,"examplePowerSupply","voltage");
        new DBListenerForTesting(iocdb,"examplePowerSupply","powerSupply");
        new DBListenerForTesting(iocdb,"examplePowerSupply",null);
        testPut(iocdb,"examplePowerSupply","current",25.0);
        testPut(iocdb,"examplePowerSupply","voltage",2.0);
        testPut(iocdb,"examplePowerSupply","power",50.0);
        testPut(iocdb,"examplePowerSupply","timeStamp",100.0);
        System.out.printf("%ntest masterListener examplePowerSupply%n");
        testPut(iocdb,"examplePowerSupply","powerSupply",0.5);
        System.out.printf("%ntest put and listen examplePowerSupplyArray%n");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0]");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1].power");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1].current");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1]");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray",null);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].current",25.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage",2.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].power",50.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].current",2.50);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage",1.00);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].power",2.50);
        testPut(iocdb,"examplePowerSupplyArray","timeStamp",100.0);
    }
    
    static void testPut(IOCDB iocdb,String recordName,String fieldName,double value) {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("%nrecord %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField pvField = pvAccess.findField(fieldName);
        if(pvField==null){
            System.out.printf("%nfield %s not in record %s%n",fieldName,recordName);
            return;
        }
        
        DBField dbField = dbRecord.findDBField(pvField);
        Type type = pvField.getField().getType();
        if(type.isNumeric()) {
            System.out.printf("%ntestPut recordName %s fieldName %s value %f%n",
                recordName,fieldName,value);
            convert.fromDouble(pvField,value);
            dbField.postPut();
            return;
        }
        if(type==Type.pvEnum) {
            System.out.printf("%ntestPut enum index recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            int index = (int)value;
            DBEnum dbEnum = (DBEnum)dbField;
            dbEnum.setIndex(index);
            System.out.printf("%ntestPut enum choices recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            String[] choice = dbEnum.getChoices();
            dbEnum.setChoices(choice);
            return;
        }
        if(type==Type.pvMenu) {
            System.out.printf("%ntestPut menu index recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            int index = (int)value;
            DBMenu dbMenu = (DBMenu)dbField;
            dbMenu.setIndex(index);
            return;
        }
        if(type==Type.pvLink) {
            System.out.printf("%ntestPut link configurationStructure recordName %s fieldName %s value %f%n",
                    recordName,fieldName,value);
            DBLink dbLink = (DBLink)dbField;
            PVStructure configStructure = dbLink.getConfigurationStructure();
            System.out.println("put supportName");
            String supportName = dbLink.getSupportName();
            String result = dbLink.setSupportName(supportName);
            if(result!=null) {
                System.out.println("setSupportName failed " + result);
                return;
            }
            System.out.println("setConfigurationStructure");
            result = dbLink.setConfigurationStructure(configStructure);
            if(result!=null) {
                System.out.println("setConfigurationStructure failed " + result);
                return;
            }
            return;
        }
        if(type!=Type.pvStructure) {
            System.out.printf("%ntestPut recordName %s fieldName %s cant handle%n",
                fieldName,recordName);
            return;
        }
        DBStructure dbStructure = (DBStructure)dbRecord.findDBField(pvField);
        DBField[] dbDatas = dbStructure.getFieldDBFields();
        System.out.printf("%ntestPut begin structure put %s%n",
                recordName + pvField.getFullFieldName());
        dbRecord.beginProcess();
        dbStructure.beginPut();
        for(DBField field : dbDatas) {
            PVField pv = field.getPVField();
            Type fieldType = pv.getField().getType();
            if(fieldType.isNumeric()) {
                System.out.printf("testPut recordName %s fieldName %s value %f%n",
                        recordName,pv.getField().getFieldName(),value);
                    convert.fromDouble(pv,value);
            } else if (fieldType==Type.pvString) {
                String valueString = Double.toString(value);
                System.out.printf("testPut recordName %s fieldName %s value %s%n",
                        recordName,pv.getField().getFieldName(),valueString);
                PVString pvString = (PVString)pv;
                pvString.put(valueString);
            } else {
                continue;
            }
            field.postPut();
        }
        dbStructure.endPut();
        dbRecord.endProcess();
    }
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "ListenerTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
