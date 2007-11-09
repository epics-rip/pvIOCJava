/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

/**
 * Interface implemented by an Int32InterruptListener.
 * @author mrk
 *
 */
public interface Float64InterruptListener {
    /**
     * An interrupt has been detected.
     * @param value The new value.
     */
    void interrupt(double value);
}
