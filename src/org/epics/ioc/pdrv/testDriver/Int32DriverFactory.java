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
 * The factory for echoDriver.
 * int32Driver is a portDriver for testing the int32 support in org.epics.ioc.pdrv.support.
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
 *       If 0.0 then echoDriver is synchronous.
 *       If > 0.0 echoDriver is asynchronous and delays delay seconds after each read/write request.
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
     * Create a new instance of echoDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure echoDriver.
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
        private Port port;
        private Trace trace;
        
        private Int32Driver(String portName,boolean autoConnect,ScanPriority priority,
            int numberRegisters,int low,int high,boolean canBlock,double delay)
        {
            register = new int[numberRegisters];
            this.low = low;
            this.high = high;
            this.delay = delay;
            boolean isMultiDevicePort = (numberRegisters==1) ? false : true;
            port = Factory.createPort(portName, this, "int32Driver",
                isMultiDevicePort, canBlock, autoConnect,priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "low " + low + " high " + high;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#connect(org.epics.ioc.pdrv.User)
         */
        public Status connect(User user) {
            trace.print(Trace.FLOW ,"connect");
            if(port.isConnected()) {
                user.setMessage("already connected");
                trace.print(Trace.ERROR ,"already connected");
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
            Int32Device intDevice = new Int32Device(addr);
            Device device = port.createDevice(intDevice, addr);
            intDevice.init(device);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#disconnect(org.epics.ioc.pdrv.User)
         */
        public Status disconnect(User user) {
            trace.print(Trace.FLOW ,"disconnect");
            if(!port.isConnected()) {
                user.setMessage("not connected");
                trace.print(Trace.ERROR ,"not connected");
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
                new Int32Interface();
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
                trace.print(Trace.FLOW ,"connect");
                if(device.isConnected()) {
                    user.setMessage("already connected");
                    trace.print(Trace.ERROR ,"already connected");
                    return Status.error;
                }
                device.exceptionConnect();
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.DeviceDriver#disconnect(org.epics.ioc.pdrv.User)
             */
            public Status disconnect(User user) {
                trace.print(Trace.FLOW ,"disconnect");
                if(!device.isConnected()) {
                    user.setMessage("not connected");
                    trace.print(Trace.ERROR ,"not connected");
                    return Status.error;
                }
                device.exceptionDisconnect();
                return Status.success;
            }
            
            private class Int32Interface extends  Int32Base{
                private long milliseconds;
                private Int32Interface() {
                    super(device,"int32");
                    milliseconds = (long)(delay * 1000.0);
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.Int32Base#getBounds(org.epics.ioc.pdrv.User, int[])
                 */
                public Status getBounds(User user, int[] bounds) {
                    bounds[0] = low;
                    bounds[1] = high;
                    return Status.success;
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.Int32Base#read(org.epics.ioc.pdrv.User)
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
                    user.setInt(register[addr]);
                    trace.print(Trace.DRIVER,"read value = " + register[addr]);
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.Int32Base#write(org.epics.ioc.pdrv.User, int)
                 */
                public Status write(User user, int value) {
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
                        trace.print(Trace.ERROR, "value out of bounds");
                        user.setMessage("value out of bounds");
                        return Status.error;
                    }
                    register[addr] = value;
                    trace.print(Trace.DRIVER,"write value = " + register[addr]);
                    super.interruptOccured(value);
                    return Status.success;
                }
            }
        }
    }
}

