/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;

import java.util.*;

/**
 * IOCDB (Input/Output Controller Database)
 * record instance definitions
 * @author mrk
 *
 */
public interface IOCDB {
    String getName();
    DBD getDBD();
    DBRecord getRecord(String recordName);
    boolean createRecord(String recordName, DBDRecordType dbdRecordType);
    Collection<DBRecord> getRecordList();
}
