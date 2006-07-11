/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * @author mrk
 *
 */
public interface Listener {
    /**
     * New data is available.
     * This is called by DBData.postData.
     * @param data The new data.
     */
    void newData(DBData data);
}
