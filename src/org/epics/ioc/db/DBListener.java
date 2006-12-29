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
     * @param pvStructure If the requester is listening on a structure this is the structure.
     * It is <i>null</i> if the listener is listening on a non-structure field. 
     * @param dbData The datathat has been modified..
     */
    void structurePut(PVStructure pvStructure,DBData dbData);
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
     * The link configration structure has been modified.
     * @param pvLink The link interface.
     */
    void configurationStructurePut(PVLink pvLink);
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
