/**
 * 
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

/**
 * Describes an array field in a recordType or structure database definition
 * @author mrk
 *
 */
public interface DBDArrayField extends DBDField, Array {
    /**
     * get the element type for the array
     * @return the type
     */
    DBType getElementDBType();
}
