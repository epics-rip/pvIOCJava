/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.testDriver;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.DeviceDriver;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.PortDriver;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.interfaces.AbstractFloat64;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Structure;


/**
 * The factory for float64Driver.
 * float64Driver is a portDriver for testing the float64 support in org.epics.ioc.pdrv.support.
 * It requires the float64Driver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>numberRegisters<br/>
 *       The number of float64 registers to simulate.
 *     </li>
 *      <li>delay<br/>
 *       If 0.0 then float64Driver is synchronous.
 *       If > 0.0 float64Driver is asynchronous and delays delay seconds after each read/write request.
 *      </li>
 * </ul>
 * float64Driver implements interface float64 by keeping an internal int[] array
 * that simulates adc registers.
 * A write request sets a value and a read request reads the current value.
 * @author mrk
 *
 */
public class Float64DriverFactory {
    
    /**
     * Create a new instance of float64Driver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure float64Driver.
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
        index = structure.getFieldIndex("delay");
        if(index<0) {
            throw new IllegalStateException("field delay not found");
        }
        PVDouble pvDelay = (PVDouble)pvFields[index];
        double delay = pvDelay.get();
        boolean canBlock = ((delay>0.0) ? true : false);
        new Float64Driver(portName,autoConnect,priority,numberRegisters,canBlock,delay);
    }
    
    static private class Float64Driver implements PortDriver {
        private double[] register;
        private double delay;
        private long milliseconds;
        private Port port;
        private Trace trace;
        
        private Float64Driver(String portName,boolean autoConnect,ThreadPriority priority,
            int numberRegisters,boolean canBlock,double delay)
        {
            register = new double[numberRegisters];
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            port = Factory.createPort(portName, this, "float64Driver",
                canBlock, autoConnect,priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "delay " + delay;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#connect(org.epics.ioc.pdrv.User)
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
         * @see org.epics.ioc.pdrv.PortDriver#createDevice(org.epics.ioc.pdrv.User, int)
         */
        public Device createDevice(User user, String deviceName) {
            int addr = Integer.parseInt(deviceName);
            if(addr>=register.length) {
                user.setMessage("illegal deviceName");
                return null;
            }
            Float64Device float64Device = new Float64Device(addr);
            Device device = port.createDevice(float64Device, deviceName);
            float64Device.init(device);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#disconnect(org.epics.ioc.pdrv.User)
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
        private class Float64Device implements DeviceDriver {   
            private Device device;
            private Trace trace;
            private int addr;
            
            private Float64Device(int addr) {
                this.addr = addr;
            }
           
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                new Float64Interface(device);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.DeviceDriver#report(int)
             */
            public String report(int details) {
                return null;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.DeviceDriver#connect(org.epics.ioc.pdrv.User)
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
             * @see org.epics.ioc.pdrv.DeviceDriver#disconnect(org.epics.ioc.pdrv.User)
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
            
            private class Float64Interface extends  AbstractFloat64{
                
                private Float64Interface(Device device) {
                    super(device);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractFloat64#read(org.epics.ioc.pdrv.User)
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
                    user.setDouble(register[addr]);
                    if((trace.getMask()&Trace.DRIVER)!=0) {
                        String info = device.getFullName() + " read value = " + register[addr];
                        trace.print(Trace.DRIVER,info);
                    }
                    return Status.success;
                }                
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractFloat64#write(org.epics.ioc.pdrv.User, double)
                 */
                public Status write(User user, double value) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " write but not connected");
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

