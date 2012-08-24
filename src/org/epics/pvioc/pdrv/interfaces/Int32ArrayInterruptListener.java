/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.interfaces;


/**
 * Interface implemented by an Int32ArrayInterruptListener.
 * @author mrk
 *
 */
public interface Int32ArrayInterruptListener{
    /**
     * An interrupt has been detected.
     * @param int32Array The array.
     */
    void interrupt(Int32Array int32Array);
}
