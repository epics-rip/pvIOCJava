/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 *  Constructor
 * @param int32Array The interface to the lower level implementation.
 * @author mrk
 *
 */
public class Int32ArrayInterposeBase implements Int32Array {
    private Int32Array int32Array;
    
    /**
     * Constructor
     * @param int32Array The interface to the lower level implementation.
     */
    protected Int32ArrayInterposeBase(Int32Array int32Array) {
        this.int32Array = int32Array;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener)
     */
    public Status addInterruptUser(User user, Int32ArrayInterruptListener int32ArrayListener) {
        return int32Array.addInterruptUser(user, int32ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#read(org.epics.ioc.pdrv.User, int[], int)
     */
    public Status read(User user, int[] value, int length) {
        return int32Array.read(user, value, length);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user, Int32ArrayInterruptListener int32ArrayListener) {
        return int32Array.removeInterruptUser(user, int32ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#write(org.epics.ioc.pdrv.User, int[], int)
     */
    public Status write(User user, int[] value, int length) {
        return int32Array.write(user, value, length);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return int32Array.getInterfaceName();
    }
}
