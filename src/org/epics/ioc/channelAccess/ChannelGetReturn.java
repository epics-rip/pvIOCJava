/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * @author mrk
 *
 */
public enum ChannelGetReturn {
    /**
     * the operation is done.
     */
    done,
    /**
     * failure.
     */
    failure,
    /**
     * the process request is active.
     */
    active,
    /**
     * the channel was already active when a process request was made.
     */
    alreadyActive
}
