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
public interface Int32InterruptListener {
    /**
     * An interrupt has been detected.
     * @param value The new value.
     */
    void interrupt(int value);
}
