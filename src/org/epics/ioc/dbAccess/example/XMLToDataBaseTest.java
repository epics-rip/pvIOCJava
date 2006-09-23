/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.IOCMessageListener;
import org.epics.ioc.util.IOCMessageType;

import java.util.*;
/**
 * JUnit test for XMLToIOCDB.
 * This also is a test for pvAccess, dbDefinition, and dbAccess because XMLToDBD
 * is called, which makes extensive use of dbDefinition and pvAccess, and
 * XMLToIOCDB is called, which makes extensive use of dbAccess.
 * It also provides an example of parsing database definitions.
 * The output is a dump of all the record instance files it reads.
 * @author mrk
 *
 */
public class XMLToDataBaseTest extends TestCase {
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
    
            Set<String> keys;
            String list = null;
            DBD addDBD = XMLToDBDFactory.addToMaster(
                "src/org/epics/ioc/dbAccess/example/xmlToDataBaseDBD.xml",
                this,IOCMessageType.info);
            if(addDBD==null) {
                System.out.printf("XMLToDBDFactory.convert reported errors");
                return;
            }
            DBD masterDBD = DBDFactory.find("master");
            assertNotNull(masterDBD);
            list = masterDBD.menuList(".*");
            System.out.println("masterDBD menus: " + list);
            list = addDBD.menuList(".*");
            System.out.println("   addDBD menus: " + list);
            list = masterDBD.structureList(".*");
            System.out.println("masterDBD structures: " + list);
            list = addDBD.structureList(".*");
            System.out.println("   addDBD structures: " + list);
            list = masterDBD.recordTypeList(".*");
            System.out.println("masterDBD recordTypes: " + list);
            list = addDBD.recordTypeList(".*");
            System.out.println("   addDBD recordTypes: " + list);
            list = masterDBD.supportList(".*");
            System.out.println("masterDBD supports: " + list);
            list = addDBD.supportList(".*");
            System.out.println("   addDBD supports: " + list);
            IOCDB addIOCDB = XMLToIOCDBFactory.addToMaster(
                "src/org/epics/ioc/dbAccess/example/xmlToDataBaseDB.xml",
                this,IOCMessageType.info);
            if(addIOCDB==null) {
                System.out.printf("XMLToIOCDBFactory.convert reported errors");
                return;
            }
            IOCDB masterIOCDB = IOCDBFactory.find("master");
            list = masterIOCDB.recordList(".*");
            System.out.println("   masterIOCDB records: " + list);
            list = addIOCDB.recordList(".*");
            System.out.println("      addIOCDB records: " + list);
            list = masterIOCDB.recordList(".*Ai.*");
            System.out.println("masterIOCDB Ai records: " + list);
            Map<String,DBRecord> recordMap = masterIOCDB.getRecordMap();
            keys = recordMap.keySet();
            System.out.printf("%n%nrecord contents%n");
            for(String key: keys) {
                list = masterIOCDB.recordToString(key);
                System.out.println(list);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.IOCMessageListener#message(java.lang.String, org.epics.ioc.util.IOCMessageType)
         */
        public void message(String message, IOCMessageType messageType) {
            System.out.println(message);
            
        }
    }
}
