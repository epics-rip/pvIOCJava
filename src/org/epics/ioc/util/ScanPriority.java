/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * The scan priorities for a record instance that is event or periodically scanned.
 * @author mrk
 *
 */
public enum ScanPriority {
    /**
     * Lowest prority.
     */
    lowest,
    /**
     * Lower priority.
     */
    lower,
    /**
     * Low priority.
     */
    low,
    /**
     * Middle priority.
     */
    middle,
    /**
     * High priority. 
     */
    high,
    /**
     * Higher priority.
     */
    higher,
    /**
     * Highest priority.
     */
    highest;
    
    private static final int[] javaPriority = {
        Thread.MIN_PRIORITY,
        Thread.MIN_PRIORITY + 1,
        Thread.NORM_PRIORITY - 1,
        Thread.NORM_PRIORITY,
        Thread.MIN_PRIORITY + 1,
        Thread.MAX_PRIORITY - 1,
        Thread.MAX_PRIORITY};
    
    /**
     * Get the Java priority for this ScanPriority.
     * @return
     */
    public int getJavaPriority() {
        return javaPriority[ordinal()];
    }
}
