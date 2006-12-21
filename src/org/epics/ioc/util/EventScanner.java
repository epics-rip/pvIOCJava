/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.db.*;

/**
 * Event Scanner.
 * This is implemented by ScannerFactory.
 * It is the interface for records that are event scanned.
 * Each event is identified by a eventName.
 * A record can be event scanned and can also have a scheduling priority,
 * which are defined by ScanPriority.
 * When an event scanned record is started, the ScanField support calls
 * EventScanner.addRecord. EventScanner manages a thread and list of records for each unique
 * eventName,priority pair.
 * When an announcer announces an event all records that are
 * in a list for the associated eventName are processed.
 * @author mrk
 *
 */
public interface EventScanner {
    /**
     * Add a record to be event scanned.
     * The record must have a scan field and it must specify event scanning.
     * @param dbRecord The record instance.
     * This is called by ScanField only after the record instance has been merged into
     * the master IOCDB and the record instance has been started.
     */
    void addRecord(DBRecord dbRecord);
    /**
     * Remove the record from it's event scanned list.
     * This is called by ScanField whenever any of the scan fields are modified or ScanField.stop is called.
     * @param dbRecord The record instance.
     */
    void removeRecord(DBRecord dbRecord);
    /**
     * Addd an event announcer.
     * @param eventName The event name.
     * @param announcer The name of the announcer.
     * @return The EventAnnounce interface that the announcer calls.
     */
    EventAnnounce addEventAnnouncer(String eventName,String announcer);
    /**
     * Remove an event announcer.
     * @param eventAnnounce The EventAnnounce returned by the call to addEventAnnounce.
     * @param announcer The name of the announcer.
     */
    void removeEventAnnouncer(EventAnnounce eventAnnounce,String announcer);
    /**
     * For all eventNames provide a list of all announcers and all record instances. 
     * @return The list.
     */
    String toString();
    /**
     * For the specified eventName provide a list of all announcers and all record instances.
     * @param eventName The event name.
     * @return The list.
     */
    String show(String eventName);
}
