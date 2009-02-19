/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;

/**
 * CDRecord - A record that holds a PVRecord.
 * @author mrk
 *
 */
public interface CDRecord {
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
     * Get the PVRecord that has the data for this CDB record instance.
     * @return The PVRecord interface.
     */
    PVRecord getPVRecord();
    /**
     * Get the interface to the subfields of this record.
     * @return The CDStructure interface.
     */
    CDStructure getCDStructure();
}
