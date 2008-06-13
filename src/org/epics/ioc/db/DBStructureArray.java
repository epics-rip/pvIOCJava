/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;
import org.epics.ioc.pv.PVStructureArray;

/**
 * Interface for an array of structures.
 * @author mrk
 *
 */
public interface DBStructureArray extends DBArray{
    /**
     * Get the PVStructureArray for this field.
     * @return The PVStructureArray interface.
     */
    PVStructureArray getPVStructureArray();
    /**
     * Get the DBStructure array.
     * @return The array of elements.
     */
    DBStructure[] getElementDBStructures();
}
