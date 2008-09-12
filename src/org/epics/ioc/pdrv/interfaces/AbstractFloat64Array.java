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
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.RunnableReady;
import org.epics.ioc.util.ThreadCreate;
import org.epics.ioc.util.ThreadCreateFactory;
import org.epics.ioc.util.ThreadReady;



/**
 * Abstract interface for Float64Array.
 * It provides the following features:
 * <ul>
 *    <li>registers the interface</li>
 *    <li>Provides a default implementation of all methods.<br />
 *        In particular it calls interrupt listeners.</li>
 *    <li>Provides method interruptOccured, which derived classes should call
 *     whenever data is written. It calls the interrupt listeners via a separate thread
 *     and with no locks held.
 *     </li>
 *</ul>
 *
 * @author mrk
 *
 */
public abstract class AbstractFloat64Array extends AbstractArrayInterface implements Float64Array{
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private  ReentrantLock lock = new ReentrantLock();
    private List<Float64ArrayInterruptListener> interruptlistenerList =
        new LinkedList<Float64ArrayInterruptListener>();
    private List<Float64ArrayInterruptListener> interruptlistenerListNew = null;
    private boolean interruptActive = false;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    /**
     * Constructor.
     * This registers the interface with the device.
     * @param parent The parent for the array.
     * @param array The Array interface.
     * @param capacity The capacity for the array.
     * @param capacityMutable Can the capacity be changed.
     * @param device The device.
     * @param interfaceName The interface name.
     */
    protected AbstractFloat64Array(
            PVField parent,Array array,int capacity,boolean capacityMutable,
            Device device)
    {
        super(parent,array,capacity,capacityMutable,device,"float64Array");
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVDoubleArray#share(org.epics.ioc.pv.PVDoubleArray)
     */
    public boolean share(double[] value, int length) {
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#endRead(org.epics.ioc.pdrv.User)
     */
    public Status endRead(User user) {
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#endWrite(org.epics.ioc.pdrv.User)
     */
    public Status endWrite(User user) {
        interruptOccured();
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#startRead(org.epics.ioc.pdrv.User)
     */
    public Status startRead(User user) {
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#startWrite(org.epics.ioc.pdrv.User)
     */
    public Status startWrite(User user) {
        return Status.success;
    }
    /**
     * Announce an interrupt.
     */
    public void interruptOccured() {
        if(interruptActive) {
            super.print(Trace.FLOW ,
                    "new interrupt while interruptActive");
            return;
        }
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status addInterruptUser(User user,
        Float64ArrayInterruptListener float64ArrayInterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Float64ArrayInterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.add(float64ArrayInterruptListener)) {
                    super.print(Trace.FLOW ,
                            "addInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.add(float64ArrayInterruptListener)) {
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
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user,
        Float64ArrayInterruptListener float64ArrayInterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Float64ArrayInterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.remove(float64ArrayInterruptListener)) {
                    super.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(float64ArrayInterruptListener)) {
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
    
    private void callback() {
        lock.lock();
        try {
            interruptActive = true;
            super.print(Trace.FLOW ,"begin calling interruptListeners");
        } finally {
            lock.unlock();
        }
        ListIterator<Float64ArrayInterruptListener> iter = interruptlistenerList.listIterator();
        while(iter.hasNext()) {
            Float64ArrayInterruptListener listener = iter.next();
            listener.interrupt(this);
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
        
        private Interrupt() {
            String name = device.getPort().getPortName() + "[" + device.getDeviceName() + "]";
            name += AbstractFloat64Array.this.getInterfaceName();
            threadCreate.create(name,4, this);
        }
        
        private void interrupt() {
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
                        callback();
                    }finally {
                        lock.unlock();
                    }
                }
            } catch(InterruptedException e) {
                
            }
        }
    }
}
