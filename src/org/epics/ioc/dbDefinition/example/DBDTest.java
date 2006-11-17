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
    
    private static class Test implements Requestor {
        
        private void doit () {
        
            DBD master = DBDFactory.create("master");
            listAll(master,"before reading anything");
            XMLToDBDFactory.convert(master,"src/org/epics/ioc/dbDefinition/example/menu.xml",this);
            listAll(master,"after reading menus");
            String[] list = master.menuList(".*[S].*");
            System.out.print("menuList with S in name:");
            for(String item : list) System.out.print(" " + item);
            System.out.println();
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
            String[] list;
            System.out.printf("%nDBD %s %s%n",dbd.getName(),message);
            list = dbd.menuList(null);
            System.out.print("menuList:");
            for(String item : list) System.out.print(" " + item);
            System.out.println();
            list = dbd.structureList(null);
            System.out.print("structureList:");
            for(String item : list) System.out.print(" " + item);
            System.out.println();
            list = dbd.recordTypeList(null);
            System.out.print("recordTypeList:");
            for(String item : list) System.out.print(" " + item);
            System.out.println();
            list = dbd.supportList(null);
            System.out.print("supportList:");
            for(String item : list) System.out.print(" " + item);
            System.out.println();
        }
        
        private static void dumpAll(DBD dbd,String message) {
            System.out.printf("%n****DBD %s %s%n",dbd.getName(),message);
            System.out.printf("%n****menu%n%s%n",dbd.menuToString(null));
            System.out.printf("%n****structure%n %s%n",dbd.structureToString(null));
            System.out.printf("%n****recordType%n%s%n",dbd.recordTypeToString(null));
            System.out.printf("%n****support%n%s%n",dbd.supportToString(null));
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "DBDTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
    
}
