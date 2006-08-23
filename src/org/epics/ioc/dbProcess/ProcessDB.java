/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.Map;

import org.epics.ioc.dbAccess.*;

/**
 * Database for the RecordProcess for each record instance.
 * @author mrk
 *
 */
public interface ProcessDB {
    /**
     * Get the IOCDB that this ProcessDB accesses.
     * @return The IOCDBD.
     */
    IOCDB getIOCDB();
    /**
     * Find the RecordProcess interface for a record instance.
     * @param recordName The instance name.
     * @return The interface or null if the record is not located.
     */
    RecordProcess getRecordProcess(String recordName);
    /**
     * Create and add a RecordProcess to the processDB.
     * @param recordName The name of the record instance.
     * @return true if it was added and false if it was already in database.
     */
    boolean createRecordProcess(String recordName);
    /**
     * Remove the RecordProcess from the database.
     * @param recordName The name of the record instance.
     */
    void removeRecordProcess(String recordName);
    /**
     * Create record and link support for a record instance.
     * @param recordName The name of the record.
     * @return (false,true) if all necessary support (was not, was) created.
     */
    boolean createSupport(String recordName);
    /**
     * Create record and link support for all record instances in the iocdb
     * as well a a RecordProcess for each instance.
     * @return (false,true) if all necessary support (was not, was) created.
     */
    boolean createSupport();
    /**
     * Get the complete set of RecordProcess instances.
     * @return The collection.
     */
    Map<String,RecordProcess> getRecordProcessMap();
}
