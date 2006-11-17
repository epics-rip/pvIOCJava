/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition.example;

import junit.framework.TestCase;
import java.util.*;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.util.*;
/**
 * JUnit test for XMLToDBDFactory.
 * This also is a test for dbDefinition and pvAccess because XMLToDBD makes
 * extensive use of dbDefinition and pvAccess.
 * It also provides an example of parsing database definitions.
 * When run it produces four warning messages. These demonstrate what happens
 * when an xml file has errors.
 * The output is a dump of all the dsatabase definitions defined by test.xml.
 * @author mrk
 *
 */
public class XMLToDBDTest extends TestCase {        
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
            DBD dbd = XMLToDBDFactory.create("add",
                "src/org/epics/ioc/dbDefinition/example/test.xml",this);
            dbd.mergeIntoMaster();
            dbd = DBDFactory.getMasterDBD();
            System.out.printf("%nmenus");
            Map<String,DBDMenu> menuMap = dbd.getMenuMap();
            Set<String> keys = menuMap.keySet();
            for(String key: keys) {
                DBDMenu dbdMenu = menuMap.get(key);
                System.out.printf("%n%s",dbdMenu.toString());
            }
            System.out.printf("%n%nstructures");
            Map<String,DBDStructure> structureMap = dbd.getStructureMap();
            keys = structureMap.keySet();
            for(String key: keys) {
                DBDStructure dbdStructure = structureMap.get(key);
                System.out.printf("%n%nstructure %s%s",dbdStructure.getName(),dbdStructure.toString());
            }
            System.out.printf("%n%nsupport");
            Map<String,DBDSupport> supportMap = dbd.getSupportMap();
            keys = supportMap.keySet();
            for(String key: keys) {
                DBDSupport dbdSupport = supportMap.get(key);
                System.out.printf("%n%s",dbdSupport.toString());
            }
            System.out.printf("%n%nrecordTypes");
            Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
            keys = recordTypeMap.keySet();
            for(String key: keys) {
                DBDRecordType dbdRecordType = recordTypeMap.get(key);
                System.out.printf("%n%nrecordType %s%s",dbdRecordType.getName(),dbdRecordType.toString());
            }
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "XMLToDBDTEST";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
