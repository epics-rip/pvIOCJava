/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * TimeStamp class.
 * @author mrk
 *
 */
public class TimeStamp {
    /**
     * Seconds past the epoch
     */
    public long secondsPastEpoch;
    /**
     * Nanoseconds within the second.
     */
    public int nanoSeconds;
}
