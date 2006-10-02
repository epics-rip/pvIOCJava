/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;

/**
 * Record processing support.
 * 
 * @author mrk
 *
 */
public interface RecordProcess {
    /**
     * Is the record disabled.
     * A process request while a record is disabled returns a noop.
     * @return (false,true) if the record (is not, is) disabled
     */
    boolean isDisabled();
    /**
     * Set the disabled state to the requested value.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setDisabled(boolean value);
    /**
     * Is the record active.
     * @return (false,true) if the record (is not, is) active.
     */
    boolean isActive();
    /**
     * Get the record this RecordProcess processes.
     * @return The DBRecord interface.
     */
    DBRecord getRecord();
    /**
     * Initialize.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     */
    void initialize();
    /**
     * Start.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     */
    void start();
    /**
     * Stop
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     */
    void stop();
    /**
     * Uninitialize.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     */
    void uninitialize();
    /**
     * Process the record instance.
     * @param listener The listener to call to show the result.
     * @return The result of the process request.
     * If active or already active and the caller supplies a listener, it will be called when the record completes
     * processing.
     */
    ProcessReturn process(ProcessCompleteListener listener);
    /**
     * Ask the record to update.
     * If the record is not active this is a noop.
     */
    void update();
    /**
     * Remove a completion listener.
     * @param listener The listener.
     */
    void removeCompletionListener(ProcessCompleteListener listener);
    /**
     * Get RecordProcessSupport interface.
     * RecordProcessSupport is normally only used by support code.
     * @return  The interface for RecordProcessSupport.
     */
    RecordProcessSupport getRecordProcessSupport();
    /**
     * Set process trace.
     * If true a message will displayed whenever process, requestProcessCallback, or processContinue are called.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setTrace(boolean value);
}
