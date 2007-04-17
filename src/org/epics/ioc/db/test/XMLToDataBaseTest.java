/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.dbd.*;
import org.epics.ioc.db.*;
import org.epics.ioc.util.Requester;
import org.epics.ioc.util.MessageType;

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
    
    private static class Test implements Requester {
        private MessageType maxMessageType = MessageType.info;
        private void doit () {
    
            Set<String> keys;
            String[] list = null;
            DBD addDBD = XMLToDBDFactory.create( "add",
                "src/org/epics/ioc/db/test/xmlToDataBaseDBD.xml",this);
            if(maxMessageType!=MessageType.info) {
                System.out.printf("XMLToDBDFactory.convert reported errors");
                return;
            }
            addDBD.mergeIntoMaster();
            DBD masterDBD = DBDFactory.getMasterDBD();
            assertNotNull(masterDBD);
            list = masterDBD.menuList(".*");
            System.out.print("masterDBD menus: "); printList(list);
            list = addDBD.menuList(".*");
            System.out.print("addDBD menus: "); printList(list);
            list = masterDBD.structureList(".*");
            System.out.print("masterDBD structures: "); printList(list);
            list = addDBD.structureList(".*");
            System.out.print("addDBD structures: "); printList(list);
            list = masterDBD.recordTypeList(".*");
            System.out.print("masterDBD recordTypes: "); printList(list);
            list = addDBD.recordTypeList(".*");
            System.out.print("addDBD recordTypes: "); printList(list);
            list = masterDBD.supportList(".*");
            System.out.print("masterDBD supports: "); printList(list);
            list = masterDBD.linkSupportList(".*");
            System.out.print("masterDBD link supports: "); printList(list);
            list = addDBD.supportList(".*");
            System.out.print("addDBD supports: "); printList(list);
            list = addDBD.linkSupportList(".*");
            System.out.print("addDBDlink  supports: "); printList(list);
            System.out.println();
            addDBD = null;
            maxMessageType = MessageType.info;
            IOCDB addIOCDB = XMLToIOCDBFactory.convert("add",
                "src/org/epics/ioc/db/test/xmlToDataBaseDB.xml",this);
            if(maxMessageType!=MessageType.info) {
                System.out.printf("XMLToIOCDBFactory.convert reported errors%n");
                //if(maxMessageType!=MessageType.warning) return;
            }
            addIOCDB.mergeIntoMaster();
            IOCDB masterIOCDB = IOCDBFactory.getMaster();
            list = masterIOCDB.recordList(".*");
            System.out.print("masterIOCDB records: "); printList(list);
            list = addIOCDB.recordList(".*");
            System.out.print("addIOCDB records: "); printList(list);
            list = masterIOCDB.recordList(".*Ai.*");
            System.out.print("masterIOCDB Ai records: "); printList(list);
            System.out.printf("%n%nrecord contents%n");
            TreeMap<String,DBRecord> recordMap = new TreeMap<String,DBRecord>(masterIOCDB.getRecordMap());
            Set<Map.Entry<String,DBRecord>> recordSet = recordMap.entrySet();
            Iterator<Map.Entry<String,DBRecord>> iter = recordSet.iterator();
            while(iter.hasNext()) {
                Map.Entry<String,DBRecord> entry = iter.next();
                System.out.println("record " + entry.getKey() + entry.getValue().toString());
            }
        }
        private void printList(String[] list) {
            for(int i=0; i<list.length; i++) {
                if((i+1)%5 == 0) {
                    System.out.println();
                    System.out.print("    ");
                } else {
                    System.out.print(" ");
                }
                System.out.print(list[i]);
            }
            System.out.println();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "XMLTODatabaseTest";
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            if(messageType.ordinal()>maxMessageType.ordinal()) {
                maxMessageType = messageType;
            }
        }
    }
}
