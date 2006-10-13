/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * Return value for channel requests.
 * @author mrk
 *
 */
public enum ChannelRequestReturn {
    /**
     * The request is done.
     */
    success,
    /**
     * The request failed.
     */
    failure,
    /**
     * The request is active.
     */
    active,
}
