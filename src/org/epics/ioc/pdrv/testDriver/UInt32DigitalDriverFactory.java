/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.testDriver;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.DeviceDriver;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.PortDriver;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pdrv.interfaces.AbstractUInt32Digital;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Structure;
/**
 * The factory for uint32DigitalDriver.
 * uint32DigitalDriver is a portDriver for testing the uint32Digital support in org.epics.ioc.pdrv.support.
 * It requires the uint32DigitalDriver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>numberRegisters<br/>
 *       The number of uint32Digital registers to simulate.
 *     </li>
 *     <li>delay<br/>
 *       If 0.0 then uint32DigitalDriver is synchronous.
 *       If > 0.0 uint32DigitalDriver is asynchronous and delays delay seconds after each read/write request.
 *      </li>
 * </ul>
 * uint32DigitalDriver implements interface uint32Digital by keeping an internal int[] array
 * that simulates digital I/O registers.
 * A write request sets a value and a read request reads the current value.
 * @author mrk
 *
 */
public class UInt32DigitalDriverFactory {
    
    /**
     * Create a new instance of uint32DigitalDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure uint32DigitalDriver.
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
        new UInt32DigitalDriver(portName,autoConnect,priority,numberRegisters,canBlock,delay);
    }
    
    static private class UInt32DigitalDriver implements PortDriver {
        private int[] register;
        private double delay;
        private Port port;
        private String portName;
        private Trace trace;
        
        private UInt32DigitalDriver(String portName,boolean autoConnect,ThreadPriority priority,
            int numberRegisters,boolean canBlock,double delay)
        {
            register = new int[numberRegisters];
            this.delay = delay;
            port = Factory.createPort(portName, this, "uint32DigitalDriver",canBlock, autoConnect,priority);
            portName = port.getPortName();
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
            trace.print(Trace.FLOW ,portName + " connect");
            if(port.isConnected()) {
                user.setMessage("already connected");
                trace.print(Trace.ERROR ,portName + " already connected");
                return Status.error;
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
            UInt32DigitalDevice intDevice = new UInt32DigitalDevice(addr);
            Device device = port.createDevice(intDevice, deviceName);
            intDevice.init(device);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#disconnect(org.epics.ioc.pdrv.User)
         */
        public Status disconnect(User user) {
            trace.print(Trace.FLOW ,portName + " disconnect");
            if(!port.isConnected()) {
                user.setMessage("not connected");
                trace.print(Trace.ERROR ,portName + " not connected");
                return Status.error;
            }
            port.exceptionDisconnect();
            return Status.success;
        }
        private class UInt32DigitalDevice implements DeviceDriver {   
            private int addr;
            private Device device;
            private Trace trace;
            
            private UInt32DigitalDevice(int addr) {
                this.addr = addr;
            }
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                new UInt32DigitalInterface(device);
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
            
            private class UInt32DigitalInterface extends  AbstractUInt32Digital{
                private long milliseconds;
                private UInt32DigitalInterface(Device device) {
                    super(device);
                    milliseconds = (long)(delay * 1000.0);
                }               
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractUInt32Digital#read(org.epics.ioc.pdrv.User, int)
                 */
                public Status read(User user, int mask) {
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
                    int value = register[addr]&mask;
                    user.setInt(value);
                    trace.print(Trace.DRIVER,device.getFullName() + " read value = " + value);
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractUInt32Digital#write(org.epics.ioc.pdrv.User, int, int)
                 */
                public Status write(User user, int value, int mask) {
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
                    int newValue = register[addr]&~mask;
                    newValue |= value&mask;
                    register[addr] = newValue;
                    trace.print(Trace.DRIVER,device.getFullName() + " write value = " + register[addr]);
                    super.interruptOccured(newValue);
                    return Status.success;
                }
            }
        }
    }
}

