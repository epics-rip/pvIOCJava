/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * listener interface.
 * @author mrk
 *
 */
public interface DBListener {
    /**
     * called when data has been modified.
     * @param dbData the interface for the modified data.
     */
    void newData(DBData dbData);
    /**
     * begin synchronous data.
     * From begin until end of synchronous data,
     * each newData returns part of the synchronous data.
     */
    void beginSynchronous();
    /**
     * end of synchronous data.
     */
    void endSynchronous();
}
