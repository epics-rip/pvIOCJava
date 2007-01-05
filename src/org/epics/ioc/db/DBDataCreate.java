/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;

/**
 * Create DBData field implementations.
 * @author mrk
 *
 */
public interface DBDataCreate {
    /**
     * Create implementation for all non-array fields except enum.
     * @param parent The parent interface.
     * @param field The reflection interface for the field
     * @return The DBData implementation
     */
    public DBData createData(DBData parent,Field field);
    /**
     * Create an implementation for an enumerated field.
     * @param field The reflection interface for the field.
     * @param choice The enum choices.
     * @return The DBData implementation.
     */
    public DBData createEnumData(DBData parent,Field field, String[] choice);
    /**
     * Create an implementation for an array field.
     * @param field The reflection interface for the field.
     * @param capacity The default capacity for the field.
     * @param capacityMutable Can the capacity be changed after initialization?
     * @return The DBArray implementation.
     */
    public PVArray createArrayData(DBData parent,Field field,int capacity,boolean capacityMutable);
    /**
     * Create a record instance.
     * @param recordName The instance name.
     * @param dbdRecordType The reflection interface for the record type.
     * @return The interface for accessing the record instance.
     */
    public DBRecord createRecord(String recordName, DBDRecordType dbdRecordType);
}
