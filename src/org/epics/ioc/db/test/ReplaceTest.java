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
 * JUnit test for replacing the default data implementration for a field.
 * @author mrk
 *
 */
public class ReplaceTest extends TestCase {
        
    /**
     * test replacing the default data implementration for a field.
     */
    public static void testReplaceField() {
        DBD dbd = DBDFactory.getMasterDBD();
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        Requester iocRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/db/test/replaceDBD.xml",iocRequester);
        
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
                 "src/org/epics/ioc/db/test/replaceDB.xml",iocRequester);
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        
        System.out.printf("%ntest replaceField%n");
        testReplace(iocdb,"exampleAi","rawValue");
        testReplace(iocdb,"exampleAi","value");
        System.out.printf("%n");
        new DBListenerForTesting(iocdb,"exampleAi","rawValue");
        new DBListenerForTesting(iocdb,"exampleAi","value");
        testPut(iocdb,"exampleAi","rawValue",2.0);
        testPut(iocdb,"exampleAi","value",5.0);
        System.out.printf("%ntest put and listen examplePowerSupply%n");
        testReplace(iocdb,"examplePowerSupply","power");
        testReplace(iocdb,"examplePowerSupply","current");
        testReplace(iocdb,"examplePowerSupply","voltage");
        new DBListenerForTesting(iocdb,"examplePowerSupply","power");
        new DBListenerForTesting(iocdb,"examplePowerSupply","current");
        new DBListenerForTesting(iocdb,"examplePowerSupply","voltage");
        testPut(iocdb,"examplePowerSupply","current",25.0);
        testPut(iocdb,"examplePowerSupply","voltage",2.0);
        testPut(iocdb,"examplePowerSupply","power",50.0);
        System.out.printf("%ntest put and listen examplePowerSupplyArray%n");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[1].power");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[1].current");
        testReplace(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0].power");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0].current");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1].power");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1].current");
        new DBListenerForTesting(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage");
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].current",25.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].voltage",2.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[0].power",50.0);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].current",2.50);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].voltage",1.00);
        testPut(iocdb,"examplePowerSupplyArray","powerSupply[1].power",2.50);
        System.out.printf("%ntest put and listen allTypes%n");
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
        
        new DBListenerForTesting(iocdb,"allTypes","boolean");
        new DBListenerForTesting(iocdb,"allTypes","byte");
        new DBListenerForTesting(iocdb,"allTypes","short");
        new DBListenerForTesting(iocdb,"allTypes","int");
        new DBListenerForTesting(iocdb,"allTypes","long");
        new DBListenerForTesting(iocdb,"allTypes","float");
        new DBListenerForTesting(iocdb,"allTypes","double");
        new DBListenerForTesting(iocdb,"allTypes","string");
        new DBListenerForTesting(iocdb,"allTypes","booleanArray");
        new DBListenerForTesting(iocdb,"allTypes","byteArray");
        new DBListenerForTesting(iocdb,"allTypes","shortArray");
        new DBListenerForTesting(iocdb,"allTypes","intArray");
        new DBListenerForTesting(iocdb,"allTypes","longArray");
        new DBListenerForTesting(iocdb,"allTypes","floatArray");
        new DBListenerForTesting(iocdb,"allTypes","doubleArray");
        new DBListenerForTesting(iocdb,"allTypes","enumArray");
        new DBListenerForTesting(iocdb,"allTypes","menuArray");
        new DBListenerForTesting(iocdb,"allTypes","linkArray");
        new DBListenerForTesting(iocdb,"allTypes","structArray");
        new DBListenerForTesting(iocdb,"allTypes","arrayArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.boolean");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.byte");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.short");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.int");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.long");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.float");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.double");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.string");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.booleanArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.byteArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.shortArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.intArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.longArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.floatArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.doubleArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.enumArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.menuArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.linkArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.structArray");
        new DBListenerForTesting(iocdb,"allTypes","allTypes.arrayArray");
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
    
    static void testPut(IOCDB iocdb,String recordName,
        String fieldName,double value)
    {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField pvField = pvAccess.findField(fieldName);
        if(pvField==null){
            System.out.printf("field %s not in record %s%n",
                fieldName,recordName);
            return;
        }
        
        DBField dbField = dbRecord.findDBField(pvField);
        Type type = pvField.getField().getType();
        if(type.isNumeric()) {
            System.out.printf("%ntestPut recordName %s fieldName %s value %f",
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
        PVStructure structure = (PVStructure)pvField;
        DBStructure dbStructure = (DBStructure)dbField;
        dbStructure.beginPut();
        PVField[] pvDatas = structure.getFieldPVFields();
        DBField[] dbDatas = dbStructure.getFieldDBFields();
        for(int i=0; i<pvDatas.length; i++) {
            PVField field = pvDatas[i];
            if(field.getField().getType().isNumeric()) {
                System.out.printf("%ntestPut recordName %s fieldName %s value %f",
                        recordName,field.getField().getFieldName(),value);
                convert.fromDouble(field,value);
                dbDatas[i].postPut();
            }
        }
        dbStructure.endPut();
    }
    
    static void testPutArray(IOCDB iocdb,String recordName,
            String fieldName,double value1,double value2,double value3)
    {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField pvField = pvAccess.findField(fieldName);
        if(pvField==null) {
            System.out.printf("field %s not in record %s%n",
                fieldName,recordName);
            return;
        }
        Type type = pvField.getField().getType();
        if(type!=Type.pvArray) {
            System.out.printf("%ntestPutArray recordName %s fieldName %s no an array%n",
                    fieldName,recordName);
                return;
        }
        PVArray dataArray = (PVArray)pvField;
        Type elementType = ((Array)dataArray.getField()).getElementType();
        DBField dbField = dbRecord.findDBField(pvField);
        if(elementType.isNumeric()) {
            System.out.printf("%ntestPut recordName %s fieldName %s values %f %f %f",
                recordName,fieldName,value1,value2,value3);
            double[] values = new double[]{value1,value2,value3};
            convert.fromDoubleArray(pvField,0,3,values,0);
            dbField.postPut();
            return;
        } else {
            System.out.printf("%ntestPut recordName %s fieldName %s cant handle%n",
                    fieldName,recordName);
            return;
        }
    }
    
    static void testPutBoolean(IOCDB iocdb,String recordName,
            String fieldName,boolean value)
    {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField pvField = pvAccess.findField(fieldName);
        if(pvField==null) {
            System.out.printf("field %s not in record %s%n",
                fieldName,recordName);
            return;
        }
        DBField dbField = dbRecord.findDBField(pvField);
        Type type = pvField.getField().getType();
        if(type==Type.pvBoolean) {
            PVBoolean data = (PVBoolean)pvField;
            System.out.printf("%ntestPutBoolean recordName %s fieldName %s value %b",
                recordName,fieldName,value);
            data.put(value);
            dbField.postPut();
            return;
        }
    }
    
    static void testPutString(IOCDB iocdb,String recordName,
            String fieldName,String value)
    {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField pvField = pvAccess.findField(fieldName);
        if(pvField==null) {
            System.out.printf("field %s not in record %s%n",
                fieldName,recordName);
            return;
        }
        DBField dbField = dbRecord.findDBField(pvField);
        Type type = pvField.getField().getType();
        if(type==Type.pvString) {
            PVString data = (PVString)pvField;
            System.out.printf("%ntestPutString recordName %s fieldName %s value %s",
                recordName,fieldName,value);
            data.put(value);
            dbField.postPut();
            return;
        }
    }
    
    private static void testReplace(IOCDB iocdb,String recordName,
        String fieldName)
    {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        PVRecord pvRecord = dbRecord.getPVRecord();
        if(pvRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVAccess pvAccess = PVAccessFactory.createPVAccess(pvRecord);
        PVField oldField = pvAccess.findField(fieldName);
        if(oldField==null) {
            System.out.printf("field %s not in record %s%n",
                fieldName,recordName);
            return;
        }
        PVField parent = oldField.getParent();
        Field field = oldField.getField();
        Type type = field.getType();
        PVField newField = null;
        switch(type) {
        case pvBoolean:
             newField = new BooleanData(parent,field);
             break;
        case pvByte:
             newField = new ByteData(parent,field);
             break;
        case pvShort:
             newField = new ShortData(parent,field);
             break;
        case pvInt:
             newField = new IntData(parent,field);
             break;
        case pvLong:
             newField = new LongData(parent,field);
             break;
        case pvFloat:
             newField = new FloatData(parent,field);
             break;
        case pvDouble:
             newField = new DoubleData(parent,field);
             break;
        case pvString:
             newField = new StringData(parent,field);
             break;
        case pvEnum:
             System.out.printf("pvEnum not supported.%n");
             return;
        case pvMenu:
             System.out.printf("dbMenu not supported.%n");
             return;
        case pvStructure:
             System.out.printf("dbStructure not supported.%n");
             return;
        case pvArray:
             Array array= (Array)field;
             Type elementType = array.getElementType();
             switch(elementType) {
             case pvBoolean:
                  newField = new BooleanArray(parent,
                    array, 0, true);
                  break;
             case pvByte:
                  newField = new ByteArray(parent,
                    array, 0, true);
                  break;
             case pvShort:
                  newField = new ShortArray(parent,
                    array, 0, true);
                  break;
             case pvInt:
                  newField = new IntArray(parent,
                    array, 0, true);
                  break;
             case pvLong:
                  newField = new LongArray(parent,
                    array, 0, true);
                  break;
             case pvFloat:
                  newField = new FloatArray(parent,
                    array, 0, true);
                  break;
             case pvDouble:
                  newField = new DoubleArray(parent,
                    array, 0, true);
                  break;
             case pvString:
                  newField = new StringArray(parent,
                    array, 0, true);
                  break;
             case pvEnum:
                  newField = new EnumArray(parent,
                    array, 0, true);
                  break;
             case pvMenu:
                 newField = new MenuArray(parent,
                     array, 0, true);
                 break;
             case pvStructure:
                 newField = new StructureArray(parent,
                     array, 0, true);
                 break;
             case pvArray:
                 newField = new ArrayArray(parent,
                     array, 0, true);
                 break;
             case pvLink:
                 newField = new LinkArray(parent,
                     array, 0, true);
                 break;
             }
             break;
        case pvLink:
             System.out.printf("dbLink not supported.%n");
             return;
        }
        DBField dbField = dbRecord.findDBField(oldField);
        dbField.replacePVField(newField);
    }
    
    
    private static class BooleanData extends AbstractPVField
        implements PVBoolean
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        BooleanData(PVField parent,Field field) {
            super(parent,field);
            value = false;
        }
        
        private boolean value;

    }

    private static class ByteData extends AbstractPVField implements PVByte {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByte#get()
         */
        public byte get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByte#put(byte)
         */
        public void put(byte value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ByteData(PVField parent,Field field) {
            super(parent,field);
            value = 0;
        }
        
        private byte value;

    }

    private static class ShortData extends AbstractPVField implements PVShort {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShort#get()
         */
        public short get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShort#put(short)
         */
        public void put(short value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        ShortData(PVField parent,Field field) {
            super(parent,field);
            value = 0;
        }
        
        private short value;

    }

    private static class IntData extends AbstractPVField implements PVInt {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#get()
         */
        public int get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVInt#put(int)
         */
        public void put(int value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        IntData(PVField parent,Field field) {
            super(parent,field);
            value = 0;
        }
        
        private int value;

    }

    private static class LongData extends AbstractPVField implements PVLong {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLong#get()
         */
        public long get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLong#put(long)
         */
        public void put(long value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        LongData(PVField parent,Field field) {
            super(parent,field);
            value = 0;
        }
        
        private long value;

    }

    private static class FloatData extends AbstractPVField implements PVFloat {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloat#get()
         */
        public float get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloat#put(float)
         */
        public void put(float value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        FloatData(PVField parent,Field field) {
            super(parent,field);
            value = 0;
        }
        
        private float value;

    }

    private static class DoubleData extends AbstractPVField implements PVDouble {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#get()
         */
        public double get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#put(double)
         */
        public void put(double value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        DoubleData(PVField parent,Field field) {
            super(parent,field);
            value = 0;
        }
        
        private double value;

    }

    private static class StringData extends AbstractPVField implements PVString {

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#get()
         */
        public String get() {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            return value;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#put(java.lang.String)
         */
        public void put(String value) {
            if(getField().isMutable()) {
                System.out.printf("%n    **%s.put**",getField().getType().toString());
                this.value = value;
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return convert.getString(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        StringData(PVField parent,Field field) {
            super(parent,field);
            value = null;
        }
        
        private String value;

    }
    
    private static abstract class AbstractDBArray extends AbstractPVArray implements PVArray{
        protected int length = 0;
        protected int capacity;
        protected boolean capacityMutable = true;
        /**
         * Constructer that derived classes must call.
         * @param parent The parent interface.
         * @param dbdArrayField The reflection interface for the DBArray data.
         */
        protected AbstractDBArray(PVField parent,Array array) {
            super(parent,array,0,true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#isCapacityMutable()
         */
        public boolean isCapacityMutable() {
            return capacityMutable;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#getCapacity()
         */
        public int getCapacity() {
            return capacity;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#getLength()
         */
        public int getLength() {
            return length;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        abstract public void setCapacity(int capacity);
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setLength(int)
         */
        public void setLength(int len) {
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(len>capacity) setCapacity(len);
            length = len;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }
    }
    private static class BooleanArray extends AbstractDBArray implements PVBooleanArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBooleanArray#get(int, int, org.epics.ioc.pv.BooleanArrayData)
         */
        public int get(int offset, int len, BooleanArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBooleanArray#put(int, int, boolean[], int)
         */
        public int put(int offset, int len, boolean[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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
        
        private BooleanArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new boolean[capacity];
        }
        
        private boolean[] value;
    }

    private static class ByteArray extends AbstractDBArray implements PVByteArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByteArray#get(int, int, org.epics.ioc.pv.ByteArrayData)
         */
        public int get(int offset, int len, ByteArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVByteArray#put(int, int, byte[], int)
         */
        public int put(int offset, int len, byte[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private ByteArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new byte[capacity];
        }
        
        private byte[] value;
    }

    private static class ShortArray extends AbstractDBArray implements PVShortArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShortArray#get(int, int, org.epics.ioc.pv.ShortArrayData)
         */
        public int get(int offset, int len, ShortArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVShortArray#put(int, int, short[], int)
         */
        public int put(int offset, int len, short[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private ShortArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new short[capacity];
        }
        
        private short[] value;
    }

    private static class IntArray extends AbstractDBArray implements PVIntArray
    {
       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVIntArray#get(int, int, org.epics.ioc.pv.IntArrayData)
         */
        public int get(int offset, int len, IntArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVIntArray#put(int, int, int[], int)
         */
        public int put(int offset, int len, int[]from,int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private IntArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new int[capacity];
        }
        
        private int[] value;
    }

    private static class LongArray extends AbstractDBArray implements PVLongArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLongArray#get(int, int, org.epics.ioc.pv.LongArrayData)
         */
        public int get(int offset, int len, LongArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLongArray#put(int, int, long[], int)
         */
        public int put(int offset, int len, long[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private LongArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new long[capacity];
        }
        
        private long[] value;
    }

    private static class FloatArray extends AbstractDBArray implements PVFloatArray
    {
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloatArray#get(int, int, org.epics.ioc.pv.FloatArrayData)
         */
        public int get(int offset, int len, FloatArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVFloatArray#put(int, int, float[], int)
         */
        public int put(int offset, int len, float[]from,int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private FloatArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new float[capacity];
        }
        
        private float[] value;
    }

    private static class DoubleArray extends AbstractDBArray implements PVDoubleArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDoubleArray#get(int, int, org.epics.ioc.pv.DoubleArrayData)
         */
        public int get(int offset, int len, DoubleArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDoubleArray#put(int, int, double[], int)
         */
        public int put(int offset, int len, double[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private DoubleArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new double[capacity];
        }
        
        private double[] value;
    }

    private static class StringArray extends AbstractDBArray implements PVStringArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#get(int, int, org.epics.ioc.pv.StringArrayData)
         */
        public int get(int offset, int len, StringArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStringArray#put(int, int, java.lang.String[], int)
         */
        public int put(int offset, int len, String[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
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

        private StringArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new String[capacity];
        }
        
        private String[] value;
    }

    private static class EnumArray extends AbstractDBArray implements PVEnumArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumArray#get(int, int, org.epics.ioc.pv.EnumArrayData)
         */
        public int get(int offset, int len, EnumArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVEnumArray#put(int, int, org.epics.ioc.pv.PVEnum[], int)
         */
        public int put(int offset, int len, PVEnum[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVEnum[]newarray = new PVEnum[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        private EnumArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVEnum[capacity];
        }
        
        private PVEnum[] value;
        int length = 0;
        int capacity;
        boolean capacityMutable;
    }

    private static class MenuArray extends AbstractDBArray implements PVMenuArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            convert.newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    convert.newLine(builder,indentLevel+1);
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            convert.newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVMenuArray#get(int, int, org.epics.ioc.pv.MenuArrayData)
         */
        public int get(int offset, int len, MenuArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVMenuArray#put(int, int, org.epics.ioc.pv.DBMenu[], int)
         */
        public int put(int offset, int len, PVMenu[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVMenu[]newarray = new PVMenu[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        private MenuArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVMenu[capacity];
        }
        
        private PVMenu[] value;
    }

    private static class StructureArray extends AbstractDBArray implements PVStructureArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructureArray#get(int, int, org.epics.ioc.pv.StructureArrayData)
         */
        public int get(int offset, int len, StructureArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVStructureArray#put(int, int, org.epics.ioc.pv.PVStructure[], int)
         */
        public int put(int offset, int len, PVStructure[]from,int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            convert.newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            convert.newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVStructure[]newarray = new PVStructure[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        private StructureArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVStructure[capacity];
        }
        
        private PVStructure[] value;
    }

    private static class ArrayArray extends AbstractDBArray implements PVArrayArray
    {
 
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArrayArray#get(int, int, org.epics.ioc.pv.ArrayArrayData)
         */
        public int get(int offset, int len, ArrayArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArrayArray#put(int, int, org.epics.ioc.pv.PVArray[], int)
         */
        public int put(int offset, int len, PVArray[]from, int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            convert.newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                convert.newLine(builder,indentLevel + 1);
                if(value[i]==null) {
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
            }
            convert.newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVArray[]newarray = new PVArray[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        private ArrayArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVArray[capacity];
        }
        
        private PVArray[] value;
    }

    private static class LinkArray extends AbstractDBArray implements PVLinkArray
    {
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVField#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            convert.newLine(builder,indentLevel);
            builder.append("{");
            for(int i=0; i < length; i++) {
                if(value[i]==null) {
                    
                    builder.append("{}");
                } else {
                    builder.append(value[i].toString(indentLevel+1));
                }
                if(i<length-1) convert.newLine(builder,indentLevel + 1);
            }
            convert.newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString() + super.toString(indentLevel);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLinkArray#get(int, int, org.epics.ioc.pv.LinkArrayData)
         */
        public int get(int offset, int len, LinkArrayData data) {
            System.out.printf("%n    **%s.get**",getField().getType().toString());
            int n = len;
            if(offset+len > length) n = length;
            data.data = value;
            data.offset = offset;
            return n;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVLinkArray#put(int, int, org.epics.ioc.pv.PVLink[], int)
         */
        public int put(int offset, int len, PVLink[]from ,int fromOffset) {
            System.out.printf("%n    **%s.put**",getField().getType().toString());
            if(!super.getField().isMutable())
                throw new IllegalStateException("PVField.isMutable is false");
            if(offset+len > length) {
                 int newlength = offset + len;
                 if(newlength>capacity) setCapacity(newlength);
                 length = newlength;
            }
            System.arraycopy(from,fromOffset,value,offset,len);
            return len;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVArray#setCapacity(int)
         */
        public void setCapacity(int len) {
            if(!capacityMutable)
                throw new IllegalStateException("capacity is immutable");
            if(length>len) length = len;
            PVLink[]newarray = new PVLink[len];
            if(length>0) System.arraycopy(value,0,newarray,0,length);
            value = newarray;
            capacity = len;
        }

        private LinkArray(PVField parent,Array array,
            int capacity,boolean capacityMutable)
        {
            super(parent,array);
            this.capacity = capacity;
            this.capacityMutable = capacityMutable;
            value = new PVLink[capacity];
        }
        
        private PVLink[] value;
    }
    
    private static Convert convert = ConvertFactory.getConvert();
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "ReplaceTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
