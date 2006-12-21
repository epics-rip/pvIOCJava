/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVStructure;

/**
 * This is an interface used for communication between AbstractDBRecord and AbstractDBData.
 * It is created via a call to DBRecord.createListener.
 * @author mrk
 *
 */
public interface RecordListener {
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
     * @param pvStructure If the requester is listening on a structure this is the structure.
     * It is <i>null</i> if the listener is listening on a non-structure field. 
     * @param dbData The interface for the modified data.
     */
    void newData(PVStructure pvStructure,DBData dbData);
}
