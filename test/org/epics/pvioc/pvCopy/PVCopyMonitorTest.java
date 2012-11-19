/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
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
import org.epics.pvdata.pv.Requester;
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
public class PVCopyMonitorTest extends TestCase {
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
    
    public static void testCopyMonitor() {
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/structures.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/powerSupply.xml", iocRequester);
        PVReplaceFactory.replace(master);
        PVRecord pvRecord = master.findRecord("powerSupply");
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoSeconds = (PVInt)pvStructure.getSubField("timeStamp.nanoSeconds");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        PVStructure pvCopyStructure = null;
        BitSet changeBitSet = null;
        BitSet overrunBitSet = null;
         
        System.out.printf(
             "%npower, current, voltage. For each value and alarm."
              + " Note that PVRecord.power does NOT have an alarm field.%n");
        String request = "field(alarm,timeStamp,power{value,alarm},"
                + "current{value,alarm},voltage{value,alarm})";
        pvRequest = CreateRequestFactory.createRequest(request,requester);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord, pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        PVLong pvCopySeconds = (PVLong)pvCopyStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvCopyNanoSeconds = (PVInt)pvCopyStructure.getSubField("timeStamp.nanoSeconds");
        PVDouble pvCopyPowerValue = (PVDouble)pvCopyStructure.getSubField("power.value");
        changeBitSet = new BitSet(pvCopyStructure.getNumberFields());
        overrunBitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, changeBitSet, true);
        CopyMonitorRequester copyMonitorRequester = new CopyMonitorRequester(pvCopy);
        copyMonitorRequester.startMonitoring(changeBitSet, overrunBitSet);
        // must flush initial
        pvRecord.beginGroupPut();
        pvRecord.endGroupPut();
        copyMonitorRequester.setDataChangedFalse();
        changeBitSet.clear();
        overrunBitSet.clear();
        pvRecord.beginGroupPut();
        assertFalse(changeBitSet.get(pvCopySeconds.getFieldOffset()));
        assertFalse(changeBitSet.get(pvCopyNanoSeconds.getFieldOffset()));
        assertFalse(changeBitSet.get(pvCopyPowerValue.getFieldOffset()));
        pvRecordSeconds.put(5000);
        pvRecordNanoSeconds.put(6000);
        pvRecordPowerValue.put(1.56);
        assertTrue(changeBitSet.get(pvCopySeconds.getFieldOffset()));
        assertTrue(changeBitSet.get(pvCopyNanoSeconds.getFieldOffset()));
        assertTrue(changeBitSet.get(pvCopyPowerValue.getFieldOffset()));
        assertFalse(overrunBitSet.get(pvCopyPowerValue.getFieldOffset()));
        pvRecordPowerValue.put(2.0);
        assertTrue(overrunBitSet.get(pvCopyPowerValue.getFieldOffset()));
        assertFalse(copyMonitorRequester.dataChanged);
        pvRecord.endGroupPut();
        assertTrue(copyMonitorRequester.dataChanged);
        copyMonitorRequester.stopMonitoring();
    }
    
    private static class CopyMonitorRequester implements PVCopyMonitorRequester {
        private PVCopyMonitor pvCopyMonitor = null;
        private boolean dataChanged = false;
        

        private CopyMonitorRequester(PVCopy pvCopy) {
            pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
        }
        
        private void startMonitoring(BitSet changeBitSet, BitSet overrunBitSet) {
            pvCopyMonitor.startMonitoring(changeBitSet, overrunBitSet);
        }
        
        private void stopMonitoring() {
            pvCopyMonitor.stopMonitoring();
        }
        
        private void setDataChangedFalse() {
            dataChanged = false;
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#dataChanged()
         */
        @Override
        public void dataChanged() {
            dataChanged = true;
        }

        /* (non-Javadoc)
         * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#unlisten()
         */
        @Override
        public void unlisten() {
            // TODO Auto-generated method stub
            
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

