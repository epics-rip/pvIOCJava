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
     * Called when data has been modified.
     * @param dbData The interface for the modified data.
     */
    void newData(DBData dbData);
    /**
     * Begin record processing.
     * From begin until end of record processing,
     * each newData returns data modified while record is being processed.
     */
    void beginProcess();
    /**
     * End of record processing.
     */
    void endProcess();
    /**
     * Connection to record is being terminated and the RecordListener is no longer valid.
     */
    void unlisten(RecordListener listener);
}
