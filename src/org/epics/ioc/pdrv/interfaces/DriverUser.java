/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.*;
import org.epics.ioc.pv.PVStructure;

/**
 * Interface for attaching user specific data to a driver.
 * @author mrk
 *
 */
public interface DriverUser {
    /**
     * Create data for the user.
     * The driver should use the User methods setPortDriverUserPvt/getPortDriverUserPvt
     * or setDeviceDriverUserPvt/getDeviceDriverUserPvt to store and private data it allocates
     * for the user.
     * Warning. If a user calls create than the user should call dispose before
     * calling user.disconnectPort or user.disconnectDevice.
     * @param user The user.
     * @param drvParams The drvParams from the PdrvLink.
     */
    void create(User user,PVStructure drvParams);
    /**
     * Dispose of the data for this user.
     * @param user The user.
     */
    void dispose(User user);
    
}
