/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.util.RunnableReady;
import org.epics.ioc.util.ThreadCreate;
import org.epics.ioc.util.ThreadCreateFactory;
import org.epics.ioc.util.ThreadReady;



/**
 * Base interface for UInt32Digital.
 * It provides the following features:
 * <ul>
 *    <li>registers the interface</li>
 *    <li>Provides a default implementation of all methods</li>
 *    <li>Provides method interruptOccured, which derived classes should call
 *     whenever the digital value changes . It calls the interrupt listeners via a separate thread
 *     and with no locks held.
 *     </li>
 *</ul>
 *
 * @author mrk
 *
 */
public abstract class AbstractUInt32Digital extends AbstractInterface  implements UInt32Digital {
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private  ReentrantLock lock = new ReentrantLock();
    private List<UserPvt> interruptlistenerList =
        new LinkedList<UserPvt>();
    private List<UserPvt> interruptlistenerListNew = null;
    private boolean interruptActive = true;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    private int value = 0;
    private int prevValue = 0;
    
    private static class UserPvt {
        int mask;
        User user;
        UInt32DigitalInterruptListener listener;
    }
    
    /**
     * Constructor.
     * This registers the interface with the device.
     * @param device The device
     */
    protected AbstractUInt32Digital(Device device) {
    	super(device,"uint32Digital");
    }
    public String[] getChoices(User user) {
		return null;
	}
	/**
     * Announce an interrupt.
     * @param data The new data.
     * @param reason The reason for the interrupt.
     */
    protected void interruptOccured(int data) {
        prevValue = value;
        value = data;
        if(interruptActive) {
            super.print(Trace.FLOW ,"new interrupt while interruptActive");
            return;
        }
        interrupt.interrupt(prevValue,value);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#getInterrupt(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.DigitalInterruptReason)
     */
    public Status getInterrupt(User user, int mask, DigitalInterruptReason reason) {
        super.print(Trace.FLOW,"getInterrupt");
        user.setMessage("getInterrupt not supported");
        return Status.error;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#setInterrupt(org.epics.ioc.pdrv.User, int, org.epics.ioc.pdrv.interfaces.DigitalInterruptReason)
     */
    public Status setInterrupt(User user, int mask, DigitalInterruptReason reason) {
        super.print(Trace.FLOW,"setInterrupt");
        user.setMessage("setInterrupt not supported");
        return Status.error;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#clearInterrupt(org.epics.ioc.pdrv.User, int)
     */
    public Status clearInterrupt(User user, int mask) {
        super.print(Trace.FLOW,"clearInterrupt");
            user.setMessage("clearInterrupt not supported");
            return Status.error;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#read(org.epics.ioc.pdrv.User, int)
     */
    abstract public Status read(User user, int mask);
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#write(org.epics.ioc.pdrv.User, int, int)
     */
    abstract public Status write(User user, int value, int mask);
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener, int)
     */
    public Status addInterruptUser(User user,
            UInt32DigitalInterruptListener uint32DigitalInterruptListener,int mask)
    {
        lock.lock();
        try {
            UserPvt userPvt = new UserPvt();
            userPvt.mask = mask;
            userPvt.user = user;
            userPvt.listener = uint32DigitalInterruptListener;
            user.setDeviceDriverPvt(userPvt);
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<UserPvt>(interruptlistenerList);
                }
                if(interruptlistenerListNew.add(userPvt)) {
                    super.print(Trace.FLOW ,"addInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.add(userPvt)) {
                super.print(Trace.FLOW ,"addInterruptUser");
                return Status.success;
            }
            super.print(Trace.ERROR,"addInterruptUser but already registered");
            user.setMessage("add failed");
            return Status.error;
        } finally {
            lock.unlock();
        }
    }

    public Status removeInterruptUser(User user,
            UInt32DigitalInterruptListener uint32DigitalInterruptListener)
    {
        lock.lock();
        try {
            UserPvt userPvt = (UserPvt)user.getDeviceDriverPvt();
            user.setPortDriverPvt(null);
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<UserPvt>(interruptlistenerList);
                }
                if(interruptlistenerListNew.remove(userPvt)) {
                    super.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(userPvt)) {
                super.print(Trace.FLOW ,"removeInterruptUser");
                return Status.success;
            }
            super.print(Trace.ERROR,"removeInterruptUser but not registered");
            user.setMessage("remove failed");
            return Status.error;
        } finally {
            lock.unlock();
        }
    }
    
    private void callback(int prevValue,int value) {
        lock.lock();
        try {
            interruptActive = true;
            super.print(Trace.FLOW ,"begin calling interruptListeners");
        } finally {
            lock.unlock();
        }
        ListIterator<UserPvt> iter = interruptlistenerList.listIterator();
        while(iter.hasNext()) {
            UserPvt userPvt = iter.next();
            UInt32DigitalInterruptListener listener = userPvt.listener;
            int change = value^prevValue;
            if((userPvt.mask&change) != 0) listener.interrupt(value);
        }
        lock.lock();
        try {
            if(interruptListenerListModified) {
                interruptlistenerList = interruptlistenerListNew;
                interruptlistenerListNew = null;
                interruptListenerListModified = false;
            }
            interruptActive = false;
            super.print(Trace.FLOW ,"end calling interruptListeners");
        } finally {
            lock.unlock();
        }
    } 
    
    private class Interrupt implements RunnableReady {
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        private int prevValue;
        private int value;
        
        private Interrupt() {
            String name = device.getPort().getPortName() + "[" + device.getDeviceName() + "]";
            name += AbstractUInt32Digital.this.getInterfaceName();
            threadCreate.create(name,4, this);
        }
        
        private void interrupt(int prevValue,int value) {
            lock.lock();
            try {
                moreWork.signal();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            boolean firstTime = true;
            try {
                while(true) {
                    lock.lock();
                    try {
                        if(firstTime) {
                            firstTime = false;
                            threadReady.ready();
                        }
                        moreWork.await();
                        callback(prevValue,value);
                    }finally {
                        lock.unlock();
                    }
                }
            } catch(InterruptedException e) {
                
            }
        }
    }
}
