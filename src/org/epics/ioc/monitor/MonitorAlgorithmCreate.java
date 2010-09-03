/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.monitor;


import org.epics.ioc.database.PVRecord;
import org.epics.ioc.database.PVRecordField;
import org.epics.pvData.monitor.MonitorAlgorithm;
import org.epics.pvData.monitor.MonitorRequester;
import org.epics.pvData.pv.PVStructure;

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
