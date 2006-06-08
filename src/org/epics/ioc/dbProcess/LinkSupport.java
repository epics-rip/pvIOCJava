/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;

/**
 * interface that must be implemented by link support.
 * @author mrk
 *
 */
public interface LinkSupport {
    /**
     * get the link support name.
     * @return the name of the link support.
     */
    String getName();
    /**
     * initialize.
     * Note that 'other' records that are for example referenced by
     * input or forward links are available but might still be
     * uninitialized.
     */
    void initialize();
    /**
     * invoked by the database when it is safe to link to I/O and/or other records.
     * typically, start() will start input links etc.
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
