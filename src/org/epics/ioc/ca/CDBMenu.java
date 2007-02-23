/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVMenu;

/**
 * @author mrk
 *
 */
public interface CDBMenu extends CDBEnum {
    /**
     * Get the PVMenu for this CDBMenu.
     * @return The PVMenu.
     */
    PVMenu getPVMenu();
}
