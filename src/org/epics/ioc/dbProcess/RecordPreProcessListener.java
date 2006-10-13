/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * A listener passed to RecordProcess.preProcess.
 * @author mrk
 *
 */
public interface RecordPreProcessListener {
    /**
     * The record is ready for processing.
     * The caller can perform actions as required and then call recordPreProcess.processNow
     * @param recordPreProcess The interface for requesting the record to process.
     * @return The result of record processing.
     * This is just the value returned by RecordPreProcess.processNow.
     */
    ProcessReturn readyForProcessing(RecordPreProcess recordPreProcess);
    /**
     * A message if preProcess fails.
     * @param message The message.
     */
    void failure(String message);
}
