/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.interfaces;

/**
 * Interface implemented by an Int32InterruptListener.
 * @author mrk
 *
 */
public interface UInt32DigitalInterruptListener {
    /**
     * An interrupt has been detected.
     * @param value The new value.
     * The value is treated as an unsigned 32 bit integer.
     */
    void interrupt(int value);
}
