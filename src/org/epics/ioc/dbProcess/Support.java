/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;

/**
 * interface that must be implemented by record support.
 * @author mrk
 *
 */
public interface Support extends ProcessContinueListener{
    /**
     * Get the support name.
     * @return The support name.
     */
    String getName();
    /**
     * Get the support state.
     * @return The state.
     */
    SupportState getSupportState();
    /**
     * Get the field which this support supports.
     * @return The field.
     */
    DBData getDBData();
    /**
     * Add a listener for change of SupportState.
     * @param listener The listener.
     * @return (false,true) if the listener (was not, was) added to list.
     */
    boolean addSupportStateListener(SupportStateListener listener);
    /**
     * Remove a listener for change of SupportState.
     * @param listener The listener.
     * @return (false,true) if the listener (was not, was) removed from the list.
     */
    boolean removeSupportStateListener(SupportStateListener listener);
    /**
     * Initialize.
     * Perform initialization related to record instance but
     * do not connect to I/O or other records.
     */
    void initialize();
    /**
     * Invoked when it is safe to link to I/O and/or other records.
     */
    void start();
    /**
     * Disconnect all links to I/O and/or other records.
     */
    void stop();
    /**
     * Clean up any internal state created during initialize.
     */
    void uninitialize();
    /**
     * Perform support processing.
     * @param supportProcessRequestor The listener to call when returning active.
     * @return The result of the process request.
     */
    RequestResult process(SupportProcessRequestor supportProcessRequestor);
    /**
     * Update state. This is only called while support is active.
     */
    void update();
}
