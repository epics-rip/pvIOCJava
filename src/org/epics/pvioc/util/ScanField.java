/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.util;

import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVString;


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
     * Get single process requester.
     * If true that only one record process requester is allowed.
     * @return The current value of scan.singleProcessRequester
     */
    boolean getSingleProcessRequester();
    /**
     * Get singleProcessRequester interface.
     * @return The interface.
     */
    PVBoolean getSingleProcessRequesterPV();
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
