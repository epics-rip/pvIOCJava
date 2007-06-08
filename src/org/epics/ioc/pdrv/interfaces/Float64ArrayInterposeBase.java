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
 *  Constructor
 * @param float64Array The interface to the lower level implementation.
 * @author mrk
 *
 */
public class Float64ArrayInterposeBase extends AbstractPVArray implements Float64Array {
    private Float64Array float64Array;
    
    /**
     * Constructor
     * @param float64Array The interface to the lower level implementation.
     */
    protected Float64ArrayInterposeBase(Float64Array float64Array) {
        super(float64Array.getParent(),(Array)float64Array.getField(),
            float64Array.getCapacity(),float64Array.isCapacityMutable());
        this.float64Array = float64Array;
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
}
