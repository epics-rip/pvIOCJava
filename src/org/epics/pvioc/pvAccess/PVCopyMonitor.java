/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvAccess;

import org.epics.pvdata.monitor.MonitorElement;


/**
 * PVCopyMonitor is a PVListener for the PVRecord to which PVCopy is attached.
 * It updates two bitSets when it receives PVListener.dataPut callbacks.
 * changeBit shows all fields that have changed between calls to updateCopy.
 * overrunBitSet shows all fields that have changed value more than once between calls
 * to updateCopy.
 * It synchronizes on changeBitSet when it accesses the two bitSets.
 * It notifies the PVCopyMonitorRequester when data has changed.
 * The caller can use this for a queue of monitors, a shared PVStructure,
 * or just a single non-shared PVStructure.
 * @author mrk
 *
 */
public interface PVCopyMonitor {
    /**
     * Start monitoring.
     */
    void startMonitoring();
    /**
     * Stop monitoring.
     */
    void stopMonitoring();
    /**
     * Set monitor element.
     * @param monitorElement
     */
    void setMonitorElement(MonitorElement monitorElement);
    /**
     * Done with monitor element.
     * @param monitorElement
     */
    void monitorDone(MonitorElement monitorElement);
}
