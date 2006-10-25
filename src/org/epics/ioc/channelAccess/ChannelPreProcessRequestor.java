/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.RequestResult;

/**
 * Interface implemented by code that calls ChannelProcess.preProcess.
 * @author mrk
 *
 */
public interface ChannelPreProcessRequestor extends ChannelProcessRequestor{
    /**
     * The channel is ready for processing.
     * This is called if the channelRequestor makes a ChannelProcess.preProcess request.
     * The caller can perform actions as required and then call channelProcess.processNow
     * @return The result of processing.
     * This is just the value returned by channelPreProcess.processNow.
     */
    RequestResult ready();
}
