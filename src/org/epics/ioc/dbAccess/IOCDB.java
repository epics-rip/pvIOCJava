/**
 * 
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
     * get the interface for a record instance.
     * @param recordName the instance name.
     * @return the interface.
     */
    DBRecord getRecord(String recordName);
    /**
     * create a new record instance.
     * @param recordName the instance name.
     * @param dbdRecordType the reflection interface for the instance record type.
     * @return
     */
    boolean createRecord(String recordName, DBDRecordType dbdRecordType);
    /**
     * get the complete set of record instances.
     * @return the collection.
     */
    Collection<DBRecord> getRecordList();
}
