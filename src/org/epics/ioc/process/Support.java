/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.db.*;
import org.epics.ioc.util.*;

/**
 * interface that must be implemented by record support.
 * @author mrk
 *
 */
public interface Support extends Requestor{
    /**
     * Get the support state.
     * @return The state.
     */
    SupportState getSupportState();
    /**
     * Get the field which this support supports.
     * @return The field.
     */
    DBField getDBField();
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
     * @param supportProcessRequestor The process requestor.
     */
    void process(SupportProcessRequestor supportProcessRequestor);
}
