/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.interfaces;


/**
 * Interface implemented by an Float64ArrayInterruptListener.
 * @author mrk
 *
 */
public interface Float64ArrayInterruptListener{
    /**
     * An interrupt has been detected.
     * @param float64Array The array.
     */
    void interrupt(Float64Array float64Array);
}
