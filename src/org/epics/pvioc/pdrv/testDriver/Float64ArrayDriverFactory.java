/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.testDriver;

import org.epics.pvdata.factory.BasePVDoubleArray;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.DoubleArrayData;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.DeviceDriver;
import org.epics.pvioc.pdrv.Factory;
import org.epics.pvioc.pdrv.Port;
import org.epics.pvioc.pdrv.PortDriver;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.User;
import org.epics.pvioc.pdrv.interfaces.AbstractFloat64Array;

/**
 * The factory for float64ArrayDriver.
 * float64ArrayDriver is a portDriver for testing the float64Array support in org.epics.pvioc.pdrv.support.
 * @author mrk
 *
 */
public class Float64ArrayDriverFactory {
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    /**
     * Create a new instance of float64ArrayDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure float64ArrayDriver.
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
        new Float64ArrayDriver(pvStructure,portName,autoConnect,priority,canBlock,delay,maxSegmentSize);
    }
    
    static private class Float64ArrayDriver implements PortDriver {
        private double delay;
        private long milliseconds;
        private int maxSegmentSize = 0;
        private PVStructure parent;
        private Port port;
        private Trace trace;
        
        private Float64ArrayDriver(PVStructure parent,String portName,boolean autoConnect,ThreadPriority priority,
            boolean canBlock,double delay,int maxSegmentSize)
        {
            this.parent = parent;
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            this.maxSegmentSize = maxSegmentSize;
            port = Factory.createPort(portName, this, "float64ArrayDriver",
                canBlock, autoConnect,priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "delay " + delay + " maxSegmentSize " + maxSegmentSize;
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
            Float64ArrayDevice dev = new Float64ArrayDevice();
            Device device = port.createDevice(dev, deviceName);
            dev.init(device);
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
        
        private class Float64ArrayDevice implements DeviceDriver {
            private Device device;
            private Trace trace;
            
            private Float64ArrayDevice() {}
            
            private void init(Device device) {
                this.device = device;
                trace = device.getTrace();
                ScalarArray array = fieldCreate.createScalarArray( ScalarType.pvDouble);
                PVDoubleArray pvDoubleArray = new PVDoubleArrayImpl(parent,array,device);
                new Float64ArrayImpl(pvDoubleArray,device);
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
            
            private class Float64ArrayImpl extends AbstractFloat64Array{
                private Float64ArrayImpl(PVDoubleArray pvDoubleArray,Device device) {
                    super(pvDoubleArray,device);
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractFloat64Array#startRead(org.epics.pvioc.pdrv.User)
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
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractFloat64Array#startWrite(org.epics.pvioc.pdrv.User)
                 */
                public Status startWrite(User user) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " startWrite but not connected");
                        return Status.error;
                    }
                    if(super.getPVDoubleArray().isImmutable()) {
                        trace.print(Trace.ERROR,device.getFullName() + " put but is immutable");
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
            
            private class PVDoubleArrayImpl extends BasePVDoubleArray{
                private Device device;
                
                private PVDoubleArrayImpl(PVStructure parent,ScalarArray array,Device device) {
                    super(array);
                    this.device = device;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvdata.factory.BasePVDoubleArray#setCapacity(int)
                 */
                public void setCapacity(int len) {
                    if(!capacityMutable) {
                        super.message("not capacityMutable", MessageType.error);
                        return;
                    }
                    if(length>len) length = len;
                    double[]newarray = new double[len];
                    if(length>0) System.arraycopy(value,0,newarray,0,length);
                    value = newarray;
                    capacity = len;
                }            
                /* (non-Javadoc)
                 * @see org.epics.pvdata.factory.BasePVDoubleArray#get(int, int, org.epics.pvdata.pv.DoubleArrayData)
                 */
                public int get(int offset,int len, DoubleArrayData data) {
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
                 * @see org.epics.pvdata.factory.BasePVDoubleArray#put(int, int, double[], int)
                 */
                public int put(int offset, int len, double[] from, int fromOffset) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() + " put but not connected");
                        return 0;
                    }
                    if(super.isImmutable()) {
                        trace.print(Trace.ERROR,device.getFullName() + " put but is immutable");
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
                 * @see org.epics.pvdata.factory.BasePVDoubleArray#equals(java.lang.Object)
                 */
                @Override
                public boolean equals(Object obj) { // implemented to satisfy FindBugs
                    return super.equals(obj);
                }
                /* (non-Javadoc)
                 * @see org.epics.pvdata.factory.BasePVDoubleArray#hashCode()
                 */
                @Override
                public int hashCode() { // implemented to satisfy FindBugs
                    return super.hashCode();
                }
            }
        }
    }
}

