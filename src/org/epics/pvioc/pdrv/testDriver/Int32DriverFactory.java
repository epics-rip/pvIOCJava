/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.testDriver;

import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.DeviceDriver;
import org.epics.pvioc.pdrv.Factory;
import org.epics.pvioc.pdrv.Port;
import org.epics.pvioc.pdrv.PortDriver;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.User;
import org.epics.pvioc.pdrv.interfaces.AbstractInt32;
/**
 * The factory for int32Driver.
 * int32Driver is a portDriver for testing the int32 support in org.epics.pvioc.pdrv.support.
 * It requires the int32Driver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>numberRegisters<br/>
 *       The number of int32 registers to simulate.
 *     </li>
 *     <li>low<br/>
 *       The low adc range to simulate.
 *      </li>
 *      <li>high<br/>
 *       The high adc range to simulate.
 *      </li>
 *      <li>delay<br/>
 *       If 0.0 then int32Driver is synchronous.
 *       If > 0.0 int32Driver is asynchronous and delays delay seconds after each read/write request.
 *      </li>
 * </ul>
 * int32Driver implements interface int32 by keeping an internal int[] array
 * that simulates adc registers.
 * A write request sets a value and a read request reads the current value.
 * @author mrk
 *
 */
public class Int32DriverFactory {
    
    /**
     * Create a new instance of int32Driver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure int32Driver.
     */
    static public void create(
        String portName,boolean autoConnect,ThreadPriority priority,PVStructure pvStructure)
    {
        PVField[] pvFields = pvStructure.getPVFields();
        Structure structure = (Structure)pvStructure.getField();
        int index = structure.getFieldIndex("numberRegisters");
        if(index<0) {
            throw new IllegalStateException("field numberRegisters not found");
        }
        PVInt pvInt = (PVInt)pvFields[index];
        int numberRegisters = pvInt.get();
        index = structure.getFieldIndex("low");
        if(index<0) {
            throw new IllegalStateException("field low not found");
        }
        pvInt = (PVInt)pvFields[index];
        int low = pvInt.get();
        index = structure.getFieldIndex("high");
        if(index<0) {
            throw new IllegalStateException("field high not found");
        }
        pvInt = (PVInt)pvFields[index];
        int high = pvInt.get();
        index = structure.getFieldIndex("delay");
        if(index<0) {
            throw new IllegalStateException("field delay not found");
        }
        PVDouble pvDelay = (PVDouble)pvFields[index];
        double delay = pvDelay.get();
        boolean canBlock = ((delay>0.0) ? true : false);
        new Int32Driver(portName,autoConnect,priority,numberRegisters,low,high,canBlock,delay);
    }
    
    static private class Int32Driver implements PortDriver {
        private int[] register;
        private int low;
        private int high;
        private double delay;
        private long milliseconds;
        private Port port;
        private Trace trace;
        
        private Int32Driver(String portName,boolean autoConnect,ThreadPriority priority,
            int numberRegisters,int low,int high,boolean canBlock,double delay)
        {
            register = new int[numberRegisters];
            this.low = low;
            this.high = high;
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            port = Factory.createPort(portName, this, "int32Driver",
                canBlock, autoConnect,priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "delay " + delay + " low " + low + " high " + high;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#connect(org.epics.pvioc.pdrv.User)
         */
        public Status connect(User user) {
            trace.print(Trace.FLOW ,port.getPortName() + " connect");
            if(port.isConnected()) {
                user.setMessage("already connected");
                trace.print(Trace.ERROR ,port.getPortName() + " already connected");
                return Status.error;
            }
            if(delay>0.0) {
                try {
                    Thread.sleep(milliseconds);
                } catch (InterruptedException ie) {

                }
            }
            port.exceptionConnect();
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#createDevice(org.epics.pvioc.pdrv.User, int)
         */
        public Device createDevice(User user, String deviceName) {
            int addr = Integer.parseInt(deviceName);
            if(addr>=register.length) {
                user.setMessage("illegal deviceName");
                return null;
            }
            Int32Device intDevice = new Int32Device(addr);
            Device device = port.createDevice(intDevice, deviceName);
            intDevice.init(device);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#disconnect(org.epics.pvioc.pdrv.User)
         */
        public Status disconnect(User user) {
            trace.print(Trace.FLOW ,port.getPortName() + " disconnect");
            if(!port.isConnected()) {
                user.setMessage("not connected");
                trace.print(Trace.ERROR ,port.getPortName() + " not connected");
                return Status.error;
            }
            port.exceptionDisconnect();
            return Status.success;
        }
        private class Int32Device implements DeviceDriver {   
            private int addr;
            private Device device;
            private Trace trace;
            
            private Int32Device(int addr) {
                this.addr = addr;
                
            }
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                new Int32Interface(device);
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pdrv.DeviceDriver#report(int)
             */
            public String report(int details) {
                return null;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pdrv.DeviceDriver#connect(org.epics.pvioc.pdrv.User)
             */
            public Status connect(User user) {
                trace.print(Trace.FLOW ,device.getFullName() + " connect");
                if(device.isConnected()) {
                    user.setMessage("already connected");
                    trace.print(Trace.ERROR ,device.getFullName() + " already connected");
                    return Status.error;
                }
                if(delay>0.0) {
                    try {
                        Thread.sleep(milliseconds);
                    } catch (InterruptedException ie) {

                    }
                }
                device.exceptionConnect();
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.pvioc.pdrv.DeviceDriver#disconnect(org.epics.pvioc.pdrv.User)
             */
            public Status disconnect(User user) {
                trace.print(Trace.FLOW ,device.getFullName() + " disconnect");
                if(!device.isConnected()) {
                    user.setMessage("not connected");
                    trace.print(Trace.ERROR ,device.getFullName() + " not connected");
                    return Status.error;
                }
                device.exceptionDisconnect();
                return Status.success;
            }
            
            private class Int32Interface extends  AbstractInt32{
                private Int32Interface(Device device) {
                    super(device);
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractInt32#getBounds(org.epics.pvioc.pdrv.User, int[])
                 */
                public Status getBounds(User user, int[] bounds) {
                    bounds[0] = low;
                    bounds[1] = high;
                    return Status.success;
                }

                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractInt32#read(org.epics.pvioc.pdrv.User)
                 */
                public Status read(User user) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " read but not connected");
                        return Status.error;
                    }
                    double timeout = user.getTimeout();
                    if(timeout>0.0 && delay>timeout) {
                        user.setMessage("timeout");
                        return Status.timeout;
                    }
                    if(delay>0.0) {
                        try {
                        Thread.sleep(milliseconds);
                        } catch (InterruptedException ie) {
                            
                        }
                    }
                    user.setInt(register[addr]);
                    if((trace.getMask()&Trace.DRIVER)!=0) {
                        String info = device.getFullName() + " read value = " + register[addr];
                        trace.print(Trace.DRIVER,info);
                    }
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractInt32#write(org.epics.pvioc.pdrv.User, int)
                 */
                public Status write(User user, int value) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " write but  not connected");
                        return Status.error;
                    }
                    double timeout = user.getTimeout();
                    if(timeout>0.0 && delay>timeout) {
                        user.setMessage("timeout");
                        return Status.timeout;
                    }
                    if(delay>0.0) {
                        try {
                        Thread.sleep(milliseconds);
                        } catch (InterruptedException ie) {
                            
                        }
                    }
                    if(value<low || value>high) {
                        trace.print(Trace.ERROR, device.getFullName() + " value " + value + " out of bounds");
                        user.setMessage("value out of bounds");
                        return Status.error;
                    }
                    register[addr] = value;
                    if((trace.getMask()&Trace.DRIVER)!=0) {
                        String info = device.getFullName() + " write value = " + register[addr];
                        trace.print(Trace.DRIVER,info);
                    }
                    super.interruptOccurred(value);
                    return Status.success;
                }
            }
        }
    }
}

