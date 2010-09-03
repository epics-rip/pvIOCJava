/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.database.PVRecord;
import org.epics.pvData.misc.ThreadPriority;

/**
 * Periodic Scanner.
 * This is implemented by ScannerFactory.
 * It is the interface for records that are periodically scanned.
 * New periodic threads are created dynamically as needed.
 * A record can be periodically scanned and can also have a scheduling priority.
 * A scan rate is specified by a double value with units of seconds, e.g. .05
 * says to process every .05 seconds.
 * ScannerFactory uses a minPeriod and deltaPeriod to compute actual rates.
 * The defaults are minPeriod = .01 seconds and deltaPeriod = .01 seconds.
 * These can be overridden by environment variables IOCPeriodicScanPeriodMinimum
 * and IOCPeriodicScanPeriodDelta.
 * The priority values are defined by ThreadPriority.
 * @author mrk
 *
 */
public interface PeriodicScanner {
    /**
     * Schedule a record to be periodically scanned.
     * The record must have a scan field and it must specify periodic scanning.
     * This is called by ScanField only after the record instance has been merged into
     * the master pvDatabase and the record instance has been started.
     * @param pvRecord The record instance.
     * @return false if the request failed or true if it was successful.
     */
    boolean addRecord(PVRecord pvRecord);
    /**
     * Remove the record from it's periodic scan list.
     * This is called by ScanField whenever any of the scan fields are modified or ScanField.stop is called
     * @param pvRecord The record instance.
     * @param rate The current scan rate.
     * @param threadPriority The current priority.
     * @return false if the request failed or true if it was successful.
     */
    boolean removeRecord(PVRecord pvRecord,double rate,ThreadPriority threadPriority);
    /**
     * Show a list of all records being periodically scanned.
     * @return The list.
     */
    String toString();
    /**
     * Show a list of all records being periodically scanned with the specified priority. 
     * @param priority The priority.
     * @return The list.
     */
    String show(ThreadPriority priority);
    /**
     * Show a list of all records being scanned at the specified rate.
     * @param rate The rate.
     * @return The list.
     */
    String show(double rate);
    /**
     * Show a list of all records being scanned at the specified rate and
     * with the specified priority.
     * @param rate The rate.
     * @param priority The priority.
     * @return The list.
     */
    String show(double rate,ThreadPriority priority);
}
