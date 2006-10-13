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
     * Don't allow any more requests.
     */
    void destroy();
    /**
     * Issue a process request.
     * @param channelProcessListener The listener to call when process is complete.
     * This can be null.
     * @return The result of the request.
     * 
     */
    ChannelRequestReturn process(ChannelProcessListener channelProcessListener);
    /**
     * Cancel the process request.
     */
    void cancelProcess();
}
