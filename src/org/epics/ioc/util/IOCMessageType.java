/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * @author mrk
 *
 */
public enum IOCMessageType {
    /**
     * Informational message.
     */
    info,
    /**
     * Warning message.
     */
    warning,
    /**
     * Error message.
     */
    error,
    /**
     * Fatal message.
     */
    fatalError
}
