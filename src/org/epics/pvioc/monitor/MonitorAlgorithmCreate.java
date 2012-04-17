/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.monitor;


import org.epics.pvdata.monitor.MonitorAlgorithm;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;

/**
 * An interface implemented by code that implements a monitor algorithm.
 * @author mrk
 *
 */
public interface MonitorAlgorithmCreate {
    /**
     * Get the name of the algorithm.
     * @return The name.
     */
    String getAlgorithmName();
    /**
     * Create a MonitorAlgorithm.
     * @param pvRecord The record;
     * @param monitorRequester The requester.
     * @param fromPVRecord The field in the PVRecord for the algorithm.
     * @param pvOptions The options for the client.
     * @return The MonitorAlgorithm interface.
     */
    MonitorAlgorithm create(
            PVRecord pvRecord,
            MonitorRequester monitorRequester,
            PVRecordField fromPVRecord,
            PVStructure pvOptions);
}
