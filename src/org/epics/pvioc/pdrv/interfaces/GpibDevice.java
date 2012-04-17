package org.epics.pvioc.pdrv.interfaces;

import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.User;

public interface GpibDevice {
    /**
     * Trigger the device.
     * @param user The user.
     * @return The status. If failure user.setAlarm() is called..
     */
    Status trigger(User user);
    /**
     * Clear the device.
     * @param user The user.
     * @return The status. If failure user.setAlarm() is called..
     */
    Status clear(User user);
    /**
     * Lock the device.
     * @param user The user.
     * @return The status. If failure user.setAlarm() is called..
     */
    Status lock(User user);
    /**
     * Unlock the device.
     * @param user The user.
     * @return The status. If failure user.setAlarm() is called..
     */
    Status unlock(User user);
    /**
     * Get current state of the object's EOI flag.
     * @return The current state of the object's EOI flag.
     */
    boolean getEOI();
    /**
     * Set the object's EOI flag.
     * If the object's EOI flag is true subsequent write operations
     * will assert the EOI line when the final byte of data is
     * transmitted to the device.
     * The default state of the EOI flag is true<.
     */
    void setEOI(boolean eoi);
    /**
     * Get the value of the device's status byte.
     * @param user The user.
     * @return The status. If success then user.getByte() has the status byte.
     * If failure user.setAlarm() is called..
     */
    Status getStatusByte(User user);
    /**
     * Place the device under remote control.
     * @param user The user.
     * @return The status. If failure user.setAlarm() is called..
     */
    Status remote(User user);
    /**
     * Place the device under local control.
     * @param user The user.
     * @return The status. If failure user.setAlarm() is called..
     */
    Status local(User user);
}
