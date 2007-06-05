/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

/**
 * Interface for code that wants to be notified when port.lock or port.unlock are called.
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
