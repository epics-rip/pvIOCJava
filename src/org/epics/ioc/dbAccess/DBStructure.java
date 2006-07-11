/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbProcess.RecordSupport;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * Data Interface for field that is a structure.
 * @author mrk
 *
 */
public interface DBStructure extends DBData, PVStructure {
    /**
     * Create fields of the structure.
     * Needed if dbdStructure is not known when a structure field is created.
     * @param dbdStructure the structure that describes the fields.
     * @return true if the fields were created.
     * The request fails if the fields were already created.
     */
    boolean createFields(DBDStructure dbdStructure);
    /**
     * Get the DBDStructure definition for this field.
     * If the DBD field definition did not specify a structure but the instance definition did then
     * this will return the DBDStructure.
     * @return The DBDStructure or null if a structure was not defined.
     */
    DBDStructure getDBDStructure();
    /**
     * Get the reflection interfaces for the fields.
     * @return An array of <i>DBField</i> that describes each of the fields in the structure.
     */
    DBData[] getFieldDBDatas();
    /**
     * Get the index of the DBData for the specified field.
     * @param fieldName The name of the field.
     * @return The index or -1 if the field does not exist.
     */
    int getFieldDBDataIndex(String fieldName);
    /**
     * Get the structure support for this structure instance.
     * @return The RecordSupport or null if no support has been set.
     */
    RecordSupport getStructureSupport();
    /**
     * Set the structure support.
     * @param support The support.
     * @return true if the support was set and false if the support already was set.
     */
    boolean setStructureSupport(RecordSupport support);
}
