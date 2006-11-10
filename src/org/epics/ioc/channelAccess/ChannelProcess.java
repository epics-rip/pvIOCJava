/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * Channel Process Request.
 * @author mrk
 *
 */
public interface ChannelProcess {
    /**
     * Refuse further requests.
     */
    void destroy();
    /**
     * Issue a process request.
     * 
     */
    void process();
}
