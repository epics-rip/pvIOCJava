/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.interfaces;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvdata.misc.RunnableReady;
import org.epics.pvdata.misc.ThreadCreate;
import org.epics.pvdata.misc.ThreadCreateFactory;
import org.epics.pvdata.misc.ThreadReady;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.User;



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
public abstract class AbstractInt32 extends AbstractInterface implements Int32 {
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
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
     */
    protected AbstractInt32(Device device) {
    	super(device,"int32");
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32#interruptOccured(int)
     */
    public void interruptOccurred(int data) {
        if(interruptActive) {
            super.print(Trace.FLOW ,"new interrupt while interruptActive");
            return;
        }
        super.print(Trace.FLOW ,"interruptOccured");
        interruptData = data;
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32#getBounds(org.epics.pvioc.pdrv.User, int[])
     */
    public Status getBounds(User user, int[] bounds) {
        user.setMessage("bounds not available");
        return Status.error;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32#read(org.epics.pvioc.pdrv.User)
     */
    public abstract Status read(User user);
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32#write(org.epics.pvioc.pdrv.User, int)
     */
    public abstract Status write(User user, int value) ;
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32#addInterruptUser(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.interfaces.Int32InterruptListener)
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
                    super.print(Trace.FLOW ,
                            "addInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.add(int32InterruptListener)) {
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
     * @see org.epics.pvioc.pdrv.interfaces.Int32#removeInterruptUser(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.interfaces.Int32InterruptListener)
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
                    super.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(int32InterruptListener)) {
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
            name += AbstractInt32.this.getInterfaceName();
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
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
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
