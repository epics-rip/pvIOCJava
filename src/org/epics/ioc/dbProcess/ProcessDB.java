/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.Map;

import org.epics.ioc.dbAccess.*;

/**
 * Database for ror the RecordProcess for each record instance.
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
    RecordProcess findRecordProcess(String recordName);
    /**
     * add a RecordProcess to the processDB.
     * @param recordProcess the RecordProcess.
     * @return true if it was added and false if it was already in database.
     */
    boolean addRecordProcess(RecordProcess recordProcess);
    /**
     * get the complete set of RecordProcess instances.
     * @return the collection.
     */
    Map<String,RecordProcess> getRecordProcessMap();
}
