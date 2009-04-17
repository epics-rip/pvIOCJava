/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.Requester;

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
    PVField getPVField();
    /**
     * Initialize.
     * Perform initialization related to record instance but
     * do not connect to I/O or other records.
     * @param locateSupport The locateSupport for this record.
     */
    void initialize(LocateSupport locateSupport);
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
