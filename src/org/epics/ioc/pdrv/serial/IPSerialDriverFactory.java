/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
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
public class IPSerialDriverFactory {
    
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
        PVString pvHost = pvStructure.getStringField("host");
        if(pvHost==null) {
            throw new IllegalStateException("field host not found");
        }
        PVInt pvPort = pvStructure.getIntField("port");
        if(pvPort==null) {
            throw new IllegalStateException("field port not found");
        }
        new IPSerialDriver(portName,autoConnect,priority,pvHost.get(),pvPort.get());
    }
    
    static private class IPSerialDriver implements PortDriver {
        private String host;
        private int ipport;
        private String portName;
        private Port port;
        private Trace trace;
        private Socket server = null;
        
        private IPSerialDriver(String portName,
            boolean autoConnect,ThreadPriority priority,String host, int ipport)
        {
            this.host = host;
            this.ipport = ipport;
            this.portName = portName;
            port = Factory.createPort(portName, this, "IPSerialDriver",true, autoConnect, priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "host " + host + " port " + ipport;
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
            try {
                server = new Socket(host,ipport);
            } catch (UnknownHostException e) {
                user.setMessage("connect UnknownHostException " + e.getMessage());
                trace.print(Trace.ERROR ,portName + " connect UnknownHostException " + e.getMessage());
                return Status.error;
            } catch (IOException e) {
                user.setMessage("connect IOException " + e.getMessage());
                trace.print(Trace.ERROR ,portName + " connect IOException " + e.getMessage());
                return Status.error;
            }
            port.exceptionConnect();
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#createDevice(org.epics.ioc.pdrv.User, int)
         */
        public Device createDevice(User user, String deviceName) {
            
            IPSerialDevice echoDevice = new IPSerialDevice();
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
            try {
                server.close();
            } catch (IOException e) {
                user.setMessage("disconnect IOException " + e.getMessage());
                trace.print(Trace.ERROR ,portName + " disconnect IOException " + e.getMessage());
                return Status.error;
            }
            port.exceptionDisconnect();
            return Status.success;
        }
          
    
        private class IPSerialDevice implements DeviceDriver {   
            private Device device;
            private Trace trace;
            private OutputStream out = null;
            private InputStream in = null;

            private boolean init(Device device) {
               
                this.device = device;
                trace = device.getTrace();
                new IPSerialOctet(device);
                return true;
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
                try {
                    out = server.getOutputStream();
                    in = server.getInputStream();
                } catch (IOException e) {
                    String message = device.getFullName() + " connect  IOException " + e.getMessage();
                    user.setMessage(message);
                    trace.print(Trace.ERROR ,message);
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

            private class IPSerialOctet extends  AbstractOctet{
                private byte[] buffer = null;
                private byte[] eosInput = {0,0};
                private int eosLenInput = 0;
                private byte[] eosOutput = {0,0};
                private int eosLenOutput = 0;

                private IPSerialOctet(Device device) {
                    super(device);
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
                        trace.print(Trace.ERROR ,device.getFullName() +  " readRaw but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    Status status = Status.success;
                    double timeout = user.getTimeout();
                    int milliseconds = (int)(timeout*1000);
                    int got = 0;
                    try {
                        server.setSoTimeout(milliseconds);
                    } catch (SocketException e) {
                        user.setMessage("socketException " + e.getMessage());
                        return Status.error;
                    }
                    try { 
                        got = in.read(data, 0, nbytes);
                    } catch (IOException e) {
                        user.setMessage("IOException " + e.getMessage());
                        return Status.error;
                    }
                    user.setAuxStatus(Octet.EOM_END);
                    if(got==nbytes) {
                        user.setAuxStatus(Octet.EOM_CNT);
                    } else if(got>=1){
                        if(eosLenInput>0) {
                            if(eosLenInput==1) {
                                if(data[got-1]==eosInput[0]) {
                                    got -= 1;
                                    user.setAuxStatus(Octet.EOM_EOS);
                                }
                            } else if(got>=2){
                                if(data[got-2]==eosInput[0] && data[got-1]==eosInput[1]) {
                                    got -= 2;
                                    user.setAuxStatus(Octet.EOM_EOS);
                                }
                            }
                        }
                    }
                    user.setInt(got);
                    super.interruptOccured(data, got);
                    trace.printIO(Trace.DRIVER ,data,got,device.getFullName() +  " read");
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
                        trace.print(Trace.ERROR ,device.getFullName() + " writeRaw but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    int numout = nbytes;
                    if(eosLenOutput>0 && nbytes < (data.length + eosLenOutput)) {
                        if(eosLenOutput==1) {
                            data[nbytes] = eosOutput[0];
                            numout += 1;
                        } else {
                            data[nbytes] = eosOutput[0];
                            data[nbytes+1] = eosOutput[1];
                            numout += 2;
                        }
                    }
                    try {
                        out.write(data, 0, numout);
                    } catch (IOException e) {
                        user.setMessage("IOException " + e.getMessage());
                        return Status.error;
                    }
                    user.setInt(nbytes);
                    trace.printIO(Trace.DRIVER ,data,numout,device.getFullName() + " writeRaw");
                    super.interruptOccured(buffer, nbytes);
                    return Status.success;
                }
            }
        }
    }
}

