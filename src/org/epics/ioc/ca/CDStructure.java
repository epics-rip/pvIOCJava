/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVStructure;

/**
 * CDStructure - A CDRecord field that holds a PVStructure.
 * @author mrk
 *
 */
public interface CDStructure extends CDField {
    /**
     * Get the <i>CDField</i> array for the fields of the structure.
     * @return array of CDField. One for each field.
     */
    CDField[] getCDFields();
    /**
     * Replace the PVStructure.
     */
    void replacePVStructure();
    /**
     * Get the PVStructure.
     * @return The PVStructure interface.
     */
    PVStructure getPVStructure();
}
