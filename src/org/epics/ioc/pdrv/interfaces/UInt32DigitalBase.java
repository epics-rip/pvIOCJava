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

import org.epics.ioc.pdrv.*;



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
public abstract class UInt32DigitalBase implements UInt32Digital {
    private Trace asynTrace;
    private String interfaceName;
    private Port port;
    private String portName;
    
    private  ReentrantLock lock = new ReentrantLock();
    private List<UserPvt> interruptlistenerList =
        new LinkedList<UserPvt>();
    private List<UserPvt> interruptlistenerListNew = null;
    private boolean interruptActive = true;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    private int zeroToOneMask = 0;
    private int oneToZeroMask = 0;
    private int transitionMask = 0;
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
     * @param interfaceName The interface.
     */
    protected UInt32DigitalBase(Device device,String interfaceName) {
        asynTrace = device.getTrace();
        port = device.getPort();
        portName = port.getPortName();
        device.registerInterface(this);
        this.interfaceName = interfaceName;
    }
    /**
     * Announce an interrupt.
     * @param data The new data.
     */
    protected void interruptOccured(int data) {
        prevValue = value;
        value = data;
        if(interruptActive) {
            asynTrace.print(Trace.FLOW ,
                    "%s new interrupt while interruptActive",
                    portName);
            return;
        }
        interrupt.interrupt(prevValue,value);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return interfaceName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#getInterruptMask(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.DigitalInterruptReason)
     */
    public Status getInterruptMask(User user, DigitalInterruptReason reason) {
        int value = 0;
        switch(reason) {
        case zeroToOne:  value = zeroToOneMask; break;
        case oneToZero:  value = oneToZeroMask; break;
        case transition: value = transitionMask; break;
        }
        user.setInt(value);
        asynTrace.print(Trace.FLOW,"%s getInterrupt",portName);
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#read(org.epics.ioc.pdrv.User, int)
     */
    public Status read(User user, int mask) {
        asynTrace.print(Trace.FLOW,"%s read",portName);
        user.setInt(value);
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#setInterruptMask(org.epics.ioc.pdrv.User, int, org.epics.ioc.pdrv.interfaces.DigitalInterruptReason)
     */
    public Status setInterruptMask(User user, int mask, DigitalInterruptReason reason) {
        switch(reason) {
        case zeroToOne:  zeroToOneMask = mask; break;
        case oneToZero:  oneToZeroMask = mask; break;
        case transition: transitionMask = mask; break;
        }
        asynTrace.print(Trace.FLOW,
            "%s setInterrupt",portName);
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32Digital#write(org.epics.ioc.pdrv.User, int, int)
     */
    public Status write(User user, int value, int mask) {
        asynTrace.print(Trace.DRIVER,"%s write",portName);
        int newValue = -1 & this.value & ~mask;
        newValue |= (value&mask);
        interruptOccured(newValue);
        return Status.success;
    }
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
                    asynTrace.print(Trace.FLOW ,
                            "%s echoDriver.addInterruptUser while interruptActive",
                            port.getPortName());
                    return Status.success;
                }
            } else if(interruptlistenerList.add(userPvt)) {
                asynTrace.print(Trace.FLOW ,"addInterruptUser");
                return Status.success;
            }
            asynTrace.print(Trace.ERROR,"%s addInterruptUser but already registered",portName);
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
                    asynTrace.print(Trace.FLOW ,"%s removeInterruptUser while interruptActive",portName);
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(userPvt)) {
                asynTrace.print(Trace.FLOW ,"%s removeInterruptUser",portName);
                return Status.success;
            }
            asynTrace.print(Trace.ERROR,"%s removeInterruptUser but not registered",portName);
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
            asynTrace.print(Trace.FLOW ,
                    "%s begin calling interruptListeners",portName);
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
            asynTrace.print(Trace.FLOW ,"%s end calling interruptListeners",portName);
        } finally {
            lock.unlock();
        }
    } 
    
    private class Interrupt implements Runnable {
        private Thread thread = new Thread(this);
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        private int prevValue;
        private int value;
        
        private Interrupt() {
            thread.start();
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
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                while(true) {
                    lock.lock();
                    try {
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
