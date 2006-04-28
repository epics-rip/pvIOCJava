/**
 * 
 */
package org.epics.ioc.dbAccess;


/**
 * interface for a menu field.
 * @author mrk
 *
 */
public interface DBMenu extends DBData, DBEnum {
    /**
     * get the menu name.
     * @return menu name.
     */
    String getMenuName();
}
