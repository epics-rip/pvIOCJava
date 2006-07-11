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
public interface Support {
    /**
     * get the record support name.
     * @return the name of the record support.
     */
    String getName();
    /**
     * initialize.
     * perform initialization related to record instance but
     * do not connect to I/O pr other records.
     */
    void initialize();
    /**
     * invoked when it is safe to link to I/O and/or other records.
     */
    void start();
    /**
     * disconnect all links to I/O and/or other records.
     */
    void stop();
    /**
     * clean up any internal state.
     */
    void destroy();
}
