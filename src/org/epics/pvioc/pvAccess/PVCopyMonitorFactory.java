/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvAccess;

import org.epics.pvdata.copy.PVCopy;
import org.epics.pvdata.copy.PVCopyTraverseMasterCallback;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.pv.PVField;
import org.epics.pvioc.database.PVListener;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
         
public class PVCopyMonitorFactory implements PVCopyMonitor, PVCopyTraverseMasterCallback, PVListener{
    private PVCopyMonitorRequester pvCopyMonitorRequester;
    private PVRecord pvRecord;
    private PVCopy pvCopy;
    private MonitorElement monitorElement = null;
    private boolean isGroupPut = false;
    private boolean dataChanged = false;
    private boolean isMonitoring = false;

    public static PVCopyMonitor create(
            PVCopyMonitorRequester pvCopyMonitorRequester,
            PVRecord pvRecord,
            PVCopy pvCopy)
    {
        return new PVCopyMonitorFactory(pvCopyMonitorRequester,pvRecord,pvCopy);
    }

    private PVCopyMonitorFactory(
            PVCopyMonitorRequester pvCopyMonitorRequester,
            PVRecord pvRecord,
            PVCopy pvCopy) {
        this.pvCopyMonitorRequester = pvCopyMonitorRequester;
        this.pvRecord = pvRecord;
        this.pvCopy = pvCopy;
    }
    @Override
    public synchronized  void startMonitoring() {
        if(isMonitoring) return;
        isMonitoring = true;
        isGroupPut = false;
        pvRecord.lock();
        try {
            pvRecord.registerListener(this);
            pvCopy.traverseMaster(this);
            monitorElement.getChangedBitSet().clear();
            monitorElement.getOverrunBitSet().clear();
            monitorElement.getChangedBitSet().set(0);
            pvCopyMonitorRequester.dataChanged();
        } finally {
            pvRecord.unlock();
        }
    }

    @Override
    public void setMonitorElement(MonitorElement monitorElement) {
        this.monitorElement = monitorElement;
    }

    @Override
    public synchronized void stopMonitoring() {
        if(!isMonitoring) return;
        isMonitoring = false;
        pvRecord.unregisterListener(this);
    }
    @Override
    public void monitorDone(MonitorElement monitorElement) {
        // TODO Auto-generated method stub

    }

    @Override
    public void nextMasterPVField(PVField pvField) {
        pvRecord.findPVRecordField(pvField).addListener(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVListener#beginGroupPut(org.epics.pvdata.pv.PVRecord)
     */
     @Override
     public void beginGroupPut(PVRecord pvRecord) {
        isGroupPut = true;
        dataChanged = false;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.database.PVListener#dataPut(org.epics.pvioc.database.PVRecordField)
     */
     @Override
     public void dataPut(PVRecordField pvRecordField) {
         int offset = pvCopy.getCopyOffset(pvRecordField.getPVField());
         BitSet changedBitSet = monitorElement.getChangedBitSet();
         BitSet overrunBitSet = monitorElement.getOverrunBitSet();
         boolean isSet = changedBitSet.get(offset);
         changedBitSet.set(offset);;
         if(isSet) overrunBitSet.set(offset);
         if(!isGroupPut) pvCopyMonitorRequester.dataChanged();
         dataChanged = true;
     }
     /* (non-Javadoc)
      * @see org.epics.pvioc.database.PVListener#dataPut(org.epics.pvioc.database.PVRecordStructure, org.epics.pvioc.database.PVRecordField)
      */
     @Override
     public void dataPut(PVRecordStructure requested,PVRecordField pvRecordField) {
         BitSet changedBitSet = monitorElement.getChangedBitSet();
         BitSet overrunBitSet = monitorElement.getOverrunBitSet();
         int offsetCopyRequested = pvCopy.getCopyOffset(requested.getPVField());
         int offset = offsetCopyRequested +(pvRecordField.getPVField().getFieldOffset()
                 - requested.getPVField().getFieldOffset());
         boolean isSet = changedBitSet.get(offset);
         changedBitSet.set(offset);;
         if(isSet) overrunBitSet.set(offset);
         if(!isGroupPut) pvCopyMonitorRequester.dataChanged();
         dataChanged = true;
     }
     /* (non-Javadoc)
      * @see org.epics.pvdata.pv.PVListener#endGroupPut(org.epics.pvdata.pv.PVRecord)
      */
     @Override
     public void endGroupPut(PVRecord pvRecord) {
         isGroupPut = false;
         if(dataChanged) {
             dataChanged = false;
             pvCopyMonitorRequester.dataChanged();
         }
     }

     /* (non-Javadoc)
      * @see org.epics.pvdata.pv.PVListener#unlisten(org.epics.pvdata.pv.PVRecord)
      */
     @Override
     public void unlisten(PVRecord pvRecord) {
         pvCopyMonitorRequester.unlisten();
     }
}
