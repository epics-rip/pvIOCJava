/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVArrayArray;

/**
 * Interface for non scalar arrays.
 * @author mrk
 *
 */
public interface DBArrayArray extends DBArray{
    /**
     * Get the PVArrayArray interface for this field.
     * @return The interface.
     */
    PVArrayArray getPVArrayArray();
    /**
     * Get the CDArray array.
     * @return The array of elements.
     * An element is null if the corresponding pvArray element is null.
     */
    DBArray[] getElementDBArrays();
    /**
     * Replace the current PVArray.
     */
    void replacePVArray();
}
