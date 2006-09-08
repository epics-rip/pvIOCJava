/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import java.util.*;
/**
 * JUnit test for DBD.
 * @author mrk
 *
 */
public class DBDTest extends TestCase {
        
    /**
     * the test.
     * This is the only public method.
     */
    public static void testXML() {
        DBD master = DBDFactory.create("master",null);
        System.out.printf("dbdList %s%n",DBDFactory.list(null));
        listAll(master,"before reading anything");
        XMLToDBDFactory.convert(master,"src/org/epics/ioc/dbDefinition/example/menu.xml");
        listAll(master,"after reading menus");
        System.out.printf("menuList with S in name: %s%n",master.menuList(".*[S].*"));
        DBD add = DBDFactory.create("add",master);
        System.out.printf("dbdList %s%n",DBDFactory.list(null));
        XMLToDBDFactory.convert(add,"src/org/epics/ioc/dbDefinition/example/structure.xml");
        XMLToDBDFactory.convert(add,"src/org/epics/ioc/dbDefinition/example/support.xml");
        XMLToDBDFactory.convert(add,"src/org/epics/ioc/dbDefinition/example/recordType.xml");
        listAll(master,"after reading into add");
        listAll(add,"after reading into add");
        add.mergeIntoMaster();
        listAll(master,"after merging into master");
        listAll(add,"after merging into master");
        dumpAll(master,"dump of everything");
    }
    
    private static void listAll(DBD dbd,String message) {
        System.out.printf("%nDBD %s %s%n",dbd.getName(),message);
        System.out.printf("menuList %s%n",dbd.menuList(null));
        System.out.printf("structureList %s%n",dbd.structureList(null));
        System.out.printf("recordTypeList %s%n",dbd.recordTypeList(null));
        System.out.printf("supportList %s%n",dbd.supportList(null));
    }
    
    private static void dumpAll(DBD dbd,String message) {
        System.out.printf("%n****DBD %s %s%n",dbd.getName(),message);
        System.out.printf("%n****menu%n%s%n",dbd.menuToString(null));
        System.out.printf("%n****structure%n %s%n",dbd.structureToString(null));
        System.out.printf("%n****recordType%n%s%n",dbd.recordTypeToString(null));
        System.out.printf("%n****support%n%s%n",dbd.supportToString(null));
    }

}
