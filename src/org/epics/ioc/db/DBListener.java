/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * listener interface.
 * @author mrk
 *
 */
public interface DBListener {
    /**
     * Start of a structure modification.
     * @param pvStructure The structure.
     */
    void beginPut(PVStructure pvStructure);
    /**
     * End of a structure modification.
     * @param pvStructure The structure.
     */
    void endPut(PVStructure pvStructure);
    /**
     * Called when data has been modified.
     * @param pvStructure If the requester is listening on a structure this is the structure.
     * It is <i>null</i> if the listener is listening on a non-structure field. 
     * @param dbData The interface for the modified data.
     */
    void newData(PVStructure pvStructure,DBData dbData);
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
