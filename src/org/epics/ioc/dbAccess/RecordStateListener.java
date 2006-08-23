/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * Listener for change of record state.
 * @author mrk
 *
 */
public interface RecordStateListener {
    /**
     * The record state has changed.
     * @param dbRecord The record with the state change.
     * @param newState The new state.
     */
    void newState(DBRecord dbRecord,RecordState newState);
}
