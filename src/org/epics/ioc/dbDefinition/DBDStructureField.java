/**
 * 
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

/**
 * support for a field that is a structure
 * @author mrk
 *
 */
public interface DBDStructureField extends DBDField, Structure {
    
    /**
     * get the structure describing the field
     * @return the DBDStructure
     */
    DBDStructure getDBDStructure();
}
