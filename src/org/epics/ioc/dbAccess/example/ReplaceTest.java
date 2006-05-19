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
        testPutBoolean(iocdb,"allTypes","boolean",true);
        testPut(iocdb,"allTypes","byte",1.0);
        testPut(iocdb,"allTypes","short",2.0);
        testPut(iocdb,"allTypes","int",3.0);
        testPut(iocdb,"allTypes","long",4.0);
        testPut(iocdb,"allTypes","float",5.0);
        testPut(iocdb,"allTypes","double",6.0);
        testPutString(iocdb,"allTypes","string","test string");
        testPutArray(iocdb,"allTypes","byteArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","shortArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","intArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","longArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","floatArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","doubleArray",1.0,2.0,3.0);
        testPutBoolean(iocdb,"allTypes","allTypes.boolean",true);
        testPut(iocdb,"allTypes","allTypes.byte",1.0);
        testPut(iocdb,"allTypes","allTypes.short",2.0);
        testPut(iocdb,"allTypes","allTypes.int",3.0);
        testPut(iocdb,"allTypes","allTypes.long",4.0);
        testPut(iocdb,"allTypes","allTypes.float",5.0);
        testPut(iocdb,"allTypes","allTypes.double",6.0);
        testPutString(iocdb,"allTypes","allTypes.string","test string");
        testPutArray(iocdb,"allTypes","allTypes.booleanArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","allTypes.byteArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","allTypes.shortArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","allTypes.intArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","allTypes.longArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","allTypes.floatArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypes","allTypes.doubleArray",1.0,2.0,3.0);
    }
    
    private static class TestListener implements DBListener{
        public void newData(DBData dbData) {
            System.out.printf("\n    TestListener recordName %s",recordName);
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
                    System.out.printf("field %s of record %s not found\n",
                        fieldName,recordName);
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

    static void testPut(IOCDB iocdb,String recordName,
        String fieldName,double value)
    {
        DBAccess dbAccess = iocdb.createAccess(recordName);
        if(dbAccess==null) {
            System.out.printf("record %s not found\n",recordName);
            return;
        }
        if(!dbAccess.setField(fieldName)){
            System.out.printf("field %s of record %s not found\n",
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
        if(!dbAccess.setField(fieldName)){
            System.out.printf("field %s of record %s not found\n",
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
System.out.printf("elementType %s\n",elementType.toString());
Class c = dbData.getClass();
System.out.printf("%s\n",c.toString());
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
        if(!dbAccess.setField(fieldName)){
            System.out.printf("field %s of record %s not found\n",
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
        if(!dbAccess.setField(fieldName)){
            System.out.printf("field %s of record %s not found\n",
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
        if(!dbAccess.setField(fieldName)){
            System.out.printf("field %s of record %s not found\n",
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
                          newField = new ArrayBooleanData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvByte:
                          newField = new ArrayByteData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvShort:
                          newField = new ArrayShortData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvInt:
                          newField = new ArrayIntData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvLong:
                          newField = new ArrayLongData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvFloat:
                          newField = new ArrayFloatData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvDouble:
                          newField = new ArrayDoubleData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvString:
                          newField = new ArrayStringData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     case pvEnum:
                          newField = new ArrayEnumData(parent,
                            (DBDArrayField)dbdField, 0, true);
                          break;
                     }
                 }
                 break;
             case dbMenu:
                 newField = new ArrayMenuData(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             case dbStructure:
                 newField = new ArrayStructureData(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             case dbArray:
                 newField = new ArrayArrayData(parent,
                     (DBDArrayField)dbdField, 0, true);
                 break;
             case dbLink:
                 newField = new ArrayLinkData(parent,
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
        public boolean get() {
            System.out.printf("\n    **%s.get**",getField().getType().toString());
            return value;
        }

        public void put(boolean value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
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

        public byte get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(byte value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
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

        public short get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(short value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }

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

        public int get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(int value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
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

        public long get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(long value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
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

        public float get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(float value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
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

        public double get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(double value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
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

        public String get() {
            System.out.printf("\n    **.get**",getField().getType().toString());
            return value;
        }

        public void put(String value) {
            if(getField().isMutable()) {
                System.out.printf("\n    **.put**",getField().getType().toString());
                this.value = value;
                postPut();
                return ;
            }
            throw new IllegalStateException("PVData.isMutable is false");
        }
        
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        StringData(DBStructure parent,DBDField dbdField) {
            super(parent,dbdField);
            value = null;
        }
        
        private String value;

    }

    private static class ArrayBooleanData
        extends AbstractDBArray implements DBBooleanArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, boolean[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, boolean[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            boolean[]newarray = new boolean[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayBooleanData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayByteData
        extends AbstractDBArray implements DBByteArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, byte[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, byte[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            byte[]newarray = new byte[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayByteData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayShortData
        extends AbstractDBArray implements DBShortArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, short[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, short[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            short[]newarray = new short[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayShortData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayIntData
        extends AbstractDBArray implements DBIntArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, int[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, int[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            int[]newarray = new int[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayIntData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayLongData
        extends AbstractDBArray implements DBLongArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, long[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, long[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            long[]newarray = new long[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayLongData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayFloatData
        extends AbstractDBArray implements DBFloatArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, float[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, float[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            float[]newarray = new float[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayFloatData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayDoubleData
        extends AbstractDBArray implements DBDoubleArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, double[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, double[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            double[]newarray = new double[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayDoubleData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayStringData
        extends AbstractDBArray implements DBStringArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, String[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, String[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            String[]newarray = new String[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayStringData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayEnumData
        extends AbstractDBArray implements DBEnumArray
    {
        public String toString() {
            return convert.getString(this);
        }
        
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, PVEnum[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVEnum[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBEnum[]newarray = new DBEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayEnumData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayMenuData
        extends AbstractDBArray implements DBMenuArray
    {
        public String toString() {
            return getString(0);
        }
        
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBMenu[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBMenu[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBMenu[]newarray = new DBMenu[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayMenuData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayStructureData
        extends AbstractDBArray implements DBStructureArray
    {
        public int get(int offset, int len, PVStructure[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVStructure[] from, int fromOffset) {
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

        public String toString() {
            return getString(0);
        }
        
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBStructure[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBStructure[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBStructure[]newarray = new DBStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayStructureData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayArrayData
        extends AbstractDBArray implements DBArrayArray
    {
 
        public int get(int offset, int len, PVArray[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVArray[] from, int fromOffset) {
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

        public String toString() {
            return getString(0);
        }
        
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBArray[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBArray[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBArray[]newarray = new DBArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayArrayData(DBStructure parent,DBDArrayField dbdArrayField,
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

    private static class ArrayLinkData
        extends AbstractDBArray implements DBLinkArray
    {
 
        public int get(int offset, int len, PVStructure[] to, int toOffset) {
            System.out.printf("\n    **.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, PVStructure[] from, int fromOffset) {
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

        public String toString() {
            return getString(0);
        }
        
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

        public boolean isCapacityMutable() {
            return capacityMutable;
        }

        public int get(int offset, int len, DBLink[] to, int toOffset) {
            int n = len;
            if(offset+len > length) n = length;
            if(n>0) System.arraycopy(value,offset,to,toOffset,n);
            return n;
        }

        public int put(int offset, int len, DBLink[] from, int fromOffset) {
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
        
        public int getCapacity() {
            return capacity;
        }

        public int getLength() {
            return length;
        }

        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            DBLink[]newarray = new DBLink[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVData.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }

        ArrayLinkData(DBStructure parent,DBDArrayField dbdArrayField,
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
