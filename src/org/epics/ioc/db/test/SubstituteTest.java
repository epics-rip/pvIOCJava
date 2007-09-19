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
 * This also is a test for pv, dbd, and db because XMLToDBD
 * is called, which makes extensive use of dbd and pv, and
 * XMLToIOCDB is called, which makes extensive use of db.
 * It also provides an example of parsing database definitions.
 * The output is a dump of all the record instance files it reads.
 * @author mrk
 *
 */
public class SubstituteTest extends TestCase {
        
    /**
     * test XMLToIOCDB.
     */
    public static void testXML() {
        Set<String> keys;
        DBD dbd = DBDFactory.getMasterDBD();
        Requester iocRequester = new Listener();
        XMLToDBDFactory.convert(dbd,
                "example/exampleDBD.xml",iocRequester);
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        XMLToIOCDBFactory.convert(dbd,iocdb,
            "src/org/epics/ioc/db/test/substituteDB.xml",iocRequester);
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        keys = recordMap.keySet();
        System.out.printf("%n%nrecord list%n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.printf("%n%s",record.getPVRecord().getRecordName());
        }
        System.out.printf("%n%nrecord contents%n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
    }
    
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "SubstituteTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }

}
