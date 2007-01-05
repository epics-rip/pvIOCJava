/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * DB listener interface.
 * @author mrk
 *
 */
public interface DBListener {
    /**
     * A scalar or array modification has occured.
     * @param dbData The data.
     */
    void dataPut(DBData dbData);
    /**
     * The enum index has been modified.
     * @param pvEnum The enum interface.
     */
    void enumIndexPut(PVEnum pvEnum);
    /**
     * The enum choices has been modified.
     * @param pvEnum The enum interface.
     */
    void enumChoicesPut(PVEnum pvEnum);
    /**
     * The supportName has been modified.
     * @param dbData
     */
    void supportNamePut(DBData dbData);
    /**
     * The link configuration structure has been modified.
     * @param pvLink The link interface.
     */
    void configurationStructurePut(PVLink pvLink);
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
     * A put to a subfield of a structure has occured.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param dbData The data that has been modified.
     */
    void dataPut(PVStructure pvStructure,DBData dbData);
    /**
     * A put to an enum subfield of a structure has occured.
     * The enum index has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvEnum The enum interface.
     */
    void enumIndexPut(PVStructure pvStructure,PVEnum pvEnum);
    /**
     * A put to an enum subfield of a structure has occured.
     * The enum choices has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvEnum The enum interface.
     */
    void enumChoicesPut(PVStructure pvStructure,PVEnum pvEnum);
    /**
     * A put to a subfield of a structure has occured.
     * The supportName has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param dbData
     */
    void supportNamePut(PVStructure pvStructure,DBData dbData);
    /**
     * A put to a subfield of a structure has occured.
     * The link configuration structure has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvLink The link interface.
     */
    void configurationStructurePut(PVStructure pvStructure,PVLink pvLink);
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
