/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.XMLToIOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.XMLToDBDFactory;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;

/**
 * JUnit test for DBListener.
 * @author mrk
 *
 */
public class ListenerTest extends TestCase {
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty();
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * test DBListener.
     */
    public static void testListener() {
        DBD dbd = DBDFactory.getMasterDBD(); 
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        Requester iocRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                "dbd/dbd.xml",iocRequester);
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
                "test/analog/analogDB.xml",iocRequester);
        XMLToIOCDBFactory.convert(dbd,iocdb,
                  "test/powerSupply/powerSupplyDB.xml",iocRequester);
        
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        System.out.printf("%ntest put and listen exampleAi%n");
        new DBListenerForTesting(iocdb,"aiRawCounter","scan",false,true);
        new DBListenerForTesting(iocdb,"ai","value",true,true);
        new DBListenerForTesting(iocdb,"ai","alarm.severity",false,true);
        new DBListenerForTesting(iocdb,"ai","input.value",true,true);
        new DBListenerForTesting(iocdb,"ai",null,false,true);
        testPut(iocdb,"aiRawCounter","scan.priority.index",2.0);
        testPut(iocdb,"ai","value",5.0);
        testPut(iocdb,"ai","input.value",2.0);
        testPut(iocdb,"ai","timeStamp.secondsPastEpoch",100.0);
        System.out.printf("%ntest put and listen examplePowerSupply%n");
        new DBListenerForTesting(iocdb,"psSimple","power.value");
        new DBListenerForTesting(iocdb,"psSimple","current.value");
        new DBListenerForTesting(iocdb,"psSimple","voltage.value");
        new DBListenerForTesting(iocdb,"psSimple",null);
        testPut(iocdb,"psSimple","current.value",25.0);
        testPut(iocdb,"psSimple","voltage.value",2.0);
        testPut(iocdb,"psSimple","power.value",50.0);
        System.out.printf("%ntest put and listen powerSupplyArray%n");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[0].power");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[0].current");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[0].voltage");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[1].power");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[1].current");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[1].voltage");
        testPut(iocdb,"powerSupplyArray","supply[0].current.value",25.0);
        testPut(iocdb,"powerSupplyArray","supply[0].voltage.value",2.0);
        testPut(iocdb,"powerSupplyArray","supply[0].power.value",50.0);
        testPut(iocdb,"powerSupplyArray","supply[1].current.value",2.50);
        testPut(iocdb,"powerSupplyArray","supply[1].voltage.value",1.00);
        testPut(iocdb,"powerSupplyArray","supply[1].power.value",2.50);
        testPut(iocdb,"powerSupplyArray","timeStamp",100.0);
    }
    
    static void testPut(IOCDB iocdb,String recordName,String fieldName,double value) {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("%nrecord %s not found%n",recordName);
            return;
        }
        PVField pvField = pvProperty.findProperty(pvRecord, fieldName);
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
        if(type!=Type.pvStructure) {
            System.out.printf("%ntestPut recordName %s fieldName %s cant handle%n",
                fieldName,recordName);
            return;
        }
        DBStructure dbStructure = (DBStructure)dbRecord.findDBField(pvField);
        DBField[] dbDatas = dbStructure.getDBFields();
        System.out.printf("%ntestPut begin structure put %s%n",
                recordName + pvField.getFullFieldName());
        dbRecord.beginProcess();
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
