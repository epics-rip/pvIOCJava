/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;
/**
 * @author mrk
 *
 */
public interface UInt32Digital extends Interface{
	/**
	 * Get the choices.
	 * @param user The user.
	 * @return The choices or null if the driver does not know the choices.
	 */
	String[] getChoices(User user);
    /**
     * Write a value.
     * @param user The user.
     * @param value The value.
     * @param mask Bit mask specifying the bits to modify.
     * @return The status.
     */
    Status write(User user, int value, int mask);
    /**
     * Read a value.
     * @param user The user.
     * @param mask Bit mask specifying the bits to modify.
     * @return The status.
     * If the status is not Status.success or Status.timeout
     * than user.message() describes the problem.
     * If successful user.getInt() returns the value.
     * All bits corresponding to 0 values in the mask will be 0.
     */
    Status read(User user, int mask);
    /**
     * Write a value.
     * @param user The user.
     * @param mask The bits which can cause an interrupt.
     * @param reason The reason for raising an interrupt.
     * @return The status.
     */
    Status setInterrupt(User user, int mask, DigitalInterruptReason reason);
    /**
     * Get each bit that is set for reason.
     * @param user The user.
     * @param mask The bits which can cause an interrupt.
     * @param reason The reason for raising an interrupt.
     * @return The status.
     * If the status is not Status.success or Status.timeout
     * than user.message() describes the problem.
     * If successful user.getInt() returns the interrupt mask.
     */
    Status getInterrupt(User user, int mask, DigitalInterruptReason reason);
    /**
     * Clear outstanding interrupts.
     * @param user The user.
     * @param mask The bits that may have caused an interrupt.
     * @return The status.
     */
    Status clearInterrupt(User user,int mask);
    /**
     * Register a listener for value changes.
     * @param user The user.
     * @param uint32DigitalInterruptListener The listener interface.
     * @param mask Bit mask specifying the bits for which to interrupt.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status addInterruptUser(User user,
        UInt32DigitalInterruptListener uint32DigitalInterruptListener,int mask);
    /**
     * Cancel a listener.
     * @param user The user.
     * @param uint32DigitalInterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,UInt32DigitalInterruptListener uint32DigitalInterruptListener);
    /**
     * An interrupt has occurred.
     * @param data
     */
    void interruptOccurred(int data);
}
