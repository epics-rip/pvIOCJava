/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * ChannelDataBaseRecord - A record that holds a PVRecord.
 * @author mrk
 *
 */
public interface CDRecord {
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
    /**
     * Get the factory for creating the introspection interfaces.
     * @return The FieldCreate interface.
     */
    FieldCreate getFieldCreate();
    /**
     * Get the factory for creating PVField instances.
     * @return The PVDataCreate interface.
     */
    PVDataCreate getPVDataCreate();
    /**
     * Create a Field that is like oldField except that it has no properties.
     * @param oldField The oldField.
     * @return The new Field interrace.
     */
    Field createField(Field oldField);
}
