/**
 * 
 */
package org.epics.ioc.dbDefinition.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import java.util.*;
public class XMLToDBDTest extends TestCase {
        
    public static void testXML() {
        DBD dbd = DBDFactory.create("test");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbDefinition/example/test.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
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
        System.out.printf("\n\nlinkSupport");
        Collection<DBDLinkSupport> linkSupportList = dbd.getLinkSupportList();
        Iterator<DBDLinkSupport> linkSupportIter = linkSupportList.iterator();
        while(linkSupportIter.hasNext()) {
            System.out.print("\n" + linkSupportIter.next().toString());
        }
        System.out.printf("\n\nrecordTypes");
        Collection<DBDRecordType> recordTypeList = dbd.getDBDRecordTypeList();
        Iterator<DBDRecordType> recordTypeIter = recordTypeList.iterator();
        while(recordTypeIter.hasNext()) {
            System.out.print("\n" + recordTypeIter.next().toString());
        }
    }

}
