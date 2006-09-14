/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;

import java.util.*;

/**
 * IOCDB (Input/Output Controller Database).
 * An IOCDB contains record instance definitions.
 * @author mrk
 *
 */
public interface IOCDB {
    /**
     * Get the name of the IOCDB.
     * @return The name.
     */
    String getName();
    /**
     * Get the DBD that provides reflection interfaces for menus, structures, etc.
     * @return The DBD.
     */
    DBD getDBD();
    /**
     * Get the master IOCDB.
     * In order to support on-line add of new record instances a master IOCDB can be created.
     * A separate IOCDB can be created for adding new record instances.
     * The new instances can be added to the new IOCDB and when all instances have been added
     * the new IOCDB can be merged into the master IOCDB. 
     * @return The master IOCDB or null if no master exists.
     */
    IOCDB getMasterIOCDB();
    /**
     * Merge all definitions into the master IOCDB.
     * After the merge all definitions are cleared from this IOCDB and this IOCDB is removed from the IOCDBFactory list.
     */
    void mergeIntoMaster();
    /**
     * Find the interface for a record instance.
     * It will be returned if it resides in this IOCDB or in the master IOCDB.
     * @param recordName The instance name.
     * @return The interface on null if the record is not located.
     */
    DBRecord findRecord(String recordName);
    /**
     * Add a new record instance.
     * @param record The record instance.
     * @return true if the record was created.
     */
    boolean addRecord(DBRecord record);
    /**
     * Remove a record instance.
     * @param record The record instance.
     * @return true if the record was removed and false otherwise.
     */
    boolean removeRecord(DBRecord record);
    /**
     * Get the complete set of record instances.
     * @return The map.
     */
    Map<String,DBRecord> getRecordMap();
    /**
     * Provide access to a record and it's fields.
     * @param recordName The record instance name.
     * @return The access interface.
     */
    DBAccess createAccess(String recordName);
    /**
     * Generate a list of record instance with names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the list.
     */
    String recordList(String regularExpression);
    /**
     * Dump all the record instances with names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String recordToString(String regularExpression);
}
