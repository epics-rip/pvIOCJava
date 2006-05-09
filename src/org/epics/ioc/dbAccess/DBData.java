/**
 * 
 */
package org.epics.ioc.dbAccess;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * The base interface for accessing a field of a record instance.
 * @author mrk
 *
 */
public interface DBData extends PVData {
    /**
     * get the reflection interface for the field.
     * @return the DBDField that describes the field.
     */
    DBDField getDBDField();
    /**
     * get the parent of this field.
     * @return the parent interface.
     */
    DBStructure getParent();
    /**
     * get the record instance that contains this field.
     * @return the record interface.
     */
    DBRecord getRecord();
}
