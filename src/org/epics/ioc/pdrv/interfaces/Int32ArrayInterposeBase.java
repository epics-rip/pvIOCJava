/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;
import org.epics.pvData.pv.PVIntArray;

/**
 * Base for interposing an Int32Array
 * @author mrk
 *
 */
public class Int32ArrayInterposeBase extends AbstractInt32Array {
    private Int32Array int32Array;
    
    /**
     * Constructor
     * @param pvIntArray data array.
     * @param device The device.
     * @param int32Array The lower level interface.
     */
    protected Int32ArrayInterposeBase(PVIntArray pvIntArray,Device device,Int32Array int32Array) {
        super(pvIntArray,device);
        this.int32Array = int32Array;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#endRead(org.epics.ioc.pdrv.User)
     */
    public Status endRead(User user) {
        return int32Array.endRead(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#endWrite(org.epics.ioc.pdrv.User)
     */
    public Status endWrite(User user) {
        return int32Array.endWrite(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#startRead(org.epics.ioc.pdrv.User)
     */
    public Status startRead(User user) {
        return int32Array.startRead(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#startWrite(org.epics.ioc.pdrv.User)
     */
    public Status startWrite(User user) {
        return int32Array.startWrite(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener)
     */
    public Status addInterruptUser(User user, Int32ArrayInterruptListener int32ArrayListener) {
        return int32Array.addInterruptUser(user, int32ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user, Int32ArrayInterruptListener int32ArrayListener) {
        return int32Array.removeInterruptUser(user, int32ArrayListener);
    }
}
