/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.install;

import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;

/**
 * A IOCDatabase can be attached to a PVDatabase.
 * @author mrk
 *
 */
public interface IOCDatabase {
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
     * Get the LocateSupport for the pvRecord.
     * @param pvRecord The record.
     * @return The interface.
     */
    LocateSupport getLocateSupport(PVRecord pvRecord);
}
