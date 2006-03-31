/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public interface DBStructure extends DBData, PVStructure {
    /**
     * get the reflection interfaces for the fields
     * @return an array of <i>DBField</i> that describes each of the fields in the structure.
     */
    DBData[] getFieldDBDatas();
}
