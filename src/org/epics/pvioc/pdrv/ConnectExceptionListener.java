/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;

/**
 * @author mrk
 *
 */
public interface ConnectExceptionListener {
    /**
     * A connection exception has occured.
     * This is normally called with the port owned by another user.
     * @param connectException The type of exception.
     */
    void exception(ConnectExceptionType connectException);
}
