/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.*;

/**
 * Base class for uint32Digital interpose implementations.
 * It implements all uint32Digital methods by calling the lower level uint32Digital methods..
 * Thus an interpose implementation only needs to implement methods it wants to modify.
 * @author mrk
 *
 */
public abstract class UInt32DigitalInterposeBase implements UInt32Digital {
    UInt32Digital uint32Digital;

    /**
     * The constructor
     * @param uint32Digital The interface to the lower level implementation.
     */
    protected UInt32DigitalInterposeBase(UInt32Digital uint32Digital) {
        this.uint32Digital = uint32Digital;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener, int)
     */
    public Status addInterruptUser(User user, UInt32DigitalInterruptListener uint32DigitalInterruptListener, int mask) {
        return uint32Digital.addInterruptUser(user, uint32DigitalInterruptListener, mask);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#getInterrupt(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.DigitalInterruptReason)
     */
    public Status getInterruptMask(User user, DigitalInterruptReason reason) {
        return uint32Digital.getInterruptMask(user, reason);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#read(org.epics.ioc.pdrv.User, int)
     */
    public Status read(User user, int mask) {
        return uint32Digital.read(user, mask);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener)
     */
    public Status removeInterruptUser(User user, UInt32DigitalInterruptListener uint32DigitalInterruptListener) {
        return uint32Digital.removeInterruptUser(user, uint32DigitalInterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#setInterrupt(org.epics.ioc.pdrv.User, int, org.epics.ioc.pdrv.interfaces.DigitalInterruptReason)
     */
    public Status setInterruptMask(User user, int value, DigitalInterruptReason reason) {
        return uint32Digital.setInterruptMask(user, value, reason);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#write(org.epics.ioc.pdrv.User, int, int)
     */
    public Status write(User user, int value, int mask) {
        return uint32Digital.write(user, value, mask);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return uint32Digital.getInterfaceName();
    }
}
