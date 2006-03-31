/**
 * 
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.Enum;

/**
 * support for a menu field
 * @author mrk
 *
 */
public interface DBDMenuField extends DBDField, Enum {
    /**
     * get the menu for the field
     * @return the menu
     */
    DBDMenu getDBDMenu();
}
