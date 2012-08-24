/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;

/**
 * Interface for code that wants to be notified when port.lock or port.unlock are called.
 * This is for use by a driver that is a user of another port. For example a multi-drop serial
 * driver can be implemented by being the user of a standard serial port.
 * When a user of the multi-drop driver becomes the port owner, the multi-drop driver can call lockPort
 * for the serial port and when the user of the multi-drop driver is no longer the port owner,
 * the multi-drop driver calls unlockPort for the serial port.
 * It is possible for lock to be called again BEFORE unlock is called.
 * The implementation must be prepared to handle this race condition.
 * @author mrk
 *
 */
public interface LockPortNotify {
    /**
     * The user has called port.lock.
     * @param user The user.
     */
    void lock(User user);
    /**
     * The user has called port.unlock.
     */
    void unlock();
}
