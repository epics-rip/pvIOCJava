/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;


/**
 * An convenience interface for the scan field of a record instance.
 * This is created by ScanFieldFactory.create.
 * @author mrk
 *
 */
public interface ScanField {
    /**
     * Get the priority.
     * @return The priority.
     */
    ThreadPriority getPriority();
    /**
     * Get the priority index interface.
     * @return The priority index interface.
     */
    PVInt getPriorityIndexPV();
    /**
     * Get the scan type.
     * @return The type.
     */
    ScanType getScanType();
    /**
     * Get the scan type index interface.
     * @return The type index interface.
     */
    PVInt getScanTypeIndexPV();
    /**
     * Get the scan rate for a periodic record.
     * @return The rate in seconds.
     */
    double getRate();
    /**
     * Get the scan rate interface.
     * @return The rate interface.
     */
    PVDouble getRatePV();
    /**
     * Get the event name for an event scanned record.
     * @return The name.
     */
    String getEventName();
    /**
     * Get the event name interface.
     * @return The name interface.
     */
    PVString getEventNamePV();
    /**
     * Get processSelf.
     * If processSelf is true then the record can be processed by calling processSelf.
     * @return The current value of scan.processSelf.
     */
    boolean getProcessSelf();
    /**
     * Get processSelf interface.
     * @return The processSelf interface.
     */
    PVBoolean getProcessSelfPV();
    /**
     * Get processAfterStart.
     * Should the record be processed once after record is started.
     * @return The current value of scan.processAfterStart.
     */
    boolean getProcessAfterStart();
    /**
     * Get processAfterStart interface.
     * @return The processAfterStart interface.
     */
    PVBoolean getProcessAfterStartPV();
}
