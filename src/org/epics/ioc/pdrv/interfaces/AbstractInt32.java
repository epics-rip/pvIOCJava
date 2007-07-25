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
 * Base interface for Int32.
 * It provides the following features:
 * <ul>
 *    <li>registers the interface</li>
 *    <li>Provides a default implementation of all methods.<br />
 *        In particular it calls interrupt listeners.
 *    </li>Provides method interruptOccured, which derived classes should call
 *     whenever data is written. It calls the interrupt listeners via a separate thread
 *     and with no locks held.
 *     </li>
 *</ul>
 *
 * @author mrk
 *
 */
public abstract class AbstractInt32 implements Int32 {
    private Trace asynTrace;
    private String interfaceName;
    private Port port;
    private String portName;
    
    private  ReentrantLock lock = new ReentrantLock();
    private List<Int32InterruptListener> interruptlistenerList =
        new LinkedList<Int32InterruptListener>();
    private List<Int32InterruptListener> interruptlistenerListNew = null;
    private boolean interruptActive = false;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    private int interruptData = 0;
    
    /**
     * Constructor.
     * This registers the interface with the device.
     * @param device The device
     * @param interfaceName The interface.
     */
    protected AbstractInt32(Device device,String interfaceName) {
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
        if(interruptActive) {
            asynTrace.print(Trace.FLOW ,
                    "%s new interrupt while interruptActive",
                    portName);
            return;
        }
        interruptData = data;
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return interfaceName;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#getBounds(org.epics.ioc.pdrv.User, int[])
     */
    public Status getBounds(User user, int[] bounds) {
        user.setMessage("bounds not available");
        return Status.error;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#read(org.epics.ioc.pdrv.User)
     */
    public abstract Status read(User user);
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#write(org.epics.ioc.pdrv.User, int)
     */
    public abstract Status write(User user, int value) ;
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32InterruptListener)
     */
    public Status addInterruptUser(User user,
            Int32InterruptListener int32InterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Int32InterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.add(int32InterruptListener)) {
                    asynTrace.print(Trace.FLOW ,
                            "%s addInterruptUser while interruptActive",
                            portName);
                    return Status.success;
                }
            } else if(interruptlistenerList.add(int32InterruptListener)) {
                asynTrace.print(Trace.FLOW ,"addInterruptUser");
                return Status.success;
            }
            asynTrace.print(Trace.ERROR,"addInterruptUser but already registered");
            user.setMessage("add failed");
            return Status.error;
        } finally {
            lock.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Int32InterruptListener)
     */
    public Status removeInterruptUser(User user, Int32InterruptListener int32InterruptListener) {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Int32InterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.remove(int32InterruptListener)) {
                    asynTrace.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(int32InterruptListener)) {
                asynTrace.print(Trace.FLOW ,"removeInterruptUser");
                return Status.success;
            }
            asynTrace.print(Trace.ERROR,"removeInterruptUser but not registered");
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
            asynTrace.print(Trace.FLOW ,
                    "%s begin calling interruptListeners",portName);
        } finally {
            lock.unlock();
        }
        ListIterator<Int32InterruptListener> iter = interruptlistenerList.listIterator();
        while(iter.hasNext()) {
            Int32InterruptListener listener = iter.next();
            listener.interrupt(interruptData);
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
