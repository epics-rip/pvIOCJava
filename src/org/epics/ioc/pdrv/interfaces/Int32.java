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
 * Interface for a device that has a 32 bit value.
 * For example an ADC (Analog to Digital Converter).
 * @author mrk
 *
 */
public interface Int32 extends Interface {
    /**
     * Write a value.
     * @param user The user.
     * @param value The value.
     * @return The status.
     * If the status is not Status.success or Status.timeout
     * than user.message() describes the problem.
     */
    Status write(User user, int value);
    /**
     * Read a value.
     * @param user The user.
     * @return The status.
     * If the status is not Status.success or Status.timeout
     * than user.message() describes the problem.
     * If successful user.getInt() returns the value.
     */
    Status read(User user);
    /**
     * Get the bounds.
     * For example a 12 bit unipolor ADC sets the bounds to 0,4095.
     * @param user The user.
     * @param bounds The array in which to put the bounds.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status getBounds(User user, int[]bounds);
    /**
     * Register a listener for value changes.
     * @param user The user.
     * @param int32InterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status addInterruptUser(User user,Int32InterruptListener int32InterruptListener);
    /**
     * Cancel a listener.
     * @param user The user.
     * @param int32InterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,Int32InterruptListener int32InterruptListener);
}
