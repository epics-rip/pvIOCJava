/**
 * 
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.dbAccess.*;

/**
 * Describes an array field in a recordType or structure database definition
 * @author mrk
 *
 */
public interface DBDArrayField extends DBDField {
    /**
     * get the element type for the array
     * @return the type
     */
    DBType getElementDBType();
}
