/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * Return from a processContinue request.
 * @author mrk
 *
 */
public enum ProcessContinueReturn {
    /**
     * The support is done.
     */
    success,
    /**
     * The request failed.
     */
    failure,
    /**
     * The support is active.
     */
    active
}