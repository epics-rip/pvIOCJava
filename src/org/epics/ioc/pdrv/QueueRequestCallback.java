/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;


/**
 * The user callback that is called when the users request is dequeued,
 * @author mrk
 *
 */
public interface QueueRequestCallback {
    /**
     * The queueRequest has been dequeued.
     * @param user The user.
     */
    void callback(User user);
}
