/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.XMLToIOCDBFactory;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.XMLToDBDFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;
/**
 * test default values for fields.
 * @author mrk
 *
 */
public class DefaultTest extends TestCase {
        
    /**
     * test default.
     */
    public static void testDefault() {
        DBD dbd = DBDFactory.getMasterDBD();
        Requester iocRequester = new Listener();
        System.out.printf("reading Support%n");
        XMLToDBDFactory.convert(dbd,
                 "example/exampleDBD.xml",iocRequester);
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        XMLToIOCDBFactory.convert(dbd,iocdb,
                 "example/allTypesDB.xml",iocRequester);
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        Set<String> keys = recordMap.keySet();
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
            return "DEfaultTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
