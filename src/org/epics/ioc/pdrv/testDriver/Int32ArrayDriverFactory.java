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
import org.epics.ioc.pdrv.interfaces.AbstractInt32Array;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.FieldCreate;
import org.epics.ioc.pv.FieldFactory;
import org.epics.ioc.pv.IntArrayData;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.ScanPriority;
/**
 * The factory for int32ArrayDriver.
 * int32ArrayDriver is a portDriver for testing the int32Array support in org.epics.ioc.pdrv.support.
 * @author mrk
 *
 */
public class Int32ArrayDriverFactory {
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * Create a new instance of int32ArrayDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure int32ArrayDriver.
     */
    static public void create(
        String portName,boolean autoConnect,ScanPriority priority,PVStructure pvStructure)
    {
        PVField[] pvFields = pvStructure.getPVFields();
        Structure structure = (Structure)pvStructure.getField();
        int index = structure.getFieldIndex("delay");
        if(index<0) {
            throw new IllegalStateException("field delay not found");
        }
        PVDouble pvDelay = (PVDouble)pvFields[index];
        double delay = pvDelay.get();
        index = structure.getFieldIndex("maxSegmentSize");
        if(index<0) {
            throw new IllegalStateException("field maxSegmentSize not found");
        }
        PVInt pvMaxSegmentSize = (PVInt)pvFields[index];
        int maxSegmentSize = pvMaxSegmentSize.get();
        boolean canBlock = ((delay>0.0) ? true : false);
        new Int32ArrayDriver(pvStructure,portName,autoConnect,priority,canBlock,delay,maxSegmentSize);
    }
    
    static private class Int32ArrayDriver implements PortDriver {
        private double delay;
        private long milliseconds;
        private int maxSegmentSize = 0;
        private PVField parent;
        private Port port;
        private Trace trace;
        
        private Int32ArrayDriver(PVField parent,String portName,boolean autoConnect,ScanPriority priority,
            boolean canBlock,double delay,int maxSegmentSize)
        {
            this.parent = parent;
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            this.maxSegmentSize = maxSegmentSize;
            port = Factory.createPort(portName, this, "int32ArrayDriver",
                true, canBlock, autoConnect,priority);
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
            Int32ArrayDevice intDevice = new Int32ArrayDevice();
            Device device = port.createDevice(intDevice, addr);
            intDevice.init(device);
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
        
        private class Int32ArrayDevice implements DeviceDriver {
            private Device device;
            private Trace trace;
            private String deviceName = null;
            
            private Int32ArrayDevice() {}
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                deviceName = device.getPort().getPortName() + ":" + device.getAddr();
                Array array = fieldCreate.createArray("drvPrivate", Type.pvInt);
                new Int32ArrayImpl(parent,array,device);
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
            
            private class Int32ArrayImpl extends AbstractInt32Array{
                private int[] value = new int[0];
                
                private Int32ArrayImpl(PVField parent,Array array,Device device) {
                    super(parent,array,0,true,device);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pv.AbstractPVArray#setCapacity(int)
                 */
                public void setCapacity(int len) {
                    if(!capacityMutable) {
                        super.message("not capacityMutable", MessageType.error);
                        return;
                    }
                    super.asynAccessCallListener(true);
                    try {
                        if(length>len) length = len;
                        int[]newarray = new int[len];
                        if(length>0) System.arraycopy(value,0,newarray,0,length);
                        value = newarray;
                        capacity = len;
                    } finally {
                        super.asynAccessCallListener(false);
                    }
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractInt32Array#startRead(org.epics.ioc.pdrv.User)
                 */
                public Status startRead(User user) {
                    double timeout = user.getTimeout();
                    if(timeout>0.0 && delay>timeout) {
                        user.setMessage("timeout");
                        return Status.timeout;
                    }
                    return super.startRead(user);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractInt32Array#startWrite(org.epics.ioc.pdrv.User)
                 */
                public Status startWrite(User user) {
                    double timeout = user.getTimeout();
                    if(timeout>0.0 && delay>timeout) {
                        user.setMessage("timeout");
                        return Status.timeout;
                    }
                    return super.startWrite(user);
                }              
                public int get(int offset,int len, IntArrayData data) {
                    if(delay>0.0) {
                        try {
                        Thread.sleep(milliseconds);
                        } catch (InterruptedException ie) {
                            
                        }
                    }
                    if(maxSegmentSize>0 && len>maxSegmentSize) len = maxSegmentSize;
                    super.asynAccessCallListener(true);
                    try {
                        int n = len;
                        if(offset+len > length) n = length - offset;
                        data.data = value;
                        data.offset = offset;
                        return n;
                    } finally {
                        super.asynAccessCallListener(false);
                    }
                }
                
                public int put(int offset, int len, int[] from, int fromOffset) {
                    if(!super.isMutable()) {
                        return 0;
                    }
                    if(delay>0.0) {
                        try {
                        Thread.sleep(milliseconds);
                        } catch (InterruptedException ie) {
                            
                        }
                    }
                    if(maxSegmentSize>0 && len>maxSegmentSize) len = maxSegmentSize;
                    super.asynAccessCallListener(true);
                    try {
                        if(offset+len > length) {
                            int newlength = offset + len;
                            if(newlength>capacity) {
                                setCapacity(newlength);
                                newlength = capacity;
                                len = newlength - offset;
                                if(len<=0) return 0;
                            }
                            length = newlength;
                        }
                        System.arraycopy(from,fromOffset,value,offset,len);                       
                    } finally {
                        super.asynAccessCallListener(false);
                    }
                    return len;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pv.AbstractPVField#toString(int)
                 */
                public String toString(int indentLevel) {
                    return convert.getString(this, indentLevel)
                    + super.toString(indentLevel);
                }
            }
        }
    }
}

