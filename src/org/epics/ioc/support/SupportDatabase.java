/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;

/**
 * A SupportDatabase can be attached to a PVDatabase.
 * @author mrk
 *
 */
public interface SupportDatabase {
    /**
     * Get the PVDatabase to which this support database is attached.
     * @return The PVDatabase.
     */
    PVDatabase getPVDatabase();
    /**
     * Merge the support database into the master support database.
     */
    void mergeIntoMaster();
    /**
     * Get the record support for the pvRecord.
     * @param pvRecord The record.
     * @return The record support interface.
     */
    RecordSupport getRecordSupport(PVRecord pvRecord);
}
