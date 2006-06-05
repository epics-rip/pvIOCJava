/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * The result returned by DBAccess.findField.
 * @author mrk
 *
 */
public enum AccessSetResult {
    /**
     * the requested field is located in another record.
     * calls to getRemoteRecord and getRemote Field can be used to connect to the field.
     */
    otherRecord,
    /**
     * the requested field is in this record.
     * getField can be called to retrieve the DBData interface.
     */
    thisRecord,
    /**
     * the field could not be found.
     */
    notFound
}
