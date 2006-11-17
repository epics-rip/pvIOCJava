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
     * Issue a process request.
     * @return (false,true) if the request (is not, is) started.
     * This fails if the request can not be satisfied.
     */
    boolean process();
}
