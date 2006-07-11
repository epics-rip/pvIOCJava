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
     * Find the interface for a record instance.
     * @param recordName The instance name.
     * @return The interface on null if the record is not located.
     */
    DBRecord findRecord(String recordName);
    /**
     * Provide access to a record and it's fields.
     * @param recordName The record instance name.
     * @return The access interface.
     */
    DBAccess createAccess(String recordName);
    /**
     * Create a new record instance.
     * @param recordName The instance name.
     * @param dbdRecordType The reflection interface for the instance record type.
     * @return true if the record was created.
     */
    boolean createRecord(String recordName, DBDRecordType dbdRecordType);
    /**
     * Get the complete set of record instances.
     * @return The collection.
     */
    Map<String,DBRecord> getRecordMap();
}
