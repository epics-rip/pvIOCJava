/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.util.*;
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
        Test test = new Test();
        test.doit();
    }
    
    private static class Test implements IOCMessageListener {
        
        private void doit () {
        
            DBD master = DBDFactory.create("master");
            listAll(master,"before reading anything");
            XMLToDBDFactory.convert(master,"src/org/epics/ioc/dbDefinition/example/menu.xml",this);
            listAll(master,"after reading menus");
            System.out.printf("menuList with S in name: %s%n",master.menuList(".*[S].*"));
            DBD add = DBDFactory.create("add");
            XMLToDBDFactory.convert(add,"src/org/epics/ioc/dbDefinition/example/structure.xml",this);
            XMLToDBDFactory.convert(add,"src/org/epics/ioc/dbDefinition/example/support.xml",this);
            XMLToDBDFactory.convert(add,"src/org/epics/ioc/dbDefinition/example/recordType.xml",this);
            listAll(master,"after reading into add");
            listAll(add,"after reading into add");
            add.mergeIntoMaster();
            listAll(master,"after merging into master");
            listAll(add,"after merging into master");
            add = null;
            dumpAll(master,"dump of everything");
            System.out.printf("%n%n******try to add already existing definitions%n%n");
            DBD newDBD = XMLToDBDFactory.create("add",
                "src/org/epics/ioc/dbDefinition/example/test.xml",this);
            System.out.printf("addToMaster is %s%n",(newDBD==null) ? "null" : "not null");
            newDBD.mergeIntoMaster();
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
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCMessageListener#message(java.lang.String, org.epics.ioc.util.IOCMessageType)
         */
        public void message(String message, IOCMessageType messageType) {
            System.out.println(message);
            
        }
    }
    
}
