/**
 * 
 */
package org.epics.ioc.dbDefinition;

/**
 * support for a menu field
 * @author mrk
 *
 */
public interface DBDMenuField extends DBDField {
    /**
     * get the menu for the field
     * @return the menu
     */
    DBDMenu getDBDMenu();
}
