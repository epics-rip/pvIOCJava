/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.drivers;

import org.epics.ioc.pdrv.*;
import org.epics.ioc.pdrv.interfaces.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;
/**
 * The factory for echoDriver.
 * echoDriver is a portDriver for testing PDRV components.
 * It requires the echoDriver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>multiDevice<br/>
 *       If true echoDriver supports multiple devices.
 *       If false it supports a single device.
 *     </li>
 *     <li>delay<br/>
 *       If 0.0 then echoDriver is synchronous.
 *       If > 0.0 echoDriver is asynchronous and delays delay seconds after each read/write request.
 *      </li>
 * </ul>
 * echoDriver implements interface octet by keeping an internal buffer.
 * A read request returns the value written by the previous write and
 * and also empties the buffer.
 *     
 * @author mrk
 *
 */
public class EchoDriverFactory {
    
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
        int index = structure.getFieldIndex("multiDevice");
        if(index<0) {
            throw new IllegalStateException("field multiDevice not found");
        }
        PVBoolean pvMultiDevice = (PVBoolean)pvFields[index];
        boolean multiDevice = pvMultiDevice.get();
        index = structure.getFieldIndex("delay");
        if(index<0) {
            throw new IllegalStateException("field delay not found");
        }
        PVDouble pvDelay = (PVDouble)pvFields[index];
        double delay = pvDelay.get();
        boolean canBlock = ((delay>0.0) ? true : false);
        new EchoDriver(portName,multiDevice,autoConnect,canBlock,priority,delay);
    }
    
    static private class EchoDriver implements PortDriver {
        private double delay;
        private Port port;
        private Trace trace;
        
        private EchoDriver(String portName,boolean isMultiDevicePort,
            boolean autoConnect,boolean canBlock,ScanPriority priority,double delay)
        {
            this.delay = delay;
            port = Factory.createPort(portName, this, "echoDriver",
                isMultiDevicePort, canBlock, autoConnect, priority);
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
            EchoDevice echoDevice = new EchoDevice(addr,delay);
            Device device = port.createDevice(echoDevice, addr);
            echoDevice.init(device);
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
    }
          
    
    static private class EchoDevice implements DeviceDriver {   
        private double delay;
        private Device device;
        private Trace trace;
        
        private EchoDevice(int addr,double delay) {
            this.delay = delay;
        }
        
        private void init(Device device) {
            this.device = device;
            trace = device.getTrace();
            new EchoOctet();
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
        
        private class EchoOctet extends  OctetBase{
            private static final int BUFFERSIZE = 4096;
            private long milliseconds;
            private byte[] buffer = new byte[BUFFERSIZE];
            private int size;
            private byte[] eosInput = {0,0};
            private int eosLenInput = 0;
            private byte[] eosOutput = {0,0};
            private int eosLenOutput = 0;
            
            private EchoOctet() {
                super(device,"octet");
                milliseconds = (long)(delay * 1000.0);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#flush(org.epics.ioc.pdrv.User)
             */
            public Status flush(User user) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"flush but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                size = 0;
                trace.print(Trace.FLOW ,"flush");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#getInputEos(org.epics.ioc.pdrv.User, byte[])
             */
            public Status getInputEos(User user, byte[] eos) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR,"getInputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                user.setAuxStatus(eosLenInput);
                eos[0] = eosInput[0];
                eos[1] = eosInput[1];
                trace.printIO(Trace.FLOW ,
                        eosInput,eosLenInput,
                        "getInputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#getOutputEos(org.epics.ioc.pdrv.User, byte[])
             */
            public Status getOutputEos(User user, byte[] eos) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"getOutputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                user.setAuxStatus(eosLenOutput);
                eos[0] = eosOutput[0];
                eos[1] = eosOutput[1];
                trace.printIO(Trace.FLOW ,
                        eosOutput,eosLenOutput,
                        "getOutputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#read(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status read(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"read but not connected");
                    user.setMessage("not connected");
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
                Status status = Status.success;
                user.setAuxStatus(Octet.EOM_END);
                int n = size;
                if(n>0 && eosLenInput==1) {
                    if(buffer[n-1]==eosInput[0]) {
                        n -= 1;
                        user.setAuxStatus(Octet.EOM_EOS);
                    }
                } else if(n>1 && eosLenInput==2) {
                    byte first = buffer[n-2];
                    byte second = buffer[n-1];
                    if(first==eosInput[0] && second==eosInput[1]) {
                        n -= 2;
                        user.setAuxStatus(Octet.EOM_EOS);
                    }
                }
                if(n>nbytes) {
                    status = Status.overflow;
                    n = nbytes;
                    user.setAuxStatus(nbytes - nbytes);
                }
                user.setInt(n);
                for(int i=0; i<n; i++) {
                    data[i] = buffer[i];
                }
                trace.printIO(Trace.IO_DRIVER ,data,n,"read");
                size = 0;
                return status;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#readRaw(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status readRaw(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"readRaw but not connected");
                    user.setMessage("not connected");
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
                Status status = Status.success;
                int n = size;
                if(n>nbytes) {
                    status = Status.overflow;
                    n = nbytes;
                    user.setAuxStatus(nbytes - nbytes);
                } else {
                    user.setAuxStatus(Octet.EOM_END);
                }
                user.setInt(n);
                for(int i=0; i<n; i++) {
                    data[i] = buffer[i];
                }
                trace.printIO(Trace.IO_DRIVER ,data,n,"readRaw");
                size = 0;
                return status;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#setInputEos(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status setInputEos(User user, byte[] eos, int eosLen) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"setInputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                if(eosLen<0 || eosLen<2) {
                    user.setMessage("illegal eosLen");
                    return Status.error;
                }
                eosLenInput = eosLen;
                for(int i=0; i<eosLen; i++) eosInput[i] = eos[i];
                trace.printIO(Trace.FLOW ,eosInput,eosLenInput,"setInputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#setOutputEos(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status setOutputEos(User user, byte[] eos, int eosLen) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"setOutputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                if(eosLen<0 || eosLen<2) {
                    user.setMessage("illegal eosLen");
                    return Status.error;
                }
                eosLenOutput = eosLen;
                for(int i=0; i<eosLen; i++) eosOutput[i] = eos[i];
                trace.printIO(Trace.FLOW ,eosOutput,eosLenOutput,"setOutputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#write(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status write(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"write but not connected");
                    user.setMessage("not connected");
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
                Status status = Status.success;
                user.setAuxStatus(Octet.EOM_END);
                int n = nbytes;
                int maxbytes = BUFFERSIZE - eosLenOutput;
                if(n>maxbytes) {
                    status = Status.overflow;
                    n = maxbytes;
                    size = n;
                    user.setAuxStatus(n - maxbytes);
                } else if(eosLenInput==1) {
                    buffer[n] = eosOutput[0];
                    size = n + 1;
                    user.setAuxStatus(Octet.EOM_EOS);
                } else if(eosLenInput==2) {
                    buffer[n] = eosOutput[0];
                    buffer[n+1] = eosOutput[1];
                    size = n + 2;
                    user.setAuxStatus(Octet.EOM_EOS);
                } else {
                    size = n;
                }
                for(int i=0; i<n; i++) {
                    buffer[i] = data[i];
                }
                user.setInt(n);
                trace.printIO(Trace.IO_DRIVER ,data,n,"write");
                super.interruptOccured(buffer, nbytes);
                return status;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.OctetBase#writeRaw(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status writeRaw(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,"writeRaw but not connected");
                    user.setMessage("not connected");
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
                Status status = Status.success;
                user.setAuxStatus(Octet.EOM_END);
                int n = nbytes;
                int maxbytes = BUFFERSIZE;
                if(n>maxbytes) {
                    status = Status.overflow;
                    n = maxbytes;
                    user.setAuxStatus(n - maxbytes);
                }
                size = n;
                for(int i=0; i<n; i++) {
                    buffer[i] = data[i];
                }
                user.setInt(size);
                trace.printIO(Trace.IO_DRIVER ,data,n,"writeRaw");
                super.interruptOccured(buffer, nbytes);
                return status;
            }
        }
    }
}

