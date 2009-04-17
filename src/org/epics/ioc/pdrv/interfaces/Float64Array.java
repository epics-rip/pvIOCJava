/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;
import org.epics.pvData.pv.PVDoubleArray;

/**
 * Interface for a device that has an array of 32 bit values.
 * @author mrk
 *
 */
public interface Float64Array extends Interface {
    /**
     * Get the interface to the data.
     * @return The interface.
     */
    PVDoubleArray getPVDoubleArray();
    /**
     * JavaIOC a read.
     * @param user The user
     * @return If not Status.success then status.getMessage shows the reason the request failed.
     */
    Status startRead(User user);
    /**
     * End a read.
     * @param user The user
     * @return If not Status.success then status.getMessage shows the reason the request failed.
     */
    Status endRead(User user);
    /**
     * JavaIOC a write.
     * @param user The user
     * @return If not Status.success then status.getMessage shows the reason the request failed.
     */
    Status startWrite(User user);
    /**
     * End a write.
     * @param user The user
     * @return If not Status.success then status.getMessage shows the reason the request failed.
     */
    Status endWrite(User user);
    /**
     * Register an interrupt listener.
     * @param user The user.
     * @param float64ArrayListener The listener interface.
     * @return If the request fails, than user.message() describes the problem.
     */
    Status addInterruptUser(User user,Float64ArrayInterruptListener float64ArrayListener);
    /**
     * Unregister an interrupt listener.
     * @param user The user.
     * @param float64ArrayListener The listener interface.
     * @return If the request fails, than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,Float64ArrayInterruptListener float64ArrayListener);
    /**
     * An interrupt has occurred.
     */
    void interruptOccurred();
}
