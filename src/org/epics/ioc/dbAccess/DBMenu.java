/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
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
