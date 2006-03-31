/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * Base interface for database array data
 * @author mrk
 *
 */
public interface DBArray extends DBData, PVArray {
    /**
     * get the reflection interface for the array
     * @return the DBDArrayField describing the array
     */
    DBDArrayField getDBDArrayField();
}
