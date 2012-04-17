/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.interfaces;

import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.User;

/**
 * Interface for a device that has a 64 bit float value.
 * @author mrk
 *
 */
public interface Float64 extends Interface {
	/**
	 * Get the display limits.
	 * @param user The user.
	 * @return The limits or null if the driver does not know limits.
	 */
	double[] getDisplayLimits(User user);
	/**
	 * Get the engineering units.
	 * @param user The user.
	 * @return The units or null if the driver does not know the units.
	 */
	String getUnits(User user);
    /**
     * Write a value.
     * @param user The user.
     * @param value The value.
     * @return The status.
     * If the status is not Status.success or Status.timeout
     * than user.message() describes the problem.
     */
    Status write(User user, double value);
    /**
     * Read a value.
     * @param user The user.
     * @return The status.
     * If the status is not Status.success or Status.timeout
     * than user.message() describes the problem.
     * If succsssful user.getDouble() returns the value.
     */
    Status read(User user);
    /**
     * Register a listener for value changes.
     * @param user The user.
     * @param float64InterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status addInterruptUser(User user,Float64InterruptListener float64InterruptListener);
    /**
     * Cancel a listener.
     * @param user The user.
     * @param float64InterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,Float64InterruptListener float64InterruptListener);
    /**
     * An interrupt has occurred.
     * @param data The data.
     */
    void interruptOccurred(double data);
}
