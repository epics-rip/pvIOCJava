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
 * test default values for fields.
 * @author mrk
 *
 */
public class DefaultTest extends TestCase {
        
    /**
     * test default.
     */
    public static void testDefault() {
        DBD dbd = DBDFactory.create("master",null);
        XMLToDBDFactory.convert(dbd,
                 "src/org/epics/ioc/dbAccess/example/defaultDBD.xml");
        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase",null);
        XMLToIOCDBFactory.convert(dbd,iocdb,
                 "src/org/epics/ioc/dbAccess/example/defaultDB.xml");
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

}
