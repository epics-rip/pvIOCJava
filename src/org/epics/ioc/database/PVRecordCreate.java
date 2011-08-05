/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.database;

import org.epics.pvData.pv.PVStructure;

/**
 * @author mrk
 *
 */
public interface PVRecordCreate {
    /**
     * Create a record instance for the top level pvStructure.
     * @param recordName The instance name.
     * @param pvStructure The top level pvStructure.
     * @return The interface for accessing the record instance.
     */
    PVRecord createPVRecord(String recordName,PVStructure pvStructure);
}
