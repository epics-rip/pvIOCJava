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
        DBD dbd = DBDFactory.create("test");
        assertNotNull(dbd);
        String[] choices = {"passive","event","interrupt","periodic"};
        
        // create a menu
        DBDMenu menu = DBDCreateFactory.createDBDMenu("scan",choices);
        assertNotNull(menu);
        assertTrue(dbd.addMenu(menu));
        
        // create a structure
        DBDField low = DBDCreateFactory.createDBDField(
            "low",Type.pvDouble,DBType.dbPvType,null);
        assertNotNull(low);
        DBDField high = DBDCreateFactory.createDBDField(
                "high",Type.pvDouble,DBType.dbPvType,null);
        assertNotNull(high);
        DBDField[] displayLimit = new DBDField[] {low,high};
        DBDStructure structure = DBDCreateFactory.createDBDStructure(
                "displayLimit",displayLimit,null);
        assertNotNull(structure);
        assertTrue(dbd.addStructure(structure));
        
        // create a property
        Property displayLimitProperty = FieldFactory.createProperty(
                "displayLimit","displayLimit");
        Property[]property = new Property[] {displayLimitProperty};
        
        // create a recordType
        DBDMenuField scan = DBDCreateFactory.createDBDMenuField(
                "scan",menu,null);
        assertNotNull(scan);
        DBDStructureField display = DBDCreateFactory.createDBDStructureField(
                "displayLimit",structure,null);
        assertNotNull(display);
        DBDField value = DBDCreateFactory.createDBDField(
                "value",Type.pvDouble,DBType.dbPvType,property);
        assertNotNull(value);
        DBDField rawValue = DBDCreateFactory.createDBDField(
                "rawValue",Type.pvInt,DBType.dbPvType,null);
        assertNotNull(rawValue);
        DBDField[] fields = new DBDField[] {scan,display,value,rawValue};
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
            System.out.printf("\nmenus\n");
            Collection<DBDMenu> menuList = dbd.getMenuList();
            Iterator<DBDMenu> menuIter = menuList.iterator();
            while(menuIter.hasNext()) {
                System.out.print(menuIter.next().toString());
            }
            System.out.printf("\nstructures\n");
            Collection<DBDStructure> structureList = dbd.getDBDStructureList();
            Iterator<DBDStructure> structureIter = structureList.iterator();
            while(structureIter.hasNext()) {
                System.out.print(structureIter.next().toString());
            }
            System.out.printf("\nrecordTypes\n");
            Collection<DBDStructure> recordTypeList = dbd.getDBDRecordTypeList();
            Iterator<DBDStructure> recordTypeIter = recordTypeList.iterator();
            while(recordTypeIter.hasNext()) {
                System.out.print(recordTypeIter.next().toString());
            }
        }
    }

}
