/**
 *
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;

/**
 * The interface for DBD fields.
 * It is used for all DBType.dbPvType fields and is the base for all other DBTypes
 * @author mrk
 *
 */
public interface DBDField extends Field {
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
