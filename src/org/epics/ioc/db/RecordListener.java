/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;


/**
 * This is an interface used for communication between BasePVRecord and derived classes.
 * It is created via a call to DBRecord.createListener.
 * @author mrk
 *
 */
public interface RecordListener {
    /**
     * Get the DBlistener.
     * @return The listener.
     */
    DBListener getDBListener();
}
