/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd.test;

import junit.framework.TestCase;

import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDRecordType;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.dbd.XMLToDBDFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.Requester;
/**
 * JUnit test for XMLToDBDFactory.
 * This also is a test for dbd and pv because XMLToDBD makes
 * extensive use of dbd and pv.
 * It also provides an example of parsing database definitions.
 * When run it produces four warning messages. These demonstrate what happens
 * when an xml file has errors.
 * The output is a dump of all the dsatabase definitions defined by test.xml.
 * @author mrk
 *
 */
public class XMLToDBDTest extends TestCase {        
    /**
     * the test.
     * This is the only public method.
     */
    public static void testXML() {
        Test test = new Test();
        test.doit();
    }
    
    private static class Test implements Requester {
        
        private void doit () {
            DBD dbd = XMLToDBDFactory.create("add",
                "src/org/epics/ioc/dbd/test/test.xml",this);
            dbd.mergeIntoMaster();
            dbd = DBDFactory.getMasterDBD();
            System.out.printf("%n%nstructures");
            DBDStructure[] dbdStructures = dbd.getDBDStructures();
            for(DBDStructure dbdStructure: dbdStructures) {
                String structureName = dbdStructure.getStructureName();
                System.out.printf("%n%nstructure %s%s",structureName,dbdStructure.toString());
            }
            System.out.printf("%n%nsupport");
            DBDSupport[] dbdSupports = dbd.getDBDSupports();
            for(DBDSupport dbdSupport: dbdSupports) {
                System.out.printf("%n%s",dbdSupport.toString());
            }
            DBDRecordType[] dbdRecordTypes = dbd.getDBDRecordTypes();
            for(DBDRecordType dbdRecordType: dbdRecordTypes) {
                System.out.printf("%n%nrecordType %s%s",dbdRecordType.getFieldName(),dbdRecordType.toString());
            }
            
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequestorName()
         */
        public String getRequesterName() {
            return "XMLToDBDTEST";
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}
