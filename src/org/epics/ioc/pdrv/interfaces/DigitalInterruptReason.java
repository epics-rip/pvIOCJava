/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

/**
 * Reason for an interrupt 
 * @author mrk
 *
 */
public enum DigitalInterruptReason {
    zeroToOne,
    oneToZero,
    transition
}
