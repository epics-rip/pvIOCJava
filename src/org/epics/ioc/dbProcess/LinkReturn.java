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
public enum LinkReturn {
    /**
     * the support has nothing to do. 
     */
    noop,
    /**
     * the support is done.
     */
    done,
    /**
     * failure.
     */
    failure,
    /**
     * support is still active.
     */
    active,
}
