/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;


/**
 * The user callback that is called when the users request is dequeued,
 * @author mrk
 *
 */
public interface QueueRequestCallback {
    /**
     * The queueRequest has been dequeued.
     * @param status Status of the request.
     * The request will fail if the port is not connected or if it is disabled.
     * If the status is not Status.success than the user must NOT
     * call and interfaces for the port and user.getMessage provides the reason.
     * @param user The user.
     */
    void callback(Status status,User user);
}
