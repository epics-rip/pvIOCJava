/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;

/**
 * Connection exception type.
 * @author mrk
 *
 */
public enum ConnectExceptionType {
    /**
     * A connect or disconnect exception has been raised.
     */
    connect,
    /**
     * An enable state has changed.
     */
    enable,
    /**
     * The autoConnect state has changed.
     */
    autoConnect
}
