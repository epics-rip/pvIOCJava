/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db.test;

import junit.framework.TestCase;

import org.epics.ioc.db.DBD;
import org.epics.ioc.db.DBDFactory;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.IOCDB;
import org.epics.ioc.db.IOCDBFactory;
import org.epics.ioc.db.XMLToIOCDBFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;
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
        DBD dbd = DBDFactory.getMasterDBD();
        IOCDB iocdb = IOCDBFactory.create("testIOCDatabase");
        Requester iocRequester = new Listener();
        XMLToIOCDBFactory.convert(dbd,iocdb,
                "dbd/dbd.xml",iocRequester);
        XMLToIOCDBFactory.convert(dbd,iocdb,
            "src/org/epics/ioc/db/test/substituteDB.xml",iocRequester);
        System.out.printf("%n%nrecord list%n");
        DBRecord[] dbRecords = iocdb.getDBRecords();
        for(DBRecord dbRecord : dbRecords) {
            System.out.printf("%n%s",dbRecord.getPVRecord().getRecordName());
        }
        System.out.printf("%n%nrecord contents%n");
        for(DBRecord dbRecord : dbRecords) {
            System.out.print(dbRecord.toString());
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
