/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * @author mrk
 *
 */
public class TimeUtility {
    public static long getMillis(TimeStamp timeStamp) {
        return timeStamp.seconds + (timeStamp.nanoSeconds/1000000);
    }
    public static void set(TimeStamp timeStamp,long millis) {
        timeStamp.seconds = millis/1000;
        timeStamp.nanoSeconds = ((int)(timeStamp.seconds%1000))*1000000;
    }
}
