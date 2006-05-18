/**
 * 
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
public class XMLToDataBaseTest extends TestCase {
        
    /**
     * test XMLToIOCDB.
     */
    public static void testXML() {
        DBD dbd = DBDFactory.create("test");
        System.out.printf("reading menuStructureSupport\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/menuStructureSupportDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading analogDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/analogDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading aiDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/aiDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading powerSupplyDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/powerSupplyDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading allTypesDBD\n");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/allTypesDBD.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }

        IOCDB iocdb = IOCDBFactory.create(dbd,"testIOCDatabase");
        System.out.printf("reading exampleAnalogDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/exampleAnalogDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading exampleAiLinearDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/exampleAiLinearDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading examplePowerSupplyDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/examplePowerSupplyDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading examplePowerSupplyArrayDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/examplePowerSupplyArrayDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("reading exampleAllTypeDB\n");
        try {
            XMLToIOCDBFactory.convert(dbd,iocdb,
                "/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbAccess/example/exampleAllTypeDB.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        Map<String,DBRecord> recordMap = iocdb.getRecordMap();
        Set<String> keys = recordMap.keySet();
        System.out.printf("\n\nrecord list\n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.printf("\n%s",record.getRecordName());
        }
        System.out.printf("\n\nrecord contents\n");
        for(String key: keys) {
            DBRecord record = recordMap.get(key);
            System.out.print(record.toString());
        }
    }

}
