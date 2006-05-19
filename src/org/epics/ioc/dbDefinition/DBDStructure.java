/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

/**
 * reflection interface for a structure.
 * @author mrk
 *
 */
public interface DBDStructure extends DBDField, Structure {
    
    /**
     * get the field descriptions.
     * @return array of DBDField describing each field.
     */
    DBDField[] getDBDFields();
    /**
     * Get the index of the DBDField for the specified field.
     * @param fieldName the name of the field.
     * @return the index or -1 if the field does not exist.
     */
    int getDBDFieldIndex(String fieldName);
}
