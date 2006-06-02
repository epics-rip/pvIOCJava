/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * The interface that must be supplied in the call to DBAccess.findField.
 * @author mrk
 *
 */
public interface DBAccessFind {
    /**
     * the requested field is located in another record.
     * @param name the pvname for accessing the field.
     */
    void remote(String name);
    /**
     * the requested field is in this record.
     * @param dbData the interface for the data.
     * This can be passed to DBAccess.setField.
     */
    void local(DBData dbData);
    /**
     * the field could not be found.
     */
    void notFound();
}
