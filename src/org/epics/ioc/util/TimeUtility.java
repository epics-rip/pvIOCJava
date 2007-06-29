/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * Utility methods for TimeStamps.
 * @author mrk
 *
 */
public class TimeUtility {
    /**
     * Get milliseconds since epoch.
     * @param timeStamp The time stamp.
     * @return The number of milliseconds since the epoch.
     */
    public static long getMillis(TimeStamp timeStamp) {
        return timeStamp.secondsPastEpoch + (timeStamp.nanoSeconds/1000000);
    }
    /**
     * Convert milliseconds since the epoch to a TimeStamp.
     * @param timeStamp
     * @param millis
     */
    public static void set(TimeStamp timeStamp,long millis) {
        timeStamp.secondsPastEpoch = millis/1000;
        timeStamp.nanoSeconds = ((int)(millis%1000))*1000000;
    }
}
