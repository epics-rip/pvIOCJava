/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;
import org.epics.pvData.pv.PVDoubleArray;

/**
 * Base for interposing a Float64Array.
 * @author mrk
 *
 */
public class Float64ArrayInterposeBase extends AbstractFloat64Array {
    private Float64Array float64Array;
    
    /**
     * Constructor
     * @param pvIntArray data array.
     * @param device The device.
     * @param float64Array The lower level interface.
     */
    protected Float64ArrayInterposeBase(PVDoubleArray pvDoubleArray,Device device,Float64Array float64Array) {
        super(pvDoubleArray,device);
        this.float64Array = float64Array;
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
}
