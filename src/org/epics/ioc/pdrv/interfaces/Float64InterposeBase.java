/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 * Base class for float64 interpose implementations.
 * It implements all float64 methods by calling the lower level float64 methods..
 * Thus an interpose implementation only needs to implement methods it wants to modify.
 * @author mrk
 *
 */
public class Float64InterposeBase extends AbstractInterface implements Float64 {
    private Float64 float64;
    
    /**
     * Constructor
     * @param float64 The interface to the lower level implementation.
     */
    protected Float64InterposeBase(Float64 float64) {
    	super(float64.getDevice(),float64.getInterfaceName());
        this.float64 = float64;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#getDisplayLimits()
     */
    public double[] getDisplayLimits(User user) {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.pdrv.interfaces.Float64#getUnits()
	 */
	public String getUnits(User user) {
		return null;
	}
	/* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64InterruptListener)
     */
    public Status addInterruptUser(User user, Float64InterruptListener float64InterruptListener) {
        return float64.addInterruptUser(user, float64InterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#read(org.epics.ioc.pdrv.User)
     */
    public Status read(User user) {
        return float64.read(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64InterruptListener)
     */
    public Status removeInterruptUser(User user, Float64InterruptListener float64InterruptListener) {
        return float64.removeInterruptUser(user, float64InterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#write(org.epics.ioc.pdrv.User, int)
     */
    public Status write(User user, double value) {
        return float64.write(user, value);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return float64.getInterfaceName();
    }
}
