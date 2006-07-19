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
     * get the IOCDB that this ProcessDB accesses.
     * @return the IOCDBD.
     */
    IOCDB getIOCDB();
    /**
     * find the RecordProcess interface for a record instance.
     * @param recordName the instance name.
     * @return the interface or null if the record is not located.
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
     * @param recordName
     */
    void removeRecordProcess(String recordName);
    /**
     * Find record and link support for a record instance.
     * @param recordName The name of the record.
     * @return (false,true) if all necessary support (was not, was) found.
     */
    boolean createSupport(String recordName);
    /**
     * Find record and link support.
     * @return (false,true) if all necessary support (was not, was) found.
     */
    boolean createSupport();
    /**
     * get the complete set of RecordProcess instances.
     * @return the collection.
     */
    Map<String,RecordProcess> getRecordProcessMap();
}
