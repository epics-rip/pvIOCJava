/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVMenu;

/**
 * ChannelDataBaseMenu - A CDRecord field that holds a PVMenu.
 * @author mrk
 *
 */
public interface CDMenu extends CDEnum {
    /**
     * Get the PVMenu for this CDMenu.
     * @return The PVMenu.
     */
    PVMenu getPVMenu();
}
