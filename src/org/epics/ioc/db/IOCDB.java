/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.util.*;

import java.util.*;

/**
 * IOCDB (Input/Output Controller Database).
 * An IOCDB contains record instance definitions.
 * @author mrk
 *
 */
public interface IOCDB extends Requestor{
    /**
     * Get the master IOCDB.
     * @return The master IOCDB.
     */
    IOCDB getMaster();
    /**
     * Get the name of the IOCDB.
     * @return The name.
     */
    String getName();
    /**
     * Merge all definitions into the master IOCDB.
     * After the merge all definitions are cleared from this IOCDB and this IOCDB is removed from the IOCDBFactory list.
     */
    void mergeIntoMaster();
    /**
     * Add a listener to call after the IOCDB is merged into the master.
     * If this is the master the listener will be called immediately.
     * @param listener The listener.
     */
    void addIOCDBMergeListener(IOCDBMergeListener listener);
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
     * Report a message.
     * If no listeners are registered the messages are sent to System.out.
     * If listeners are registered they are called.
     * @param message The message.
     * @param messageType The message type.
     */
    void message(String message, MessageType messageType);
    /**
     * Add a message requestor.
     * @param requestor The requestor.
     */
    void addRequestor(Requestor requestor);
    /**
     * Remove a message requestor.
     * @param requestor The requestor.
     */
    void removeRequestor(Requestor requestor);
    /**
     * Generate a list of record instance with names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] recordList(String regularExpression);
    /**
     * Dump all the record instances with names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String recordToString(String regularExpression);
}
