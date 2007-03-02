/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * The result returned by PVAccess.findField.
 * @author mrk
 *
 */
public enum AccessSetResult {
    /**
     * The requested field is located in another record.
     * Calls to getRemoteRecord and getRemoteField can be used to connect to the field.
     */
    otherRecord,
    /**
     * The requested field is in this record.
     * getField can be called to retrieve the PVField interface.
     */
    thisRecord,
    /**
     * The field could not be found.
     */
    notFound
}
