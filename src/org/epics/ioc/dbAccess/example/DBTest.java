/**
 * 
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

import java.util.*;
public class DBTest extends TestCase {
        
    public static void testDB() {
        DBDField[] fields;

        DBD dbd = DBDFactory.create("test");
        // create scan menu
        String[] choices = {"passive","event","interrupt","periodic"};
        DBDMenu menu = DBDCreateFactory.createDBDMenu("scan",choices);
        assertTrue(dbd.addMenu(menu));
        // create a structure displayLimit
        DBDField low = DBDCreateFactory.createDBDField(
            "low",Type.pvDouble,DBType.dbPvType,null);
        DBDField high = DBDCreateFactory.createDBDField(
            "high",Type.pvDouble,DBType.dbPvType,null);
        fields = new DBDField[] {low,high};
        DBDStructure displayLimit = DBDCreateFactory.createDBDStructure(
                "displayLimit",fields,null);
        assertTrue(dbd.addStructure(displayLimit));
        // create structure processLink
        DBDField pvname = DBDCreateFactory.createDBDField(
            "pvname",Type.pvString,DBType.dbPvType,null);
        DBDAttribute attribute = pvname.getDBDAttribute();
        attribute.setLink(true);
        DBDField wait = DBDCreateFactory.createDBDField(
            "wait",Type.pvBoolean,DBType.dbPvType,null);
        DBDField timeout = DBDCreateFactory.createDBDField(
            "timeout",Type.pvDouble,DBType.dbPvType,null);
        fields = new DBDField[] {pvname,wait,timeout};
        DBDStructure processLink = DBDCreateFactory.createDBDStructure(
                "processLink",fields,null);
        assertTrue(dbd.addStructure(processLink));
        // create structure inputLink
        DBDField process = DBDCreateFactory.createDBDField(
            "process",Type.pvBoolean,DBType.dbPvType,null);
        DBDField inheritSeverity = DBDCreateFactory.createDBDField(
            "inheritSeverity",Type.pvBoolean,DBType.dbPvType,null);
        fields = new DBDField[] {
            pvname,process,wait,timeout,inheritSeverity};
        DBDStructure inputLink  = DBDCreateFactory.createDBDStructure(
            "inputLink",fields,null);
        assertTrue(dbd.addStructure(inputLink));
        // create a property
        Property displayLimitProperty = FieldFactory.createProperty(
            "displayLimit","displayLimit");
        Property[]property = new Property[] {displayLimitProperty};
        // create a recordType
        DBDMenuField scan = DBDCreateFactory.createDBDMenuField(
            "scan",menu,null);
        assertNotNull(scan);
        DBDStructureField display = DBDCreateFactory.createDBDStructureField(
            "displayLimit",displayLimit,null);
        assertNotNull(display);
        DBDField value = DBDCreateFactory.createDBDField(
            "value",Type.pvDouble,DBType.dbPvType,property);
        assertNotNull(value);
        DBDField rawValue = DBDCreateFactory.createDBDField(
            "rawValue",Type.pvInt,DBType.dbPvType,null);
        assertNotNull(rawValue);
        DBDArrayField doubleArray = DBDCreateFactory.createDBDArrayField(
            "doubleArray",Type.pvDouble,DBType.dbPvType,null);
        DBDLinkField input = DBDCreateFactory.createDBDLinkField("input",
            null);
        assertNotNull(input);
        DBDArrayField processField = DBDCreateFactory.createDBDArrayField(
            "process",Type.pvStructure,DBType.dbLink,null);
        assertNotNull(processField);
        fields = new DBDField[] {
            scan,
            display,
            value,
            rawValue,
            doubleArray,
            input,
            processField
        };
        DBDStructure recordType = DBDCreateFactory.createDBDStructure(
            "ai",fields,null);
        assertNotNull(recordType);
        assertTrue(dbd.addRecordType(recordType));
        Collection<DBD> dbdList = DBDFactory.getDBDList();
        
        Iterator<DBD> iter = dbdList.iterator();
        boolean reflect = false;
        while(reflect && iter.hasNext()) {
            dbd = iter.next();
            System.out.printf("DBD %s\n",
                    dbd.getName());
            System.out.printf("\nrecordTypes\n");
            Collection<DBDStructure> recordTypeList = dbd.getDBDRecordTypeList();
            Iterator<DBDStructure> recordTypeIter = recordTypeList.iterator();
            while(recordTypeIter.hasNext()) {
                System.out.print(recordTypeIter.next().toString());
            }
        }
        // create a record instance
        DBDStructureField dbdStructureField = 
            DBDCreateFactory.createDBDStructureField("aiExample",recordType,null);
        DBStructure dbStructure = FieldDataFactory.createStructureData(
            dbdStructureField);
        DBRecord dbRecord = new RecordData(dbStructure,dbStructure.getDBDField());
        System.out.print(dbRecord.toString());
        DBMenu scanData = null;
        DBStructure displayData = null;
        DBDouble valueData = null;
        DBInt rawValueData = null; 
        DBDoubleArray doubleArrayData = null;
        DBLink inputData = null;
        DBLinkArray processData = null;
        DBData[] dbData = dbRecord.getFieldDBDatas();
        for(int i=0; i< dbData.length; i++) {
            Field field = dbData[i].getField();
            String fieldName = field.getName();
            if(fieldName.equals("scan")) scanData = (DBMenu)dbData[i];
            if(fieldName.equals("displayLimit")) displayData = (DBStructure)dbData[i];
            if(fieldName.equals("value")) valueData = (DBDouble)dbData[i];
            if(fieldName.equals("rawValue")) rawValueData = (DBInt)dbData[i];
            if(fieldName.equals("doubleArray")) doubleArrayData = (DBDoubleArray)dbData[i];
            if(fieldName.equals("input")) inputData = (DBLink)dbData[i];
            if(fieldName.equals("process")) processData = (DBLinkArray)dbData[i];
        }
        assertNotNull(scanData);
        assertNotNull(displayData);
        assertNotNull(valueData);
        assertNotNull(rawValueData);
        assertNotNull(doubleArrayData);
        assertNotNull(inputData);
        assertNotNull(processData);
        scanData.setIndex(2);
        valueData.put(10.0);
        rawValueData.put(10);
        doubleArrayData.setCapacity(2);
        double[] dvalue = new double[] { 1.0, 10.0};
        doubleArrayData.put(0,2,dvalue,0);
        inputData.putConfigStructureFieldName("inputConfig");
        inputData.putLinkSupportName("linkSupport");
        processData.setCapacity(2);
        DBDLinkField process0 = DBDCreateFactory.createDBDLinkField("process0",
                null);
        DBLink dbLink0 = FieldDataFactory.createLinkData(process0);
        dbLink0.putConfigStructureFieldName("process0Config");
        dbLink0.putLinkSupportName("process0LinkSuppport");
        DBDLinkField process1 = DBDCreateFactory.createDBDLinkField("process1",
                null);
        DBLink dbLink1 = FieldDataFactory.createLinkData(process1);
        dbLink1.putConfigStructureFieldName("process1Config");
        dbLink1.putLinkSupportName("process1LinkSuppport");
        DBLink[] dbLinks = new DBLink[] {dbLink0,dbLink1};
        processData.put(0,2,dbLinks,0);
        System.out.print(dbRecord.toString());
    }


    private static class RecordData extends AbstractDBData
        implements DBRecord
    {

        public String toString() {
            return dbStructure.toString();
        }
        
        public String toString(int indentLevel) {
            return dbStructure.toString(indentLevel);
        }

        public PVData[] getFieldPVDatas() {
            return dbStructure.getFieldPVDatas();
        }

        public String getRecordName() {
            Field field = dbStructure.getField();
            return field.getName();
        }

        public DBData[] getFieldDBDatas() {
            return dbStructure.getFieldDBDatas();
        }
    
        RecordData(DBStructure dbStructure,DBDField dbdField) {
            super(dbdField);
            this.dbStructure = dbStructure;
        }
    
        private DBStructure dbStructure;
    }
}
