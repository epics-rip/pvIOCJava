/**
 * 
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;
import java.util.*;
import java.net.*;
public class XMLToDataBaseTest extends TestCase {
        
    public static void testXML() {
        DBD dbd = DBDFactory.create("test");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/dbd.xml");
        } catch (MalformedURLException e) {
            System.out.println("Exception: " + e);
        }

        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/testDb.xml");
        } catch (MalformedURLException e) {
            System.out.println("Exception: " + e);
        }
        

        System.out.printf("\nrecords\n");
        Collection<DBRecord> recordList = iocdb.getRecordList();
        Iterator<DBRecord> recordIter = recordList.iterator();
        while(recordIter.hasNext()) {
            DBRecord record = recordIter.next();
            System.out.print(record.toString());
        }
    }

}
