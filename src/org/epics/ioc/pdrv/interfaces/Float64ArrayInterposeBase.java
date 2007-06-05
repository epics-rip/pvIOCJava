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
 * @param float64Array The interface to the lower level implementation.
 * @author mrk
 *
 */
public class Float64ArrayInterposeBase implements Float64Array {
    private Float64Array float64Array;
    
    /**
     * Constructor
     * @param float64Array The interface to the lower level implementation.
     */
    protected Float64ArrayInterposeBase(Float64Array float64Array) {
        this.float64Array = float64Array;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status addInterruptUser(User user, Float64ArrayInterruptListener float64ArrayListener) {
        return float64Array.addInterruptUser(user, float64ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#read(org.epics.ioc.pdrv.User, double[], int)
     */
    public Status read(User user, double[] value, int length) {
        return float64Array.read(user, value, length);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user, Float64ArrayInterruptListener float64ArrayListener) {
        return float64Array.removeInterruptUser(user, float64ArrayListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#write(org.epics.ioc.pdrv.User, double[], int)
     */
    public Status write(User user, double[] value, int length) {
        return float64Array.write(user, value, length);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return float64Array.getInterfaceName();
    }
}
