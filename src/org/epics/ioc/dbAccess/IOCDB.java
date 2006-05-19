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
     * get the name of the IOCDB.
     * @return the name.
     */
    String getName();
    /**
     * get the DBD that provides reflection interfaces for menus, structures, etc.
     * @return the DBD.
     */
    DBD getDBD();
    /**
     * find the interface for a record instance.
     * @param recordName the instance name.
     * @return the interface on null if the record is not located.
     */
    DBRecord findRecord(String recordName);
    /**
     * Provide access to a record and it's fields.
     * @param recordName the record instance name.
     * @return the access interface.
     */
    DBAccess createAccess(String recordName);
    /**
     * create a new record instance.
     * @param recordName the instance name.
     * @param dbdRecordType the reflection interface for the instance record type.
     * @return true if the record was created.
     */
    boolean createRecord(String recordName, DBDRecordType dbdRecordType);
    /**
     * get the complete set of record instances.
     * @return the collection.
     */
    Map<String,DBRecord> getRecordMap();
}
