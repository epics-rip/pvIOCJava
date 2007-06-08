/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pv.PVDoubleArray;

/**
 * Interface implemented by an Float64ArrayInterruptListener.
 * @author mrk
 *
 */
public interface Float64ArrayInterruptListener{
    /**
     * An interrupt has occured.
     * @param pvDoubleArray The array.
     */
    void interrupt(PVDoubleArray pvDoubleArray);
}
