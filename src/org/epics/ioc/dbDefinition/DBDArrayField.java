/**
 *
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;

/**
 * The interface for DBD Array fields.
 * @author mrk
 *
 */
public interface DBDArrayField extends DBDField, Array {
    /**
     * retrieve the DBType
     * @return DBType
     */
    DBType getDBType();
    /**
     * get the attribute interface for the field
     * @return the DBDAttribute
     */
    DBDAttribute getDBDAttribute();
}
