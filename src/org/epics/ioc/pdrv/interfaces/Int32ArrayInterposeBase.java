/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pv.*;

/**
 * Base for interposing a Int32Array.
 * @param int32Array The interface to the lower level implementation.
 * @author mrk
 *
 */
public class Int32ArrayInterposeBase extends AbstractArrayInterface implements Int32Array {
    private Int32Array int32Array;
    
    /**
     * Constructor
     * @param arg The interface to the lower level implementation.
     */
    protected Int32ArrayInterposeBase(Int32Array arg) {
        super(arg.getParent(),arg.getArray(),arg.getCapacity(),
        		arg.isCapacityMutable(),
        		arg.getDevice(),arg.getInterfaceName());
        this.int32Array = arg;
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
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return int32Array.getInterfaceName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVIntArray#get(int, int, org.epics.ioc.pv.IntArrayData)
     */
    public int get(int offset, int len, IntArrayData data) {
        return int32Array.get(offset, len, data);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVIntArray#put(int, int, double[], int)
     */
    public int put(int offset, int len, int[] from, int fromOffset) {
        return int32Array.put(offset, len, from, fromOffset);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVArray#setCapacity(int)
     */
    public void setCapacity(int capacity) {
        int32Array.setCapacity(capacity);
    }
}
