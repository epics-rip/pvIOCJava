/**
 * 
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

/**
 * structure and recordType database definition
 * @author mrk
 *
 */
public interface DBDStructure extends Structure{
    
    /**
     * get the field descriptions
     * @return array of DBDField describing each field
     */
    DBDField[] getDBDFields();
}
