/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Interface;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 * Interface for a device that has an array of 32 bit values.
 * @author mrk
 *
 */
public interface Int32Array extends Interface{
    /**
     * Write an array.
     * @param user The user.
     * @param value The array of values.
     * @param length The number of elements to write.
     * @return The status of the request.
     * user.getInt() returns the number of elements written.
     */
    Status write(User user, int[] value,int length);
    /**
     * Read an array.
     * @param user The user.
     * @param value The array of values.
     * @param length The number of elements to read.
     * @return The status of the request.
     * user.getInt() returns the number of elements read.
     */
    Status read(User user,int[] value,int length);
    /**
     * Register an interrupt listener.
     * @param user The user.
     * @param int32ArrayListener The listener interface.
     * @return If the request fails, than user.message() describes the problem.
     */
    Status addInterruptUser(User user,Int32ArrayInterruptListener int32ArrayListener);
    /**
     * Unregister an interrupt listener.
     * @param user The user.
     * @param int32ArrayListener The listener interface.
     * @return If the request fails, than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,Int32ArrayInterruptListener int32ArrayListener);
}
