/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.*;

/**
 * Channel Process Request.
 * @author mrk
 *
 */
public interface ChannelProcess {
    /**
     * Issue a process request.
     * @param channelProcessRequestor The channelProcessRequestor.
     * @return The result of the request.
     * 
     */
    RequestResult process();
}
