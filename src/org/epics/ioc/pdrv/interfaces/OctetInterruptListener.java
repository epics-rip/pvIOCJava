/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

/**
 * Interface implemented by an OctetInterruptListener.
 * @author mrk
 *
 */
public interface OctetInterruptListener {
    /**
     * An interrupt has been detected.
     * @param data The data array.
     * @param nbytes The number of bytes in the array.
     */
    void interrupt(byte[] data,int nbytes);
}
