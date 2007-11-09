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
import org.epics.ioc.pv.*;



/**
 * Abstract interface for Int32Array.
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
public abstract class AbstractInt32Array extends AbstractArrayInterface implements Int32Array{
    private  ReentrantLock lock = new ReentrantLock();
    private List<Int32ArrayInterruptListener> interruptlistenerList =
        new LinkedList<Int32ArrayInterruptListener>();
    private List<Int32ArrayInterruptListener> interruptlistenerListNew = null;
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
     */
    protected AbstractInt32Array(
        PVField parent,Array array,int capacity,boolean capacityMutable,Device device)
    {
        super(parent,array,capacity,capacityMutable,device,"int32Array");
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#endRead(org.epics.ioc.pdrv.User)
     */
    public Status endRead(User user){
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#endWrite(org.epics.ioc.pdrv.User)
     */
    public Status endWrite(User user) {
        interruptOccured();
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#startRead(org.epics.ioc.pdrv.User)
     */
    public Status startRead(User user) {
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#startWrite(org.epics.ioc.pdrv.User)
     */
    public Status startWrite(User user) {
        return Status.success;
    }
    /**
     * Announce an interrupt.
     */
    public void interruptOccured() {
        if(interruptActive) {
            super.print(Trace.FLOW ,"new interrupt while interruptActive");
            return;
        }
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener)
     */
    public Status addInterruptUser(User user,
            Int32ArrayInterruptListener int32ArrayInterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Int32ArrayInterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.add(int32ArrayInterruptListener)) {
                    super.print(Trace.FLOW ,"addInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.add(int32ArrayInterruptListener)) {
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
     * @see org.epics.ioc.pdrv.interfaces.Int32Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user,
            Int32ArrayInterruptListener int32ArrayInterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Int32ArrayInterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.remove(int32ArrayInterruptListener)) {
                    super.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(int32ArrayInterruptListener)) {
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
        ListIterator<Int32ArrayInterruptListener> iter = interruptlistenerList.listIterator();
        while(iter.hasNext()) {
            Int32ArrayInterruptListener listener = iter.next();
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
    
    private class Interrupt implements Runnable {
        private Thread thread = new Thread(this);
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        
        private Interrupt() {
            thread.start();
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
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                while(true) {
                    lock.lock();
                    try {
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
