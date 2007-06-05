/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

/**
 * @author mrk
 *
 */
public interface ConnectExceptionListener {
    /**
     * A connection exception has occured.
     * @param connectException The type of exception.
     */
    void exception(ConnectException connectException);
}
