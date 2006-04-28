/**
 *
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;

/**
 * reflection interface for database fields.
 * It is used for all scalar fields and is the base for other type fields.
 * Thus it is used for pvBoolean, ..., pvString.
 * It is the base for pvEnum, pvArray, and pvStructure.
 * @author mrk
 *
 */
public interface DBDField extends Field {
    /**
     * retrieve the DBType.
     * @return DBType.
     */
    DBType getDBType();
    /**
     * get the attribute interface for the field.
     * @return the DBDAttribute.
     */
    DBDAttribute getDBDAttribute();
}
