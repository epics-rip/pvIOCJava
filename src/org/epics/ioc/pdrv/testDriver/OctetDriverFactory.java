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
import org.epics.ioc.pdrv.interfaces.AbstractOctet;
import org.epics.ioc.pdrv.interfaces.Octet;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVStructure;
/**
 * The factory for octetDriver.
 * octetDriver is a portDriver for testing PDRV components.
 * It requires the octetDriver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>multiDevice<br/>
 *       If true octetDriver supports multiple devices.
 *       If false it supports a single device.
 *     </li>
 *     <li>delay<br/>
 *       If 0.0 then octetDriver is synchronous.
 *       If > 0.0 octetDriver is asynchronous and delays delay seconds after each read/write request.
 *      </li>
 * </ul>
 * octetDriver implements interface octet by keeping an internal buffer.
 * A read request returns the value written by the previous write and
 * and also empties the buffer.
 *     
 * @author mrk
 *
 */
public class OctetDriverFactory {
    
    /**
     * Create a new instance of octetDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure octetDriver.
     */
    static public void create(
        String portName,boolean autoConnect,ThreadPriority priority,PVStructure pvStructure)
    {
        PVDouble pvDelay = pvStructure.getDoubleField("delay");
        if(pvDelay==null) {
            throw new IllegalStateException("field delay not found");
        }
        double delay = pvDelay.get();
        boolean canBlock = ((delay>0.0) ? true : false);
        new OctetDriver(portName,autoConnect,canBlock,priority,delay);
    }
    
    static private class OctetDriver implements PortDriver {
        private double delay;
        private String portName;
        private Port port;
        private Trace trace;
        
        private OctetDriver(String portName,
            boolean autoConnect,boolean canBlock,ThreadPriority priority,double delay)
        {
            this.delay = delay;
            this.portName = portName;
            port = Factory.createPort(portName, this, "octetDriver",
                 canBlock, autoConnect, priority);
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
            
            EchoDevice echoDevice = new EchoDevice(delay);
            Device device = port.createDevice(echoDevice, deviceName);
            echoDevice.init(device);
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
    }
          
    
    static private class EchoDevice implements DeviceDriver {   
        private double delay;
        private Device device;
        private Trace trace;
        
        private EchoDevice(double delay) {
            this.delay = delay;
        }
        
        private void init(Device device) {
            this.device = device;
            trace = device.getTrace();
            new EchoOctet(device);
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
        
        private class EchoOctet extends  AbstractOctet{
            private static final int BUFFERSIZE = 4096;
            private long milliseconds;
            private byte[] buffer = new byte[BUFFERSIZE];
            private int size;
            private byte[] eosInput = {0,0};
            private int eosLenInput = 0;
            private byte[] eosOutput = {0,0};
            private int eosLenOutput = 0;
            
            private EchoOctet(Device device) {
                super(device);
                milliseconds = (long)(delay * 1000.0);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#flush(org.epics.ioc.pdrv.User)
             */
            public Status flush(User user) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() + " flush but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                size = 0;
                trace.print(Trace.FLOW ,"flush");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#getInputEos(org.epics.ioc.pdrv.User, byte[])
             */
            public Status getInputEos(User user, byte[] eos) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR,device.getFullName() +  " getInputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                user.setAuxStatus(eosLenInput);
                eos[0] = eosInput[0];
                eos[1] = eosInput[1];
                trace.printIO(Trace.FLOW ,
                        eosInput,eosLenInput,
                        device.getFullName() +  " getInputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#getOutputEos(org.epics.ioc.pdrv.User, byte[])
             */
            public Status getOutputEos(User user, byte[] eos) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() +  " getOutputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                user.setAuxStatus(eosLenOutput);
                eos[0] = eosOutput[0];
                eos[1] = eosOutput[1];
                trace.printIO(Trace.FLOW ,
                        eosOutput,eosLenOutput,
                        device.getFullName() + " getOutputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#read(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status read(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() +  " read but not connected");
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
                    user.setMessage("overflow");
                    n = nbytes;
                    user.setAuxStatus(nbytes - nbytes);
                }
                user.setInt(n);
                for(int i=0; i<n; i++) {
                    data[i] = buffer[i];
                }
                trace.printIO(Trace.DRIVER ,data,n,device.getFullName() + " read");
                size = 0;
                return status;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#readRaw(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status readRaw(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() +  " readRaw but not connected");
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
                trace.printIO(Trace.DRIVER ,data,n,device.getFullName() +  " readRaw");
                size = 0;
                return status;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#setInputEos(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status setInputEos(User user, byte[] eos, int eosLen) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() + " setInputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                if(eosLen<0 || eosLen<2) {
                    user.setMessage("illegal eosLen");
                    return Status.error;
                }
                eosLenInput = eosLen;
                for(int i=0; i<eosLen; i++) eosInput[i] = eos[i];
                trace.printIO(Trace.FLOW ,eosInput,eosLenInput,device.getFullName() + " setInputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#setOutputEos(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status setOutputEos(User user, byte[] eos, int eosLen) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() + " setOutputEos but not connected");
                    user.setMessage("not connected");
                    return Status.error;
                }
                if(eosLen<0 || eosLen<2) {
                    user.setMessage("illegal eosLen");
                    return Status.error;
                }
                eosLenOutput = eosLen;
                for(int i=0; i<eosLen; i++) eosOutput[i] = eos[i];
                trace.printIO(Trace.FLOW ,eosOutput,eosLenOutput,device.getFullName() + " setOutputEos");
                return Status.success;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#write(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status write(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() + " write but not connected");
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
                trace.printIO(Trace.DRIVER ,data,n,device.getFullName() + " write");
                super.interruptOccured(buffer, nbytes);
                return status;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#writeRaw(org.epics.ioc.pdrv.User, byte[], int)
             */
            public Status writeRaw(User user, byte[] data, int nbytes) {
                if(!device.isConnected()) {
                    trace.print(Trace.ERROR ,device.getFullName() + " writeRaw but not connected");
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
                trace.printIO(Trace.DRIVER ,data,n,device.getFullName() + " writeRaw");
                super.interruptOccured(buffer, nbytes);
                return status;
            }
        }
    }
}

