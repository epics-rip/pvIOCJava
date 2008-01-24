/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;

/**
 * CDNonScalarArray - A CRRecord array field that contains non-scalar elements.
 * This means the elementType is pvArray, pvEnum, pvMenu, pvStructure, or pvStructure.
 * @author mrk
 *
 */
public interface CDStructureArray extends CDArray {
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
     * Get the CDStructure array.
     * @return The array of elements.
     * An element is null if the corresponding pvArray element is null.
     */
    CDStructure[] getElementCDStructures();
}
