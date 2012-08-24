/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
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
import org.epics.pvdata.pv.PVIntArray;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.User;



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
public abstract class AbstractInt32Array extends AbstractInterface implements Int32Array{
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private PVIntArray pvIntArray;
    private  ReentrantLock lock = new ReentrantLock();
    private List<Int32ArrayInterruptListener> interruptlistenerList =
        new LinkedList<Int32ArrayInterruptListener>();
    private List<Int32ArrayInterruptListener> interruptlistenerListNew = null;
    private boolean interruptActive = false;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    /**
     * Constructor.
     * @param pvIntArray the data.
     * @param device The device.
     */
    protected AbstractInt32Array(PVIntArray pvIntArray,Device device)
    {
        super(device,"int32Array");
        this.pvIntArray = pvIntArray;
    }    
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#getPVIntArray()
     */
    public PVIntArray getPVIntArray() {
        return pvIntArray;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#endRead(org.epics.pvioc.pdrv.User)
     */
    public Status endRead(User user){
        super.print(Trace.FLOW ,"endRead");
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#endWrite(org.epics.pvioc.pdrv.User)
     */
    public Status endWrite(User user) {
        super.print(Trace.FLOW ,"endWrite");
        interruptOccurred();
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#startRead(org.epics.pvioc.pdrv.User)
     */
    public Status startRead(User user) {
        super.print(Trace.FLOW ,"startRead");
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#startWrite(org.epics.pvioc.pdrv.User)
     */
    public Status startWrite(User user) {
        super.print(Trace.FLOW ,"startWrite");
        return Status.success;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#interruptOccured()
     */
    public void interruptOccurred() {
        if(interruptActive) {
            super.print(Trace.FLOW ,"new interrupt while interruptActive");
            return;
        }
        super.print(Trace.FLOW ,"interruptOccured");
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#addInterruptUser(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.interfaces.Int32ArrayInterruptListener)
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
     * @see org.epics.pvioc.pdrv.interfaces.Int32Array#removeInterruptUser(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.interfaces.Int32ArrayInterruptListener)
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
    
    private class Interrupt implements RunnableReady {
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        
        private Interrupt() {
            String name = device.getPort().getPortName() + "[" + device.getDeviceName() + "]";
            name += AbstractInt32Array.this.getInterfaceName();
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
