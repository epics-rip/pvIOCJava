/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVMenu;
/**
 * Interface for an IOC record instance menu field.
 * @author mrk
 *
 */
public interface DBMenu extends DBEnum {
    /**
     * Get the PVMenu for this DBMenu.
     * @return The PVMenu.
     */
    PVMenu getPVMenu();
}
