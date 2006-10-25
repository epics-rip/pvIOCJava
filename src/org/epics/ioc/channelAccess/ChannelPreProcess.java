/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.RequestResult;

/**
 * Request that a channel become active but call the requestor before the channel is processed.
 * @author mrk
 *
 */
public interface ChannelPreProcess {
    /**
     * Issue a preProcess request.
     * @param channelProcessRequestor The channelProcessRequestor.
     * @return The result of the request.
     */
    RequestResult preProcess();
    /**
     * Process the channel.
     * This is called by a channelProcessRequestor when from method ready.
     * @param channelProcessRequestor The channelProcessRequestor.
     * @return The result of the request.
     */
    RequestResult processNow();
}
