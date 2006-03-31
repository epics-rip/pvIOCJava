/**
 * 
 */
package org.epics.ioc.dbDefinition.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import java.util.*;
public class DBDTest extends TestCase {
        
    public static void testDBD() {
        DBDField[] fields;

        DBD dbd = DBDFactory.create("test");
        assertNotNull(dbd);
        // create menu scan
        String[] choices = {"passive","event","interrupt","periodic"};
        DBDMenu menuScan = DBDCreateFactory.createDBDMenu("scan",choices);
        assertNotNull(menuScan);
        assertTrue(dbd.addMenu(menuScan));
        // create structure displayLimit
        DBDField low = DBDCreateFactory.createDBDField(
            "low",Type.pvDouble,DBType.dbPvType,null);
        assertNotNull(low);
        DBDField high = DBDCreateFactory.createDBDField(
                "high",Type.pvDouble,DBType.dbPvType,null);
        assertNotNull(high);
        fields = new DBDField[] {low,high};
        DBDStructure displayLimit = DBDCreateFactory.createDBDStructure(
                "displayLimit",fields,null);
        assertNotNull(displayLimit);
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
        // create a recordType
        DBDMenuField scan = DBDCreateFactory.createDBDMenuField(
                "scan",menuScan,null);
        assertNotNull(scan);
        DBDStructureField display = DBDCreateFactory.createDBDStructureField(
                "displayLimit",displayLimit,null);
        assertNotNull(display);
        Property displayLimitProperty = FieldFactory.createProperty(
                "displayLimit","displayLimit");
        Property[]property = new Property[] {displayLimitProperty};
        DBDField value = DBDCreateFactory.createDBDField(
                "value",Type.pvDouble,DBType.dbPvType,property);
        assertNotNull(value);
        DBDField rawValue = DBDCreateFactory.createDBDField(
                "rawValue",Type.pvInt,DBType.dbPvType,null);
        assertNotNull(rawValue);
        DBDArrayField doubleArray = DBDCreateFactory.createDBDArrayField(
            "doubleArray",Type.pvDouble,DBType.dbPvType,null);
        DBDStructureField input = DBDCreateFactory.createDBDStructureField(
            "input",inputLink,null);
        assertNotNull(input);
        DBDArrayField processField = DBDCreateFactory.createDBDArrayField(
            "process",Type.pvStructure,DBType.dbLink,null);
        assertNotNull(processField);
        fields = new DBDField[] {
            scan,display,value,rawValue,doubleArray,input,processField};
        DBDStructure recordType = DBDCreateFactory.createDBDStructure(
                "ai",fields,null);
        assertNotNull(recordType);
        assertTrue(dbd.addRecordType(recordType));
        Collection<DBD> dbdList = DBDFactory.getDBDList();
        
        Iterator<DBD> iter = dbdList.iterator();
        while(iter.hasNext()) {
            dbd = iter.next();
            System.out.printf("DBD %s\n",
                    dbd.getName());
            System.out.printf("\nmenus");
            Collection<DBDMenu> menuList = dbd.getMenuList();
            Iterator<DBDMenu> menuIter = menuList.iterator();
            while(menuIter.hasNext()) {
                System.out.print(menuIter.next().toString());
            }
            System.out.printf("\n\nstructures");
            Collection<DBDStructure> structureList = dbd.getDBDStructureList();
            Iterator<DBDStructure> structureIter = structureList.iterator();
            while(structureIter.hasNext()) {
                System.out.print("\n" + structureIter.next().toString());
            }
            System.out.printf("\n\nrecordTypes");
            Collection<DBDStructure> recordTypeList = dbd.getDBDRecordTypeList();
            Iterator<DBDStructure> recordTypeIter = recordTypeList.iterator();
            while(recordTypeIter.hasNext()) {
                System.out.print("\n" + recordTypeIter.next().toString());
            }
        }
    }

}
