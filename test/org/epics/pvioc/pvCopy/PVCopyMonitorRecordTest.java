/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import junit.framework.TestCase;

import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.copy.PVCopy;
import org.epics.pvdata.copy.PVCopyFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorQueueFactory;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVReplaceFactory;
import org.epics.pvioc.pvAccess.PVCopyMonitor;
import org.epics.pvioc.pvAccess.PVCopyMonitorFactory;
import org.epics.pvioc.pvAccess.PVCopyMonitorRequester;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PVCopyMonitorRecordTest extends TestCase {
    private final static PVDatabase master = PVDatabaseFactory.getMaster();
    
    public static void testCopyMonitor() {
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/structures.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/powerSupply.xml", iocRequester);
        PVReplaceFactory.replace(master);
        PVRecord pvRecord = master.findRecord("powerSupply");
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        PVLong pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoseconds = (PVInt)pvStructure.getSubField("timeStamp.nanoseconds");
        PVDouble pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        PVStructure pvCopyStructure = null;
        String request = "field()";
        pvRequest = CreateRequest.create().createRequest(request);
//System.out.println("pvRequest " + pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
//System.out.println("pvCopyStructure " + pvCopyStructure);
        PVLong pvCopySeconds = (PVLong)pvCopyStructure.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvCopyNanoseconds = (PVInt)pvCopyStructure.getSubField("timeStamp.nanoseconds");
        PVDouble pvCopyPowerValue = (PVDouble)pvCopyStructure.getSubField("power.value");
        MonitorElement monitorElement = MonitorQueueFactory.createMonitorElement(pvCopyStructure);
        BitSet changedBitSet = monitorElement.getChangedBitSet();
        BitSet overrunBitSet = monitorElement.getOverrunBitSet();
        pvCopy.initCopy(pvCopyStructure, changedBitSet);
        CopyMonitorRequester copyMonitorRequester = new CopyMonitorRequester(pvRecord,pvCopy);
        copyMonitorRequester.startMonitoring(monitorElement);
        // must flush initial
        pvRecord.beginGroupPut();
        pvRecord.endGroupPut();
        copyMonitorRequester.setDataChangedFalse();
        changedBitSet.clear();
        overrunBitSet.clear();
        pvRecord.beginGroupPut();
        assertFalse(changedBitSet.get(pvCopySeconds.getFieldOffset()));
        assertFalse(changedBitSet.get(pvCopyNanoseconds.getFieldOffset()));
        assertFalse(changedBitSet.get(pvCopyPowerValue.getFieldOffset()));
        pvRecordSeconds.put(5000);
        pvRecordNanoseconds.put(6000);
        pvRecordPowerValue.put(1.56);
        assertTrue(changedBitSet.get(pvCopySeconds.getFieldOffset()));
        assertTrue(changedBitSet.get(pvCopyNanoseconds.getFieldOffset()));
        assertTrue(changedBitSet.get(pvCopyPowerValue.getFieldOffset()));
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

        private CopyMonitorRequester(PVRecord pvRecord,PVCopy pvCopy) {
            pvCopyMonitor = PVCopyMonitorFactory.create(this, pvRecord, pvCopy);
            
        }
        
        private void startMonitoring(MonitorElement monitorElement) {
            pvCopyMonitor.setMonitorElement(monitorElement);
            pvCopyMonitor.startMonitoring();
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

