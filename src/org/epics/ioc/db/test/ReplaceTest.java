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
                "example/exampleDBD.xml",iocRequester);
        
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
                "example/exampleDB.xml",iocRequester);
//        System.out.printf("%nrecords%n");
//        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
//        Set<String> keys = recordMap.keySet();
//        for(String key: keys) {
//            DBRecord record = recordMap.get(key);
//            System.out.print(record.toString());
//        }
        
        System.out.printf("%ntest replaceField%n");
        testReplace(iocdb,"ai","input.value");
        testReplace(iocdb,"ai","value");
        System.out.printf("%n");
        new DBListenerForTesting(iocdb,"ai","input.value");
        new DBListenerForTesting(iocdb,"ai","value");
        testPut(iocdb,"ai","input.value",2.0);
        testPut(iocdb,"ai","value",5.0);
        System.out.printf("%ntest put and listen psSimple%n");
        testReplace(iocdb,"psSimple","power.value");
        testReplace(iocdb,"psSimple","current.value");
        testReplace(iocdb,"psSimple","voltage.value");
        new DBListenerForTesting(iocdb,"psSimple","power.value");
        new DBListenerForTesting(iocdb,"psSimple","current.value");
        new DBListenerForTesting(iocdb,"psSimple","voltage.value");
        testPut(iocdb,"psSimple","current.value",25.0);
        testPut(iocdb,"psSimple","voltage.value",2.0);
        testPut(iocdb,"psSimple","power.value",50.0);
        System.out.printf("%ntest put and listen powerSupplyArray%n");
        testReplace(iocdb,"powerSupplyArray","supply[0].power.value");
        testReplace(iocdb,"powerSupplyArray","supply[0].current.value");
        testReplace(iocdb,"powerSupplyArray","supply[0].voltage.value");
        testReplace(iocdb,"powerSupplyArray","supply[1].power.value");
        testReplace(iocdb,"powerSupplyArray","supply[1].current.value");
        testReplace(iocdb,"powerSupplyArray","supply[1].voltage.value");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[0].power.value");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[0].current.value");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[0].voltage.value");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[1].power.value");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[1].current.value");
        new DBListenerForTesting(iocdb,"powerSupplyArray","supply[1].voltage.value");
        testPut(iocdb,"powerSupplyArray","supply[0].current.value",25.0);
        testPut(iocdb,"powerSupplyArray","supply[0].voltage.value",2.0);
        testPut(iocdb,"powerSupplyArray","supply[0].power.value",50.0);
        testPut(iocdb,"powerSupplyArray","supply[1].current.value",2.50);
        testPut(iocdb,"powerSupplyArray","supply[1].voltage.value",1.00);
        testPut(iocdb,"powerSupplyArray","supply[1].power.value",2.50);
        System.out.printf("%ntest put and listen allTypes%n");
        testReplace(iocdb,"allTypesInitial","boolean");
        testReplace(iocdb,"allTypesInitial","byte");
        testReplace(iocdb,"allTypesInitial","short");
        testReplace(iocdb,"allTypesInitial","int");
        testReplace(iocdb,"allTypesInitial","long");
        testReplace(iocdb,"allTypesInitial","float");
        testReplace(iocdb,"allTypesInitial","double");
        testReplace(iocdb,"allTypesInitial","string");
        testReplace(iocdb,"allTypesInitial","booleanArray");
        testReplace(iocdb,"allTypesInitial","byteArray");
        testReplace(iocdb,"allTypesInitial","shortArray");
        testReplace(iocdb,"allTypesInitial","intArray");
        testReplace(iocdb,"allTypesInitial","longArray");
        testReplace(iocdb,"allTypesInitial","floatArray");
        testReplace(iocdb,"allTypesInitial","doubleArray");
        testReplace(iocdb,"allTypesInitial","structArray");
        testReplace(iocdb,"allTypesInitial","arrayArray");
        testReplace(iocdb,"allTypesInitial","allTypes.boolean");
        testReplace(iocdb,"allTypesInitial","allTypes.byte");
        testReplace(iocdb,"allTypesInitial","allTypes.short");
        testReplace(iocdb,"allTypesInitial","allTypes.int");
        testReplace(iocdb,"allTypesInitial","allTypes.long");
        testReplace(iocdb,"allTypesInitial","allTypes.float");
        testReplace(iocdb,"allTypesInitial","allTypes.double");
        testReplace(iocdb,"allTypesInitial","allTypes.string");
        testReplace(iocdb,"allTypesInitial","allTypes.booleanArray");
        testReplace(iocdb,"allTypesInitial","allTypes.byteArray");
        testReplace(iocdb,"allTypesInitial","allTypes.shortArray");
        testReplace(iocdb,"allTypesInitial","allTypes.intArray");
        testReplace(iocdb,"allTypesInitial","allTypes.longArray");
        testReplace(iocdb,"allTypesInitial","allTypes.floatArray");
        testReplace(iocdb,"allTypesInitial","allTypes.doubleArray");
        testReplace(iocdb,"allTypesInitial","allTypes.structArray");
        testReplace(iocdb,"allTypesInitial","allTypes.arrayArray");
        
        new DBListenerForTesting(iocdb,"allTypesInitial","boolean");
        new DBListenerForTesting(iocdb,"allTypesInitial","byte");
        new DBListenerForTesting(iocdb,"allTypesInitial","short");
        new DBListenerForTesting(iocdb,"allTypesInitial","int");
        new DBListenerForTesting(iocdb,"allTypesInitial","long");
        new DBListenerForTesting(iocdb,"allTypesInitial","float");
        new DBListenerForTesting(iocdb,"allTypesInitial","double");
        new DBListenerForTesting(iocdb,"allTypesInitial","string");
        new DBListenerForTesting(iocdb,"allTypesInitial","booleanArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","byteArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","shortArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","intArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","longArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","floatArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","doubleArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","structArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","arrayArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.boolean");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.byte");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.short");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.int");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.long");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.float");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.double");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.string");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.booleanArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.byteArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.shortArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.intArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.longArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.floatArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.doubleArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.structArray");
        new DBListenerForTesting(iocdb,"allTypesInitial","allTypes.arrayArray");
        testPutBoolean(iocdb,"allTypesInitial","boolean",true);
        testPut(iocdb,"allTypesInitial","byte",1.0);
        testPut(iocdb,"allTypesInitial","short",2.0);
        testPut(iocdb,"allTypesInitial","int",3.0);
        testPut(iocdb,"allTypesInitial","long",4.0);
        testPut(iocdb,"allTypesInitial","float",5.0);
        testPut(iocdb,"allTypesInitial","double",6.0);
        testPutString(iocdb,"allTypesInitial","string","test string");
        testPutArray(iocdb,"allTypesInitial","byteArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","shortArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","intArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","longArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","floatArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","doubleArray",1.0,2.0,3.0);
        testPutBoolean(iocdb,"allTypesInitial","allTypes.boolean",true);
        testPut(iocdb,"allTypesInitial","allTypes.byte",1.0);
        testPut(iocdb,"allTypesInitial","allTypes.short",2.0);
        testPut(iocdb,"allTypesInitial","allTypes.int",3.0);
        testPut(iocdb,"allTypesInitial","allTypes.long",4.0);
        testPut(iocdb,"allTypesInitial","allTypes.float",5.0);
        testPut(iocdb,"allTypesInitial","allTypes.double",6.0);
        testPutString(iocdb,"allTypesInitial","allTypes.string","test string");
        testPutArray(iocdb,"allTypesInitial","allTypes.booleanArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","allTypes.byteArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","allTypes.shortArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","allTypes.intArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","allTypes.longArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","allTypes.floatArray",1.0,2.0,3.0);
        testPutArray(iocdb,"allTypesInitial","allTypes.doubleArray",1.0,2.0,3.0);
    }
    
    static void testPut(IOCDB iocdb,String recordName,
        String fieldName,double value)
    {
        DBRecord dbRecord = iocdb.findRecord(recordName);
        if(dbRecord==null) {
            System.out.printf("record %s not found%n",recordName);
            return;
        }
        PVRecord pvRecord = dbRecord.getPVRecord();
        PVField pvField = pvRecord.findProperty(fieldName);
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
        PVField[] pvDatas = structure.getPVFields();
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
        PVField pvField = pvRecord.findProperty(fieldName);
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
        PVField pvField = pvRecord.findProperty(fieldName);
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
        PVField pvField = pvRecord.findProperty(fieldName);
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
        PVField oldField = pvRecord.findProperty(fieldName);
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
             case pvStructure:
                 newField = new StructureArray(parent,
                     array, 0, true);
                 break;
             case pvArray:
                 newField = new ArrayArray(parent,
                     array, 0, true);
                 break;
             }
             break;
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
            if(super.isMutable()) {
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
         * @param dbdArrayField The reflection interface for the CDArray data.
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
            if(!super.isMutable())
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
