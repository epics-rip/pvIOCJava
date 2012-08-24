/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.xml;

import java.util.Map;

import org.epics.pvdata.pv.PVArray;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecord;

/**
 * Methods implemented by a listener for XMLToPVDatabase.
 * @author mrk
 *
 */
public interface XMLToPVDatabaseListener {
    /**
     * Start of a structure element.
     * This can be a new structure or new definitions for an existing structure.
     * @param pvStructure The interface.
     */
    void startStructure(PVStructure pvStructure);
    /**
     * End of the current structure.
     */
    void endStructure();
    /**
     * Start of a record element.
     * This can be a new record or new definitions for an existing record.
     * @param pvRecord The interface.
     */
    void  startRecord(PVRecord pvRecord);
    /**
     * End of the current record.
     */
    void endRecord();
    /**
     * Start of a structure field.
     * @param pvStructure The interface.
     */
    void newStructureField(PVStructure pvStructure);
    /**
     * End of the structure field.
     */
    void endStructureField();
    /**
     * Start of an array field.
     * @param pvArray The interface.
     */
    void startArray(PVArray pvArray);
    /**
     * End of the current array.
     */
    void endArray();
    /**
     * Start of a scalar field.
     * @param pvScalar The interface.
     */
    void startScalar(PVScalar pvScalar);
    /**
     * End of the scalar field.
     */
    void endScalar();
    /**
     * Beginning of auxInfo.
     * @param name The tag name.
     * @param attributes The attributes.
     */
    void startAuxInfo(String name,Map<String,String> attributes);
    /**
     * End of auxInfo.
     */
    void endAuxInfo();
    
}
