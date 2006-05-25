/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * return values for returning from process.
 * @author mrk
 *
 */
public enum ProcessReturn {
    /**
     * the support has nothing to do. 
     */
    noop,
    /**
     * the support is done.
     */
    done,
    /**
     * failure. do not attemp any further processing.
     */
    abort,
    /**
     * support is still active.
     */
    active,
    /**
     * the record was already active when a process request was made.
     */
    alreadyActive
}
