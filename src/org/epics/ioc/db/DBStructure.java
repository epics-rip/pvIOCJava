/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVStructure;

/**
 * Interface for a structure field.
 * @author mrk
 *
 */
public interface DBStructure extends DBField {
    /**
     * Get the <i>DBField</i> array for the fields of the structure.
     * @return array of DBField. One for each field.
     */
    DBField[] getDBFields();
    /**
     * Get the PVStructure for this DBStructure.
     * @return The PVStructure.
     */
    PVStructure getPVStructure();
}
