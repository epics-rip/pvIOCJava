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
public enum ScanPriority {
    lower,
    low,
    medium,
    high,
    higher;
    
    private static final int[] javaPriority = {
        Thread.MIN_PRIORITY,
        Thread.MIN_PRIORITY+2,
        Thread.NORM_PRIORITY,
        Thread.MAX_PRIORITY-2,
        Thread.MAX_PRIORITY};
    
    public int getJavaPriority() {
        return javaPriority[ordinal()];
    }
}
