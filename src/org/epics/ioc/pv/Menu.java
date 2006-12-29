/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Menu interface.
 * @author mrk
 *
 */
public interface Menu extends Enum {
    /**
     * Get the name of the menu.
     * @return The menu name.
     */
    String getMenuName();
}
