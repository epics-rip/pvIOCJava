/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;


/**
 * Channel Process Request.
 * @author mrk
 *
 */
public interface ChannelProcess {
    /**
     * Issue a process request.
     * This fails if the request can not be satisfied.
     * If it fails the channelProcessRequestor.processDone is called before process returns.
     */
    void process();
}
