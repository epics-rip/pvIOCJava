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
 * JUnit test for DMLToDBD.
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
        DBD dbd = DBDFactory.create("test");
        try {
            XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbDefinition/example/test.xml");
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
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
            System.out.printf("%n%s",dbdStructure.toString());
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
            System.out.printf("%n%s",dbdRecordType.toString());
        }
        
    }

}
