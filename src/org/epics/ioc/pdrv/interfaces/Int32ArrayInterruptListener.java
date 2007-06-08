/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pv.PVIntArray;

/**
 * Interface implemented by an Int32ArrayInterruptListener.
 * @author mrk
 *
 */
public interface Int32ArrayInterruptListener{
    /**
     * An interrupt has occured.
     * @param pvIntArray The array.
     */
    void interrupt(PVIntArray pvIntArray);
}
