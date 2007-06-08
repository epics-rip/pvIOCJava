/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Get/put a menu array.
 * This is identical to PVEnum except that put will return false if the caller calls setChoices.
 * @author mrk
 *
 */
public interface PVMenu extends PVEnum {
    /**
     * Get the Menu introspection interface.
     * @return The introspection interface.
     */
    Menu getMenu();
}
