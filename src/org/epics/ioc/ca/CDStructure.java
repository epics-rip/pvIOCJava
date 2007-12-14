/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;

/**
 * CDStructure - A CDRecord field that holds a PVStructure.
 * @author mrk
 *
 */
public interface CDStructure extends CDField {
    /**
     * Find the CDField which holds PVField.
     * @param pvField The PVField.
     * @return The CDField or null if not found.
     */
    CDField findCDField(PVField pvField);
    /**
     * Find the CDField which was created for the souce PVField.
     * @param sourcePVField The source PVField.
     * @return The CDField or null if not found.
     */
    CDField findSourceCDField(PVField sourcePVField);
    /**
     * Get the CDField array for the fields of the structure.
     * @return array of CDField. One for each field.
     */
    CDField[] getCDFields();
    /**
     * Get the PVStructure.
     * @return The PVStructure interface.
     */
    PVStructure getPVStructure();
}
