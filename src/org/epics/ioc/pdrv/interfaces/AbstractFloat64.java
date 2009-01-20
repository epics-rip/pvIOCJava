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
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.pvData.misc.RunnableReady;
import org.epics.pvData.misc.ThreadCreate;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadReady;




/**
 * Base interface for Float64.
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
public abstract class AbstractFloat64 extends AbstractInterface implements Float64 {
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private Port port;
    private String portName;
    
    private  ReentrantLock lock = new ReentrantLock();
    private List<Float64InterruptListener> interruptlistenerList =
        new LinkedList<Float64InterruptListener>();
    private List<Float64InterruptListener> interruptlistenerListNew = null;
    private boolean interruptActive = false;
    private boolean interruptListenerListModified = false;
    private Interrupt interrupt = new Interrupt();
    private double interruptData = 0;
    
    /**
     * Constructor.
     * This registers the interface with the device.
     * @param device The device
     */
    protected AbstractFloat64(Device device) {
    	super(device,"float64");
        port = device.getPort();
        portName = port.getPortName();
    }
    public double[] getDisplayLimits(User user) {
		return null;
	}
	public String getUnits(User user) {
		return null;
	}
	/**
     * Announce an interrupt.
     * @param data The new data.
     */
    protected void interruptOccured(double data) {
        if(interruptActive) {
            super.print(Trace.FLOW ,
                    "new interrupt while interruptActive");
            return;
        }
        interruptData = data;
        interrupt.interrupt();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#read(org.epics.ioc.pdrv.User)
     */
    public abstract Status read(User user);
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#write(org.epics.ioc.pdrv.User, int)
     */
    public abstract Status write(User user, double value) ;
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64#addInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64InterruptListener)
     */
    public Status addInterruptUser(User user,
            Float64InterruptListener float64InterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Float64InterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.add(float64InterruptListener)) {
                    super.print(Trace.FLOW ,
                            "%s addInterruptUser while interruptActive",
                            portName);
                    return Status.success;
                }
            } else if(interruptlistenerList.add(float64InterruptListener)) {
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
     * @see org.epics.ioc.pdrv.interfaces.Float64#removeInterruptUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.interfaces.Float64InterruptListener)
     */
    public Status removeInterruptUser(User user,
            Float64InterruptListener float64InterruptListener)
    {
        lock.lock();
        try {
            if(interruptActive) {
                interruptListenerListModified = true;
                if(interruptlistenerListNew==null) {
                    interruptlistenerListNew = 
                        new LinkedList<Float64InterruptListener>(interruptlistenerList);
                }
                if(interruptlistenerListNew.remove(float64InterruptListener)) {
                    super.print(Trace.FLOW ,"removeInterruptUser while interruptActive");
                    return Status.success;
                }
            } else if(interruptlistenerList.remove(float64InterruptListener)) {
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
            super.print(Trace.FLOW ,
                "%s begin calling interruptListeners",portName);
        } finally {
            lock.unlock();
        }
        ListIterator<Float64InterruptListener> iter = interruptlistenerList.listIterator();
        while(iter.hasNext()) {
            Float64InterruptListener listener = iter.next();
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
            super.print(Trace.FLOW ,
                "%s end calling interruptListeners",portName);
        } finally {
            lock.unlock();
        }
    } 
    
    private class Interrupt implements RunnableReady {
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        
        private Interrupt() {
            String name = device.getPort().getPortName() + "[" + device.getDeviceName() + "]";
            name += AbstractFloat64.this.getInterfaceName();
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
