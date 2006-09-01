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
     * @param callback The listener to call when process is complete.
     * @param wait Wait until process completes?
     */
    void process(ChannelProcessListener callback, boolean wait);
    /**
     * Cancel the process request.
     */
    void cancelProcess();
}
