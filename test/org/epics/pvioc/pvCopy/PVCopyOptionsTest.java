/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import junit.framework.TestCase;

import org.epics.pvaccess.client.CreateRequestFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.*;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVReplaceFactory;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PVCopyOptionsTest extends TestCase {
    private final static PVDatabase master = PVDatabaseFactory.getMaster();
    private final static Requester requester = new RequesterImpl();
   
    
    private static class RequesterImpl implements Requester {
		@Override
		public String getRequesterName() {
			return "pvCopyTest";
		}
		@Override
		public void message(String message, MessageType messageType) {
		    System.out.printf("message %s messageType %s%n",message,messageType.name());
			
		}
    }
    
    public static void testCopyOptions() {
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/structures.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/powerSupply.xml", iocRequester);
        PVReplaceFactory.replace(master);
        PVRecord pvRecord = master.findRecord("powerSupply");
System.out.println(pvRecord.getPVRecordStructure().getPVStructure().toString());
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        PVStructure pvCopyStructure = null;
         
        String request = "record[queueSize=2,process=true]"
                + "field(timeStamp[algorithm=onChange,causeMonitor=false],power{value[algorithm=onChange,causeMonitor=true],alarm},"
                + "current{value,alarm},voltage{value,alarm})";
//System.out.println("request " + request);
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertTrue(pvRequest!=null);
System.out.println("pvRequest " + pvRequest);
        PVStructure pvOptions = (PVStructure)pvRequest.getSubField("record._options");
        assertTrue(pvOptions!=null);
System.out.println("options " + pvOptions);
        pvCopy = PVCopyFactory.create(pvRecord, pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        PVField pvField = pvCopyStructure.getSubField("timeStamp");
        int offset = pvField.getFieldOffset();
        pvOptions = pvCopy.getOptions(pvCopyStructure, offset);
        assertTrue(pvOptions!=null);
System.out.println("options " + pvOptions);
        pvField = pvCopyStructure.getSubField("power.value");
        offset = pvField.getFieldOffset();
        pvOptions = pvCopy.getOptions(pvCopyStructure, offset);
        assertTrue(pvOptions!=null);
System.out.println("options " + pvOptions);
    }
}

