/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.testDriver;

import org.epics.ioc.pdrv.*;
import org.epics.ioc.pdrv.interfaces.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
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
        String portName,boolean autoConnect,ScanPriority priority,PVStructure pvStructure)
    {
        PVField[] pvFields = pvStructure.getFieldPVFields();
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
        
        private Float64Driver(String portName,boolean autoConnect,ScanPriority priority,
            int numberRegisters,boolean canBlock,double delay)
        {
            register = new double[numberRegisters];
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            boolean isMultiDevicePort = (numberRegisters==1) ? false : true;
            port = Factory.createPort(portName, this, "float64Driver",
                isMultiDevicePort, canBlock, autoConnect,priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            return null;
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
            port.exceptionConnect();
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#createDevice(org.epics.ioc.pdrv.User, int)
         */
        public Device createDevice(User user, int addr) {
            if(addr>=register.length) {
                user.setMessage("illegal address");
                return null;
            }
            Float64Device float64Device = new Float64Device(addr);
            Device device = port.createDevice(float64Device, addr);
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
            private int addr;
            private Device device;
            private Trace trace;
            private String deviceName = null;
            
            private Float64Device(int addr) {
                this.addr = addr;
            }
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                deviceName = device.getPort().getPortName() + ":" + device.getAddr();
                new Float64Interface();
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
                trace.print(Trace.FLOW ,deviceName + " connect");
                if(device.isConnected()) {
                    user.setMessage("already connected");
                    trace.print(Trace.ERROR ,deviceName + " already connected");
                    return Status.error;
                }
                device.exceptionConnect();
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.DeviceDriver#disconnect(org.epics.ioc.pdrv.User)
             */
            public Status disconnect(User user) {
                trace.print(Trace.FLOW ,deviceName + " disconnect");
                if(!device.isConnected()) {
                    user.setMessage("not connected");
                    trace.print(Trace.ERROR ,deviceName + " not connected");
                    return Status.error;
                }
                device.exceptionDisconnect();
                return Status.success;
            }
            
            private class Float64Interface extends  AbstractFloat64{
                
                private Float64Interface() {
                    super(device,"float64");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractFloat64#read(org.epics.ioc.pdrv.User)
                 */
                public Status read(User user) {
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
                    trace.print(Trace.DRIVER,deviceName + " read value = " + register[addr]);
                    return Status.success;
                }                
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractFloat64#write(org.epics.ioc.pdrv.User, double)
                 */
                public Status write(User user, double value) {
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
                    trace.print(Trace.DRIVER,deviceName + " write value = " + register[addr]);
                    super.interruptOccured(value);
                    return Status.success;
                }
            }
        }
    }
}

