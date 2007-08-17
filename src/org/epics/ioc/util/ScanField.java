/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

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
    ScanPriority getPriority();
    /**
     * Get the scan type.
     * @return The type.
     */
    ScanType getScanType();
    /**
     * Get the scan rate for a periodic record.
     * @return The rate in seconds.
     */
    double getRate();
    /**
     * Get the event name for an event scanned record.
     * @return The name.
     */
    String getEventName();
    /**
     * Get processSelf.
     * If processSelf is true then the record can be processed by calling processSelf.
     * @return The current value of scan.processSelf.
     */
    boolean getProcessSelf();
    void addModifyListener(ScanFieldModifyListener modifyListener);
    void removeModifyListener(ScanFieldModifyListener modifyListener);
}
