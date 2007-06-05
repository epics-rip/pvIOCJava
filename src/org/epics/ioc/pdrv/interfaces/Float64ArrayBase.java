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
 * Base interface for Float64Array.
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
public abstract class Float64ArrayBase implements Float64Array {
    private Trace asynTrace;
    private String interfaceName;
    private Port port;
    private String portName;
    
    private  ReentrantLock lock = new ReentrantLock();
    private List<Float64ArrayInterruptListener> interruptlistenerList =
        new LinkedList<Float64ArrayInterruptListener>();
    private List<Float64ArrayInterruptListener> interruptlistenerListNew = null;
    private boolean interruptActive = true;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    private double[] interruptData = null;
    private int length = 0;
    
    /**
     * Constructor.
     * This registers the interface with the device.
     * @param device The device
     * @param interfaceName The interface.
     */
    protected Float64ArrayBase(Device device,String interfaceName) {
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
    protected void interruptOccured(double[] data,int length) {
        if(interruptActive) {
            asynTrace.print(Trace.FLOW ,
                    "%s new interrupt while interruptActive",
                    portName);
            return;
        }
        interruptData = data;
        this.length = length;
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#read(org.epics.ioc.pdrv.User, double[], int)
     */
    public abstract Status read(User user, double[] value, int length);
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#write(org.epics.ioc.pdrv.User, double[], int)
     */
    public abstract Status write(User user, double[] value, int length);
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.Interface#getInterfaceName()
     */
    public String getInterfaceName() {
        return interfaceName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status addInterruptUser(User user,
            Float64ArrayInterruptListener float64InterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Float64ArrayInterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.add(float64InterruptListener)) {
                    asynTrace.print(Trace.FLOW ,
                            "%s echoDriver.addInterruptUser while interruptActive",
                            portName);
                    return Status.success;
                }
            } else if(interruptlistenerList.add(float64InterruptListener)) {
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
     * @see org.epics.ioc.pdrv.interfaces.Float64Array#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener)
     */
    public Status removeInterruptUser(User user, Float64ArrayInterruptListener float64InterruptListener) {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Float64ArrayInterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.remove(float64InterruptListener)) {
                    asynTrace.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(float64InterruptListener)) {
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
        ListIterator<Float64ArrayInterruptListener> iter = interruptlistenerList.listIterator();
        while(iter.hasNext()) {
            Float64ArrayInterruptListener listener = iter.next();
            listener.interrupt(interruptData,length);
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
