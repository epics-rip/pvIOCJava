/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import junit.framework.TestCase;

import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.database.PVReplaceFactory;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PVRecordTest extends TestCase {
    private final static PVDatabase master = PVDatabaseFactory.getMaster();
    
    private final static Convert convert = ConvertFactory.getConvert();
   
    
    public static void testPVRecord() {
        // get database for testing
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/structures.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/powerSupplyLinked.xml", iocRequester);
        PVReplaceFactory.replace(master);
        pvRecordTest();
    }
    
    private static void showNames(PVRecordStructure recordStructure,int indentLevel)
    {
        StringBuilder builder = new StringBuilder();
        PVRecordField[] recordFields =recordStructure.getPVRecordFields();
        PVStructure pvStructure = recordStructure.getPVStructure();
        PVField[] pvFields = pvStructure.getPVFields();
        convert.newLine(builder, indentLevel);
        builder.append("structure ");
        String string = pvStructure.getFieldName();
        if(string==null) string = "null";
        if(string.length()>0) {
            builder.append(string);
            builder.append(" ");
        }
        string = recordStructure.getFullFieldName();
        if(string.length()>0) {
            builder.append(string);
            builder.append(" ");
        }
        String recordFullName = recordStructure.getFullName();
        builder.append(recordFullName);
        string = pvStructure.getFieldName();
        System.out.printf("%s",builder.toString());
        string = recordStructure.getFullName();
        if(string.length()>0) {
            builder.append(string);
            builder.append(" ");
        }
       
        builder.setLength(0);
        assertTrue(recordFields.length==pvFields.length);
        int length = recordFields.length;
        indentLevel += 1;
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            PVRecordField pvRecordField = recordFields[i];
            if(pvField.getField().getType()!=Type.structure) {
                convert.newLine(builder, indentLevel);
                builder.append(pvField.getField().getType().name());
                builder.append(" ");
                String fieldName = pvField.getFieldName();
                builder.append(fieldName);
                builder.append(" ");
                builder.append(pvRecordField.getFullFieldName());
                builder.append(" ");
                builder.append(pvRecordField.getFullName());
                builder.append(" ");
                System.out.printf("%s",builder.toString());
                builder.setLength(0);
            } else {
                showNames((PVRecordStructure)pvRecordField,indentLevel);
            }
        }
    }
    
    private static void pvRecordTest()
    {
        PVRecord pvRecord = master.findRecord("voltage");
        assertTrue(pvRecord!=null);
        System.out.println(pvRecord);
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        showNames(pvRecord.getPVRecordStructure(),0);
        System.out.println();
        PVField pvField = pvStructure.getSubField("input.input");
        assertTrue(pvField!=null);
        System.out.printf("name |%s| %n",pvField.getFieldName());
//System.out.println(pvField);
        PVRecordField pvRecordField = pvRecord.findPVRecordField(pvField);
        String fullName = pvRecordField.getFullName();
        System.out.println(fullName);
        pvRecord = master.findRecord("ai");
        if(!pvRecord.checkValid()) {
            System.err.println("pvRecord.isValid failed");
        }
        if(!pvRecord.getPVRecordStructure().getPVStructure().checkValid()) {
            System.err.println("pvStructure.isValid failed");
        }
    }
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        master.cleanMaster();
    }
}

