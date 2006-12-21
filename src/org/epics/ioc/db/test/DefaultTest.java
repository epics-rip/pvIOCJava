/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.dbd.*;
import org.epics.ioc.db.*;
import org.epics.ioc.util.Requestor;
import org.epics.ioc.util.MessageType;

import java.util.*;
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
        Requestor iocRequestor = new Listener();
        System.out.printf("reading menuStructureSupport%n");
        XMLToDBDFactory.convert(dbd,
                 "dbd/menuStructureSupport.xml",iocRequestor);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/db/test/defaultDBD.xml",iocRequestor);
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/db/test/defaultDB.xml",iocRequestor);
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        Set<String> keys = recordMap.keySet();
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
    private static class Listener implements Requestor {
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return "DEfaultTest";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
