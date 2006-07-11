/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * @author mrk
 *
 */
public enum RequestProcessReturn {
    /**
     * The record was set active and the caller now owns the record, i.e. the caller can call process.
     * The caller will own the record until processing is complete.
     */
    success,
    /**
     * The record is already active. The caller supplied listener will be called when the record completes processing.
     */
    listenerAdded,
    /**
     * The record is already active and the caller did not suppoly a listened.
     */
    alreadyActive,
    /**
     * The call failed.
     */
    failure
}
