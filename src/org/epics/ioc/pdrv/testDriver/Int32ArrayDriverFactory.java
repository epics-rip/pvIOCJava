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
import org.epics.pvData.factory.BasePVIntArray;
import org.epics.pvData.factory.FieldFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.FieldCreate;
import org.epics.pvData.pv.IntArrayData;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVIntArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarArray;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Structure;


/**
 * The factory for int32ArrayDriver.
 * int32ArrayDriver is a portDriver for testing the int32Array support in org.epics.ioc.pdrv.support.
 * @author mrk
 *
 */
public class Int32ArrayDriverFactory {
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    /**
     * Create a new instance of int32ArrayDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure int32ArrayDriver.
     */
    static public void create(
        String portName,boolean autoConnect,ThreadPriority priority,PVStructure pvStructure)
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
        private PVStructure parent;
        private Port port;
        private Trace trace;
        
        private Int32ArrayDriver(PVStructure parent,String portName,boolean autoConnect,ThreadPriority priority,
            boolean canBlock,double delay,int maxSegmentSize)
        {
            this.parent = parent;
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            this.maxSegmentSize = maxSegmentSize;
            port = Factory.createPort(portName, this, "int32ArrayDriver",
                canBlock, autoConnect,priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "delay " + delay + " maxSegmentSize " + maxSegmentSize;
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
            Int32ArrayDevice intDevice = new Int32ArrayDevice();
            Device device = port.createDevice(intDevice, deviceName);
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
            
            private Int32ArrayDevice() {}
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                ScalarArray array = fieldCreate.createScalarArray( ScalarType.pvInt);
                PVIntArray pvIntArray = new PVIntArrayImpl(parent,array,device);
                new Int32ArrayImpl(pvIntArray,device);
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
            
            private class Int32ArrayImpl extends AbstractInt32Array{
                
                private Int32ArrayImpl(PVIntArray pvIntArray,Device device) {
                    super(pvIntArray,device);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractInt32Array#startRead(org.epics.ioc.pdrv.User)
                 */
                public Status startRead(User user) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " startRead but not connected");
                        return Status.error;
                    }
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
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " startWrite but not connected");
                        return Status.error;
                    }
                    if(super.getPVIntArray().isImmutable()) {
                        trace.print(Trace.ERROR,device.getFullName() + " put is immutable");
                        user.setMessage("not mutable");
                        return Status.error;
                    }
                    double timeout = user.getTimeout();
                    if(timeout>0.0 && delay>timeout) {
                        user.setMessage("timeout");
                        return Status.timeout;
                    }
                    return super.startWrite(user);
                }              
            }
            
            private class PVIntArrayImpl extends BasePVIntArray{
                private Device device;
                
                private PVIntArrayImpl(PVStructure parent,ScalarArray array,Device device) {
                    super(parent,array);
                    this.device = device;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvData.factory.BasePVIntArray#setCapacity(int)
                 */
                public void setCapacity(int len) {
                    if(!capacityMutable) {
                        super.message("not capacityMutable", MessageType.error);
                        return;
                    }
                    if(length>len) length = len;
                    int[]newarray = new int[len];
                    if(length>0) System.arraycopy(value,0,newarray,0,length);
                    value = newarray;
                    capacity = len;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvData.factory.BasePVIntArray#get(int, int, org.epics.pvData.pv.IntArrayData)
                 */
                public int get(int offset,int len, IntArrayData data) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " get but not connected");
                        return 0;
                    }
                    if(delay>0.0) {
                        try {
                            Thread.sleep(milliseconds);
                        } catch (InterruptedException ie) {

                        }
                    }
                    if((trace.getMask()&Trace.DRIVER)!=0) {
                        String info = device.getFullName() + " get offset " + offset + " len " + len;
                        trace.print(Trace.DRIVER,info);
                    }
                    if(maxSegmentSize>0 && len>maxSegmentSize) len = maxSegmentSize;
                    int n = len;
                    if(offset+len > length) n = length - offset;
                    data.data = value;
                    data.offset = offset;
                    return n;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvData.factory.BasePVIntArray#put(int, int, int[], int)
                 */
                public int put(int offset, int len, int[] from, int fromOffset) {
                    if(super.isImmutable()) {
                        return 0;
                    }
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " put but not connected");
                        return 0;
                    }
                    if(delay>0.0) {
                        try {
                            Thread.sleep(milliseconds);
                        } catch (InterruptedException ie) {

                        }
                    }
                    if((trace.getMask()&Trace.DRIVER)!=0) {
                        String info = device.getFullName() + " put offset " + offset + " len " + len;
                        trace.print(Trace.DRIVER,info);
                    }
                    if(maxSegmentSize>0 && len>maxSegmentSize) len = maxSegmentSize;
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
                    return len;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvData.factory.BasePVIntArray#equals(java.lang.Object)
                 */
                @Override
                public boolean equals(Object obj) { // implemented to satisfy FindBugs
                    return super.equals(obj);
                }
                /* (non-Javadoc)
                 * @see org.epics.pvData.factory.BasePVIntArray#hashCode()
                 */
                @Override
                public int hashCode() { // implemented to satisfy FindBugs
                    return super.hashCode();
                }
            }
        }
    }
}

