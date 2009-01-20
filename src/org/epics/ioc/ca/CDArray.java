/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.PVArray;


/**
 * Interface for an array field.
 * @author mrk
 *
 */
public interface CDArray extends CDField{
    /**
     * Get the PVArray for this CDArray
     * @return The PVArray.
     */
    PVArray getPVArray();
}
