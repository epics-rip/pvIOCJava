/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * Interface implemented by RecordProcessFactory and used by code that calls RecordProcess.preProcess.
 * @author mrk
 *
 */
public interface RecordPreProcess {
    /**
     * Process the record.
     * @param listener The listener to call to show the result.
     * @return The result of the process request.
     * If active and the caller supplies a listener, it will be called when the record completes
     * processing.
     */
    ProcessReturn processNow(ProcessRequestListener listener);
}
