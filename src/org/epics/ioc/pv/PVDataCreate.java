/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Create PVData field implementations.
 * @author mrk
 *
 */
public interface PVDataCreate {
    /**
     * Create implementation for all non-array fields except enum.
     * @param parent The parent interface.
     * The parent can be a PVStructure, PVArray, or PVLink.
     * @param field The reflection interface for the field
     * @return The PVData implementation
     */
    public PVData createData(PVData parent,Field field);
    /**
     * Create an implementation for an array field.
     * @param parent The parent interface.
     * The parent can be a PVStructure or PVArray.
     * @param field The reflection interface for the field.
     * @param capacity The default capacity for the field.
     * @param capacityMutable Can the capacity be changed after initialization?
     * @return The PVArray implementation.
     */
    public PVArray createArrayData(PVData parent,Field field,int capacity,boolean capacityMutable);
    /**
     * Create a record instance.
     * @param recordName The instance name.
     * @param dbdRecordType The reflection interface for the record type.
     * @return The interface for accessing the record instance.
     */
    public PVRecord createRecord(String recordName,Structure structure);
}
