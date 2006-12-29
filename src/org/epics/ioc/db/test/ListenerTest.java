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
    
    private static class TestListener implements DBListener{ 
        private RecordListener listener;
        private String pvName = null;
        private String actualFieldName = null;
        private boolean isProcessing = false;
        private boolean changeOccured;
        private String fullName = null;
        
        TestListener(IOCDB iocdb,String recordName,String pvName) {
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
                    System.out.printf("%s\n",dbAccess.getDbRecord().toString());
                    return;
                }
                dbData = dbAccess.getField();
                actualFieldName = dbData.getField().getFieldName();
                fullName = dbData.getPVRecord().getRecordName() + dbData.getFullFieldName();
            }
            listener = dbData.getDBRecord().createRecordListener(this);
            dbData.addListener(listener);
            if(dbData.getField().getType()!=Type.pvStructure) {
                Property[] property = dbData.getField().getPropertys();
                for(Property prop : property) {
                    dbData = dbAccess.getPropertyField(prop);
                    dbData.addListener(listener);
                }
            }
        }
        
        private void putCommon(String message) {
            System.out.printf("%s %s isProcessing %b pvName %s actualFieldName %s%n",
                message,
                fullName,
                isProcessing,
                pvName,
                actualFieldName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#beginProcess()
         */
        public void beginProcess() {
            isProcessing = true;
            changeOccured = false;
            putCommon("beginProcess");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endProcess()
         */
        public void endProcess() {
            putCommon("endProcess changeOccured " + Boolean.toString(changeOccured));
            isProcessing = false;
            changeOccured = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#beginPut(org.epics.ioc.pv.PVStructure)
         */
        public void beginPut(PVStructure pvStructure) {
            DBData dbData = (DBData)pvStructure;
            String name = dbData.getPVRecord().getRecordName() + pvStructure.getFullFieldName();
            System.out.println("beginPut " + name);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#endPut(org.epics.ioc.pv.PVStructure)
         */
        public void endPut(PVStructure pvStructure) {
            DBData dbData = (DBData)pvStructure;
            String name = dbData.getPVRecord().getRecordName() + pvStructure.getFullFieldName();
            System.out.println("endPut " + name);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#configurationStructurePut(org.epics.ioc.pv.PVLink)
         */
        public void configurationStructurePut(PVLink pvLink) {
            putCommon("configurationStructurePut");
            String name = pvLink.getPVRecord().getRecordName() + pvLink.getFullFieldName();
            if(!name.equals(fullName)) {
                System.out.printf("%s NOT_EQUAL %s%n",name,fullName);
            }
            System.out.printf("%n    %s = %s%n",
                name,pvLink.getConfigurationStructure().toString(2));
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#dataPut(org.epics.ioc.db.DBData)
         */
        public void dataPut(DBData dbData) {
            putCommon("dataPut");
            changeOccured = true;
            String name = dbData.getPVRecord().getRecordName() + dbData.getFullFieldName();
            if(!name.equals(fullName)) {
                System.out.printf("%s NOT_EQUAL %s%n",name,fullName);
            }
            System.out.printf("    %s = %s%n",
                name,dbData.toString(2));
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#enumChoicesPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumChoicesPut(PVEnum pvEnum) {
            putCommon("enumChoicesPut");
            String name = pvEnum.getPVRecord().getRecordName() + pvEnum.getFullFieldName();
            if(!name.equals(fullName)) {
                System.out.printf("%s NOT_EQUAL %s%n",name,fullName);
            }
            System.out.printf("    %s = %s%n",
                name,pvEnum.toString(2));
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#enumIndexPut(org.epics.ioc.pv.PVEnum)
         */
        public void enumIndexPut(PVEnum pvEnum) {
            putCommon("enumIndexPut");
            String name = pvEnum.getPVRecord().getRecordName() + pvEnum.getFullFieldName();
            if(!name.equals(fullName)) {
                System.out.printf("%s NOT_EQUAL %s%n",name,fullName);
            }
            System.out.printf("    %s index = %d%n",
                name,pvEnum.getIndex());
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#structurePut(org.epics.ioc.pv.PVStructure, org.epics.ioc.db.DBData)
         */
        public void structurePut(PVStructure pvStructure, DBData dbData) {
            putCommon("structurePut");
            changeOccured = true;
            String recordName = dbData.getPVRecord().getRecordName();
            String name = recordName + dbData.getFullFieldName();
            System.out.printf("    %s = %s%n",
                name,dbData.toString(2));
            String structureName = recordName + pvStructure.getFullFieldName();
            System.out.printf("    structure is %s%n",structureName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.DBListener#supportNamePut(org.epics.ioc.db.DBData)
         */
        public void supportNamePut(DBData dbData) {
            // TODO Auto-generated method stub
            
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
