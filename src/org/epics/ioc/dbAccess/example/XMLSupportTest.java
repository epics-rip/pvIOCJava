/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.MessageListener;
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
public class XMLSupportTest extends TestCase {
        
    /**
     * test XMLToIOCDB.
     */
    public static void testXML() {
        Set<String> keys;
        DBD dbd = DBDFactory.getMasterDBD();
        
        System.out.printf("reading supportDBD%n");
        MessageListener iocMessageListener = new Listener();
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/supportDBD.xml",iocMessageListener);
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        System.out.printf("reading exampleSupportDB%n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/dbAccess/example/supportDB.xml",iocMessageListener);
        } catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        keys = recordMap.keySet();
        System.out.printf("%n%nrecord list%n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.printf("%n%s",record.getRecordName());
        }
        System.out.printf("%n%nrecord contents%n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
    }

    private static class Listener implements MessageListener {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.MessageListener#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
