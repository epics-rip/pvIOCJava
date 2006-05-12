package org.epics.ioc;
import org.epics.ioc.dbAccess.DBRecord;
import org.epics.ioc.dbAccess.IOCDB;
import org.epics.ioc.dbAccess.IOCDBFactory;
import org.epics.ioc.dbAccess.XMLToIOCDBFactory;
import org.epics.ioc.dbDefinition.*;

import java.util.*;
import java.net.*;

/**
 * read and dump a database instance file.
 * @author mrk
 *
 */
public class XMLToDB {

    /**
     * read and dump a database instance file.
     * @param args database definition and database instance files.
     * The first argument must be a database definition file.
     * The second file must be a database instance file.
     */
    public static void main(String[] args) {
        DBD dbd = DBDFactory.create("test");
        try {
            XMLToDBDFactory.convert(dbd,args[0]);
        } catch (MalformedURLException e) {
            System.out.println("Exception: " + e);
        } catch (IllegalStateException e) {
            System.out.println("Exception: " + e);
        }

        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,args[1]);
        } catch (MalformedURLException e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("\nrecords\n");
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        Set<String> keys = recordMap.keySet();
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
    }

}
