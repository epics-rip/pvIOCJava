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
 * Base for interposing a Float64Array.
 * @author mrk
 *
 */
public class Float64ArrayInterposeBase extends AbstractPVArray implements Float64Array {
    private Float64Array float64Array;
    
    /**
     * Constructor
     * @param arg The interface to the lower level implementation.
     */
    protected Float64ArrayInterposeBase(Float64Array arg) {
        super(arg.getParent(),arg.getArray(),arg.getCapacity(),arg.isCapacityMutable());
        this.float64Array = arg;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#endRead(org.epics.ioc.pdrv.User)
     */
    public Status endRead(User user) {
        return float64Array.endRead(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#endWrite(org.epics.ioc.pdrv.User)
     */
    public Status endWrite(User user) {
        return float64Array.endWrite(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#startRead(org.epics.ioc.pdrv.User)
     */
    public Status startRead(User user) {
        return float64Array.startRead(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#startWrite(org.epics.ioc.pdrv.User)
     */
    public Status startWrite(User user) {
        return float64Array.startWrite(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status addInterruptUser(User user, Float64ArrayInterruptListener float64ArrayListener) {
        return float64Array.addInterruptUser(user, float64ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user, Float64ArrayInterruptListener float64ArrayListener) {
        return float64Array.removeInterruptUser(user, float64ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return float64Array.getInterfaceName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVDoubleArray#get(int, int, org.epics.ioc.pv.DoubleArrayData)
     */
    public int get(int offset, int len, DoubleArrayData data) {
        return float64Array.get(offset, len, data);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVDoubleArray#put(int, int, double[], int)
     */
    public int put(int offset, int len, double[] from, int fromOffset) {
        return float64Array.put(offset, len, from, fromOffset);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVArray#setCapacity(int)
     */
    public void setCapacity(int capacity) {
        float64Array.setCapacity(capacity);
    }
}
