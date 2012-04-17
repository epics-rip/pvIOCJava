/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.interfaces;

/**
 * Reason for an interrupt 
 * @author mrk
 *
 */
public enum DigitalInterruptReason {
    /**
     * Interrupt on zero to one transition.
     */
    zeroToOne,
    /**
     * Interrupt on one to zero transition
     */
    oneToZero,
    /**
     * Interrupt on any bit transition
     */
    transition
}
