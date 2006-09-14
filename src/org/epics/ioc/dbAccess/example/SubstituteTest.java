/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess.example;

import junit.framework.TestCase;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbAccess.*;

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
public class SubstituteTest extends TestCase {
        
    /**
     * test XMLToIOCDB.
     */
    public static void testXML() {
        Set<String> keys;
        DBD dbd = DBDFactory.create("master",null);
        boolean result = XMLToDBDFactory.convert(dbd,
            "src/org/epics/ioc/dbAccess/example/substituteDBD.xml");
        if(!result) System.out.printf("XMLToDBDFactory.convert reported errors");
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase",null);
        result = XMLToIOCDBFactory.convert(dbd,iocdb,
            "src/org/epics/ioc/dbAccess/example/substituteDB.xml");
        if(!result) System.out.printf("XMLToIOCDBFactory.convert reported errors");
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

}
