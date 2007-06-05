/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.*;

/**
 * Interface for attaching user specific data to a driver.
 * @author mrk
 *
 */
public interface DriverUser {
    /**
     * Create data for the user.
     * Warning. If a user calls create than the user MUST call dispose before
     * calling user.disconnrectPort or user.disconnectDevice.
     * @param user The user.
     * @param drvParams The drvParams from the PdrvLink.
     * @return The created object or nulll if no object was created.
     */
    Object create(User user,String drvParams);
    /**
     * Dispose of the data for this user.
     * @param user The user.
     */
    void dispose(User user);
    
}
