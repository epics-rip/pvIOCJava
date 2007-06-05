/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 * Base class for int32 interpose implementations.
 * It implements all int32 methods by calling the lower level int32 methods..
 * Thus an interpose implementation only needs to implement methods it wants to modify.
 * @author mrk
 *
 */
public class Int32InterposeBase implements Int32 {
    private Int32 int32;
    
    /**
     * Constructor
     * @param int32 The interface to the lower level implementation.
     */
    protected Int32InterposeBase(Int32 int32) {
        this.int32 = int32;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32InterruptListener)
     */
    public Status addInterruptUser(User user, Int32InterruptListener int32InterruptListener) {
        return int32.addInterruptUser(user, int32InterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#getBounds(org.epics.ioc.pdrv.User, int[])
     */
    public Status getBounds(User user, int[] bounds) {
        return int32.getBounds(user, bounds);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#read(org.epics.ioc.pdrv.User)
     */
    public Status read(User user) {
        return int32.read(user);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32InterruptListener)
     */
    public Status removeInterruptUser(User user, Int32InterruptListener int32InterruptListener) {
        return int32.removeInterruptUser(user, int32InterruptListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#write(org.epics.ioc.pdrv.User, int)
     */
    public Status write(User user, int value) {
        return int32.write(user, value);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return int32.getInterfaceName();
    }
}
