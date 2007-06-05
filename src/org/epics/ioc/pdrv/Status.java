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
public enum Status {
    /**
     * The request was successful.
     */
    success,
    /**
     * The request failed because of a timeout.
     */
    timeout,
    /**
     * The driver has lost input data.
     * This can happen if an internal buffer or the user supplied buffer is too small.
     * Whenever possible, low level drivers should be written so that the user can
     * read input in small pieces.
     */
    overflow,
    /**
     * Some other error occured.
     */
    error
}
