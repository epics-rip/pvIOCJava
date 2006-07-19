/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * This is an interface used for communication between AbstractDBRecord and AbstractDBData.
 * It is created via a call to DBRecord.createListener.
 * @author mrk
 *
 */
public interface RecordListener {
    /**
     * New data is available.
     * This is called by DBData.postData.
     * @param data The new data.
     */
    void newData(DBData data);
}
