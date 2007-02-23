/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * @author mrk
 *
 */
public interface CDBRecord {
    /**
     * Get the PVRecord that has the data for this CDB record instance.
     * @return The PVRecord interface.
     */
    PVRecord getPVRecord();
    /**
     * Get the interface to the subfields of this record.
     * @return The CDBStructure interface.
     */
    CDBStructure getCDBStructure();
    FieldCreate getFieldCreate();
    PVDataCreate getPVDataCreate();
    /**
     * Create a Field that is like oldField except that it has no properties.
     * @param oldField The oldField.
     * @return The new Field interrace.
     */
    Field createField(Field oldField);
}
