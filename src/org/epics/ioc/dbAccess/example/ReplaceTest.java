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
 * JUnit test for replacing the default data implementration for a field.
 * @author mrk
 *
 */
public class ReplaceTest extends TestCase {
        
    /**
     * test replacing the default data implementration for a field.
     */
    public static void testReplaceField() {
        DBD dbd = DBDFactory.create("test"); 
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        System.out.printf("reading menuStructureSupport\n");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/menuStructureSupportDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        
        System.out.printf("reading aiDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/aiDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading powerSupplyDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/powerSupplyDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading allTypesDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/allTypesDBD.xml");
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
                 "src/org/epics/ioc/dbAccess/example/exampleAiLinearDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading examplePowerSupplyDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/dbAccess/example/examplePowerSupplyDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading examplePowerSupplyArrayDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/dbAccess/example/examplePowerSupplyArrayDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading exampleAllTypeDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/dbAccess/example/exampleAllTypeDB.xml");
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
        
        System.out.printf("\ntest replaceField\n");
        testReplace(iocdb,"exampleAiLinear","rawValue");
        testReplace(iocdb,"exampleAiLinear","value");
        System.out.printf("\n");
        new TestListener(iocdb,"exampleAiLinear","rawValue");
        new TestListener(iocdb,"exampleAiLinear","value");
        testPut(iocdb,"exampleAiLinear","rawValue",2.0);
        testPut(iocdb,"exampleAiLinear","value",5.0);
        System.out.printf("\ntest put and listen examplePowerSupply\n");
        testReplace(iocdb,"examplePowerSupply","power");
        testReplace(iocdb,"examplePowerSupply","current");
        testReplace(iocdb,"examplePowerSupply","voltage");
        new TestListener(iocdb,"examplePowerSupply","power");
        new TestListener(iocdb,"examplePowerSupply","current");
        new TestListener(iocdb,"examplePowerSupply","voltage");
        testPut(iocdb,"examplePowerSupply","current",25.0);
        testPut(iocdb,"examplePowerSupply","voltage",2.0);
        testPut(iocdb,"examplePowerSupply","power",50.0);
        System.out.printf("\ntest put and listen examplePowerSupplyArray\n");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[1].power");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[1].current");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1].power");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1].current");
        new TestListener(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage");
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].current",25.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage",2.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].power",50.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].current",2.50);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage",1.00);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].power",2.50);
        System.out.printf("\ntest put and listen allTypes\n");
        testReplace(iocdb,"allTypes","boolean");
        testReplace(iocdb,"allTypes","byte");
        testReplace(iocdb,"allTypes","short");
        testReplace(iocdb,"allTypes","int");
        testReplace(iocdb,"allTypes","long");
        testReplace(iocdb,"allTypes","float");
        testReplace(iocdb,"allTypes","double");
        testReplace(iocdb,"allTypes","string");
        testReplace(iocdb,"allTypes","booleanArray");
        testReplace(iocdb,"allTypes","byteArray");
        testReplace(iocdb,"allTypes","shortArray");
        testReplace(iocdb,"allTypes","intArray");
        testReplace(iocdb,"allTypes","longArray");
        testReplace(iocdb,"allTypes","floatArray");
        testReplace(iocdb,"allTypes","doubleArray");
        testReplace(iocdb,"allTypes","enumArray");
        testReplace(iocdb,"allTypes","menuArray");
        testReplace(iocdb,"allTypes","linkArray");
        testReplace(iocdb,"allTypes","structArray");
        testReplace(iocdb,"allTypes","arrayArray");
        testReplace(iocdb,"allTypes","allTypes.boolean");
        testReplace(iocdb,"allTypes","allTypes.byte");
        testReplace(iocdb,"allTypes","allTypes.short");
        testReplace(iocdb,"allTypes","allTypes.int");
        testReplace(iocdb,"allTypes","allTypes.long");
        testReplace(iocdb,"allTypes","allTypes.float");
        testReplace(iocdb,"allTypes","allTypes.double");
        testReplace(iocdb,"allTypes","allTypes.string");
        testReplace(iocdb,"allTypes","allTypes.booleanArray");
        testReplace(iocdb,"allTypes","allTypes.byteArray");
        testReplace(iocdb,"allTypes","allTypes.shortArray");
        testReplace(iocdb,"allTypes","allTypes.intArray");
        testReplace(iocdb,"allTypes","allTypes.longArray");
        testReplace(iocdb,"allTypes","allTypes.floatArray");
        testReplace(iocdb,"allTypes","allTypes.doubleArray");
        testReplace(iocdb,"allTypes","allTypes.enumArray");
        testReplace(iocdb,"allTypes","allTypes.menuArray");
        testReplace(iocdb,"allTypes","allTypes.linkArray");
        testReplace(iocdb,"allTypes","allTypes.structArray");
        testReplace(iocdb,"allTypes","allTypes.arrayArray");
        
        new TestListener(iocdb,"allTypes","boolean");
        new TestListener(iocdb,"allTypes","byte");
        new TestListener(iocdb,"allTypes","short");
        new TestListener(iocdb,"allTypes","int");
        new TestListener(iocdb,"allTypes","long");
        new TestListener(iocdb,"allTypes","float");
        new TestListener(iocdb,"allTypes","double");
        new TestListener(iocdb,"allTypes","string");
        new TestListener(iocdb,"allTypes","booleanArray");
        new TestListener(iocdb,"allTypes","byteArray");
        new TestListener(iocdb,"allTypes","shortArray");
        new TestListener(iocdb,"allTypes","intArray");
        new TestListener(iocdb,"allTypes","longArray");
        new TestListener(iocdb,"allTypes","floatArray");
        new TestListener(iocdb,"allTypes","doubleArray");
        new TestListener(iocdb,"allTypes","enumArray");
        new TestListener(iocdb,"allTypes","menuArray");
        new TestListener(iocdb,"allTypes","linkArray");
        new TestListener(iocdb,"allTypes","structArray");
        new TestListener(iocdb,"allTypes","arrayArray");
        new TestListener(iocdb,"allTypes","allTypes.boolean");
        new TestListener(iocdb,"allTypes","allTypes.byte");
        new TestListener(iocdb,"allTypes","allTypes.short");
        new TestListener(iocdb,"allTypes","allTypes.int");
        new TestListener(iocdb,"allTypes","allTypes.long");
        new TestListener(iocdb,"allTypes","allTypes.float");
        new TestListener(iocdb,"allTypes","allTypes.double");
        new TestListener(iocdb,"allTypes","allTypes.string");
        new TestListener(iocdb,"allTypes","allTypes.booleanArray");
        new TestListener(iocdb,"allTypes","allTypes.byteArray");
        new TestListener(iocdb,"allTypes","allTypes.shortArray");
        new TestListener(iocdb,"allTypes","allTypes.intArray");
        new TestListener(iocdb,"allTypes","allTypes.longArray");
        new TestListener(iocdb,"allTypes","allTypes.floatArray");
        new TestListener(iocdb,"allTypes","allTypes.doubleArray");
        new TestListener(iocdb,"allTypes","allTypes.enumArray");
        new TestListener(iocdb,"allTypes","allTypes.menuArray");
        new TestListener(iocdb,"allTypes","allTypes.linkArray");
        new TestListener(iocdb,"allTypes","allTypes.structArray");
        new TestListener(iocdb,"allTypes","allTypes.arrayArray");
//        testPutBoolean(iocdb,"allTypes","boolean",true);
//        testPut(iocdb,"allTypes","byte",1.0);
//        testPut(iocdb,"allTypes","short",2.0);
//        testPut(iocdb,"allTypes","int",3.0);
//        testPut(iocdb,"allTypes","long",4.0);
//        testPut(iocdb,"allTypes","float",5.0);
//        testPut(iocdb,"allTypes","double",6.0);
//        testPutString(iocdb,"allTypes","string","test string");
//        testPutArray(iocdb,"allTypes","byteArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","shortArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","intArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","longArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","floatArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","doubleArray",1.0,2.0,3.0);
//        testPutBoolean(iocdb,"allTypes","allTypes.boolean",true);
//        testPut(iocdb,"allTypes","allTypes.byte",1.0);
//        testPut(iocdb,"allTypes","allTypes.short",2.0);
//        testPut(iocdb,"allTypes","allTypes.int",3.0);
//        testPut(iocdb,"allTypes","allTypes.long",4.0);
//        testPut(iocdb,"allTypes","allTypes.float",5.0);
//        testPut(iocdb,"allTypes","allTypes.double",6.0);
//        testPutString(iocdb,"allTypes","allTypes.string","test string");
//        testPutArray(iocdb,"allTypes","allTypes.booleanArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","allTypes.byteArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","allTypes.shortArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","allTypes.intArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","allTypes.longArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","allTypes.floatArray",1.0,2.0,3.0);
//        testPutArray(iocdb,"allTypes","allTypes.doubleArray",1.0,2.0,3.0);
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
                System.out.printf("record %s not found\n",recordName);
                return;
            }
            DBData dbData;
            if(pvName==null || pvName.length()==0) {
                dbData = dbAccess.getDbRecord();
            } else {
                if(dbAccess.setField(pvName)!=AccessSetResult.thisRecord){
                    System.out.printf("name %s not in record %s\n",pvName,recordName);
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
            synchronousData = true;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#endSynchronous()
         */
        public void endSynchronous() {
            synchronousData = false;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData dbData) {
            System.out.printf(" actualField %s value %s\n",
                dbData.getField().getName(), dbData.toString());
            System.out.printf("TestListener recordName %s is Synchronous %b"
                    + " pvName %s actualFieldName %s",
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
            String value = dbData.toString();
            System.out.printf("    dbDataName %s value %s\n",
                dbDataName,value);    
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBListener#unlisten(org.epics.ioc.dbAccess.RecordListener)
         */
        public void unlisten(RecordListener listener) {
            // Nothing to do.
        }
    }

    static void testPut(IOCDB iocdb,String recordName,
        String fieldName,double value)
    {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s\n",
                fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        Type type = dbData.getField().getType();
        if(type.isNumeric()) {
            System.out.printf("\ntestPut recordName %s fieldName %s value %f",
                recordName,fieldName,value);
            convert.fromDouble(dbData,value);
            return;
        }
        if(type!=Type.pvStructure) {
            System.out.printf("\ntestPut recordName %s fieldName %s cant handle\n",
                fieldName,recordName);
            return;
        }
        DBStructure structure = (DBStructure)dbData;
        DBData[] fields = structure.getFieldDBDatas();
        for(DBData field : fields) {
            if(field.getField().getType().isNumeric()) {
                System.out.printf("\ntestPut recordName %s fieldName %s value %f",
                        recordName,field.getField().getName(),value);
                    convert.fromDouble(field,value);
            }
        }
    }
    
    static void testPutArray(IOCDB iocdb,String recordName,
            String fieldName,double value1,double value2,double value3)
    {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s\n",
                fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        Type type = dbData.getField().getType();
        if(type!=Type.pvArray) {
            System.out.printf("\ntestPutArray recordName %s fieldName %s no an array\n",
                    fieldName,recordName);
                return;
        }
        DBArray dataArray = (DBArray)dbData;
        Type elementType = ((Array)dataArray.getField()).getElementType();
        if(elementType.isNumeric()) {
            System.out.printf("\ntestPut recordName %s fieldName %s values %f %f %f",
                recordName,fieldName,value1,value2,value3);
            double[] values = new double[]{value1,value2,value3};
            convert.fromDoubleArray(dbData,0,3,values,0);
            return;
        } else {
            System.out.printf("\ntestPut recordName %s fieldName %s cant handle\n",
                    fieldName,recordName);
            return;
        }
    }
    
    static void testPutBoolean(IOCDB iocdb,String recordName,
            String fieldName,boolean value)
    {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s\n",
                fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        Type type = dbData.getField().getType();
        if(type==Type.pvBoolean) {
            DBBoolean data = (DBBoolean)dbData;
            System.out.printf("\ntestPutBoolean recordName %s fieldName %s value %b",
                recordName,fieldName,value);
            data.put(value);
            return;
        }
    }
    
    static void testPutString(IOCDB iocdb,String recordName,
            String fieldName,String value)
    {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s\n",
                fieldName,recordName);
            return;
        }
        DBData dbData = dbAccess.getField();
        Type type = dbData.getField().getType();
        if(type==Type.pvString) {
            DBString data = (DBString)dbData;
            System.out.printf("\ntestPutString recordName %s fieldName %s value %s",
                recordName,fieldName,value);
            data.put(value);
            return;
        }
    }
    
    private static void testReplace(IOCDB iocdb,String recordName,
        String fieldName)
    {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(dbAccess.setField(fieldName)!=AccessSetResult.thisRecord){
            System.out.printf("field %s not in record %s\n",
                fieldName,recordName);
            return;
        }
        DBData oldField = dbAccess.getField();
        DBStructure parent = oldField.getParent();
        DBDField dbdField = oldField.getDBDField();
        DBType dbType = dbdField.getDBType();
        DBData newField = null;
        switch(dbType) {
        case dbPvType:
            Type type = dbdField.getType();
            switch(type) {
            case pvUnknown:
                 System.out.printf("type is pvUnknown. Why???\n");
                 return;
            case pvBoolean:
                 newField = new BooleanData(parent,dbdField);
                 break;
            case pvByte:
                 newField = new ByteData(parent,dbdField);
                 break;
            case pvShort:
                 newField = new ShortData(parent,dbdField);
                 break;
            case pvInt:
                 newField = new IntData(parent,dbdField);
                 break;
            case pvLong:
                 newField = new LongData(parent,dbdField);
                 break;
            case pvFloat:
                 newField = new FloatData(parent,dbdField);
                 break;
            case pvDouble:
                 newField = new DoubleData(parent,dbdField);
                 break;
            case pvString:
                 newField = new StringData(parent,dbdField);
                 break;
            case pvEnum:
                 System.out.printf("pvEnum not supported.\n");
                 return;
            }
            break;
        case dbMenu:
             System.out.printf("dbMenu not supported.\n");
             return;
        case dbStructure:
             System.out.printf("dbStructure not supported.\n");
             return;
        case dbArray:
             DBType elementDbType= dbdField.getAttribute().getElementDBType();
             switch(elementDbType) {
             case dbPvType: {
                     Type elementType = dbdField.getAttribute().getElementType();
                     switch(elementType) {
                     case pvBoolean:
                          newField = new BooleanArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvByte:
                          newField = new ByteArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvShort:
                          newField = new ShortArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvInt:
                          newField = new IntArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvLong:
                          newField = new LongArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvFloat:
                          newField = new FloatArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvDouble:
                          newField = new DoubleArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvString:
                          newField = new StringArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvEnum:
                          newField = new EnumArray(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     }
                 }
                 break;
             case dbMenu:
                 newField = new MenuArray(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             case dbStructure:
                 newField = new StructureArray(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             case dbArray:
                 newField = new ArrayArray(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             case dbLink:
                 newField = new LinkArray(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             }
             break;
        case dbLink:
             System.out.printf("dbLink not supported.\n");
             return;
        }
        dbAccess.replaceField(oldField,newField);
    }
    
    
    private static class BooleanData extends AbstractDBData
        implements DBBoolean
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBoolean#get()
         */
        public boolean get() {
            System.out.printf("\n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        BooleanData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = false;
        }
        
        private boolean value;

    }

    private static class ByteData extends AbstractDBData implements DBByte {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByte#get()
         */
        public byte get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByte#put(byte)
         */
        public void put(byte value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ByteData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
        }
        
        private byte value;

    }

    private static class ShortData extends AbstractDBData implements DBShort {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShort#get()
         */
        public short get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShort#put(short)
         */
        public void put(short value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ShortData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
        }
        
        private short value;

    }

    private static class IntData extends AbstractDBData implements DBInt {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVInt#get()
         */
        public int get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVInt#put(int)
         */
        public void put(int value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        IntData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
        }
        
        private int value;

    }

    private static class LongData extends AbstractDBData implements DBLong {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLong#get()
         */
        public long get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLong#put(long)
         */
        public void put(long value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        LongData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
        }
        
        private long value;

    }

    private static class FloatData extends AbstractDBData implements DBFloat {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloat#get()
         */
        public float get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloat#put(float)
         */
        public void put(float value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        FloatData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
        }
        
        private float value;

    }

    private static class DoubleData extends AbstractDBData implements DBDouble {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDouble#get()
         */
        public double get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDouble#put(double)
         */
        public void put(double value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        DoubleData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = 0;
        }
        
        private double value;

    }

    private static class StringData extends AbstractDBData implements DBString {

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVString#get()
         */
        public String get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVString#put(java.lang.String)
         */
        public void put(String value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        StringData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = null;
        }
        
        private String value;

    }

    private static class BooleanArray
        extends AbstractDBArray implements DBBooleanArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBooleanArray#get(int, int, org.epics.ioc.pvAccess.BooleanArrayData)
         */
        public int get(int offset, int len, BooleanArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVBooleanArray#put(int, int, boolean[], int)
         */
        public int put(int offset, int len, boolean[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            boolean[]newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        BooleanArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
        }
        
        private boolean[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ByteArray
        extends AbstractDBArray implements DBByteArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByteArray#get(int, int, org.epics.ioc.pvAccess.ByteArrayData)
         */
        public int get(int offset, int len, ByteArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVByteArray#put(int, int, byte[], int)
         */
        public int put(int offset, int len, byte[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            byte[]newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ByteArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new byte[capacity];
        }
        
        private byte[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ShortArray
        extends AbstractDBArray implements DBShortArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShortArray#get(int, int, org.epics.ioc.pvAccess.ShortArrayData)
         */
        public int get(int offset, int len, ShortArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVShortArray#put(int, int, short[], int)
         */
        public int put(int offset, int len, short[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            short[]newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ShortArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new short[capacity];
        }
        
        private short[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class IntArray
        extends AbstractDBArray implements DBIntArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVIntArray#get(int, int, org.epics.ioc.pvAccess.IntArrayData)
         */
        public int get(int offset, int len, IntArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVIntArray#put(int, int, int[], int)
         */
        public int put(int offset, int len, int[]from,int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            int[]newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        IntArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new int[capacity];
        }
        
        private int[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class LongArray
        extends AbstractDBArray implements DBLongArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLongArray#get(int, int, org.epics.ioc.pvAccess.LongArrayData)
         */
        public int get(int offset, int len, LongArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVLongArray#put(int, int, long[], int)
         */
        public int put(int offset, int len, long[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            long[]newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        LongArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new long[capacity];
        }
        
        private long[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class FloatArray
        extends AbstractDBArray implements DBFloatArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloatArray#get(int, int, org.epics.ioc.pvAccess.FloatArrayData)
         */
        public int get(int offset, int len, FloatArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVFloatArray#put(int, int, float[], int)
         */
        public int put(int offset, int len, float[]from,int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            float[]newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        FloatArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new float[capacity];
        }
        
        private float[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class DoubleArray
        extends AbstractDBArray implements DBDoubleArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDoubleArray#get(int, int, org.epics.ioc.pvAccess.DoubleArrayData)
         */
        public int get(int offset, int len, DoubleArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVDoubleArray#put(int, int, double[], int)
         */
        public int put(int offset, int len, double[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            double[]newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        DoubleArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new double[capacity];
        }
        
        private double[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class StringArray
        extends AbstractDBArray implements DBStringArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStringArray#get(int, int, org.epics.ioc.pvAccess.StringArrayData)
         */
        public int get(int offset, int len, StringArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStringArray#put(int, int, java.lang.String[], int)
         */
        public int put(int offset, int len, String[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            String[]newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        StringArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new String[capacity];
        }
        
        private String[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class EnumArray
        extends AbstractDBArray implements DBEnumArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnumArray#get(int, int, org.epics.ioc.pvAccess.EnumArrayData)
         */
        public int get(int offset, int len, EnumArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVEnumArray#put(int, int, org.epics.ioc.pvAccess.PVEnum[], int)
         */
        public int put(int offset, int len, PVEnum[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBEnum[]newarray = new DBEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        EnumArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBEnum[capacity];
        }
        
        private DBEnum[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class MenuArray
        extends AbstractDBArray implements DBMenuArray
    {
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return getString(0);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    newLine(builder,indentLevel+1);
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBMenuArray#get(int, int, org.epics.ioc.dbAccess.MenuArrayData)
         */
        public int get(int offset, int len, MenuArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBMenuArray#put(int, int, org.epics.ioc.dbAccess.DBMenu[], int)
         */
        public int put(int offset, int len, DBMenu[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBMenu[]newarray = new DBMenu[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        MenuArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBMenu[capacity];
        }
        
        private DBMenu[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class StructureArray
        extends AbstractDBArray implements DBStructureArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructureArray#get(int, int, org.epics.ioc.pvAccess.StructureArrayData)
         */
        public int get(int offset, int len, StructureArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructureArray#put(int, int, org.epics.ioc.pvAccess.PVStructure[], int)
         */
        public int put(int offset, int len, PVStructure[]from,int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return getString(0);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBStructureArray#get(int, int, org.epics.ioc.dbAccess.DBStructureArrayData)
         */
        public int get(int offset, int len, DBStructureArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBStructureArray#put(int, int, org.epics.ioc.dbAccess.DBStructure[], int)
         */
        public int put(int offset, int len, DBStructure[]from,int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBStructure[]newarray = new DBStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        StructureArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBStructure[capacity];
        }
        
        private DBStructure[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class ArrayArray
        extends AbstractDBArray implements DBArrayArray
    {
 
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArrayArray#get(int, int, org.epics.ioc.pvAccess.ArrayArrayData)
         */
        public int get(int offset, int len, ArrayArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArrayArray#put(int, int, org.epics.ioc.pvAccess.PVArray[], int)
         */
        public int put(int offset, int len, PVArray[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return getString(0);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                newLine(builder,indentLevel + 1);
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBArrayArray#get(int, int, org.epics.ioc.dbAccess.DBArrayArrayData)
         */
        public int get(int offset, int len, DBArrayArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBArrayArray#put(int, int, org.epics.ioc.dbAccess.DBArray[], int)
         */
        public int put(int offset, int len, DBArray[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBArray[]newarray = new DBArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBArray[capacity];
        }
        
        private DBArray[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class LinkArray
        extends AbstractDBArray implements DBLinkArray
    {
 
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructureArray#get(int, int, org.epics.ioc.pvAccess.StructureArrayData)
         */
        public int get(int offset, int len, StructureArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVStructureArray#put(int, int, org.epics.ioc.pvAccess.PVStructure[], int)
         */
        public int put(int offset, int len, PVStructure[]from, int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return getString(0);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVData#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
                if(i<length-1) newLine(builder,indentLevel + 1);
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBLinkArray#get(int, int, org.epics.ioc.dbAccess.LinkArrayData)
         */
        public int get(int offset, int len, LinkArrayData data) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.DBLinkArray#put(int, int, org.epics.ioc.dbAccess.DBLink[], int)
         */
        public int put(int offset, int len, DBLink[]from ,int fromOffset) {
            System.out.printf("\n    **.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            postPut();
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBLink[]newarray = new DBLink[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        LinkArray(DBStructure parent,DBDArrayField dbdArrayField,
            int capacity,boolean capacityMutable)
        {
            super(parent,dbdArrayField);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new DBLink[capacity];
        }
        
        private DBLink[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }
    
    private static Convert convert = ConvertFactory.getConvert();
}
