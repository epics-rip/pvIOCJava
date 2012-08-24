/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support;

import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;

/**
 * Interface that must be implemented by IOC support.
 * @author mrk
 *
 */
public interface Support extends Requester{
    /**
     * Get the support name.
     * @return The name.
     */
    String getSupportName();
    /**
     * Get the support state.
     * @return The state.
     */
    SupportState getSupportState();
    /**
     * Get the field which this support supports.
     * @return The field.
     */
    PVRecordField getPVRecordField();
    /**
     * Initialize.
     * Perform initialization related to record instance but
     * do not connect to I/O or other records.
     */
    void initialize();
    /**
     * Invoked when it is safe to link to I/O and/or other records.
     * @param afterStart interface for being called after all support has started.
     */
    void start(AfterStart afterStart);
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
     * @param supportProcessRequester The process requester.
     */
    void process(SupportProcessRequester supportProcessRequester);
}
