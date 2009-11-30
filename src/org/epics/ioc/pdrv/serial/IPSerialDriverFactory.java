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
import org.epics.ioc.pdrv.interfaces.AbstractSerial;
import org.epics.ioc.pdrv.interfaces.Serial;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
/**
 * Internet Protocol Serial Driver.
 * This is a driver that connects via TCP to a network to serial server.
 * The network server must just pass the octet stream to a serial port
 * and pass back whatever is received from the serial port.
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
        byte[] eosInput = getEOS(pvStructure.getStringField("eosInput"));
        byte[] eosOutput = getEOS(pvStructure.getStringField("eosOutput"));
        new IPSerialDriver(portName,autoConnect,priority,pvHost.get(),pvPort.get(),eosInput,eosOutput);
    }
    
    static byte[] getEOS(PVString pvString) {
        if(pvString==null) return new byte[0];
        String value = pvString.get();
        if(value==null) return new byte[0];
        if(value.equals("CR")) {
            return new byte[]{'\r'};
        } else if(value.equals("NL")) {
            return new byte[]{'\n'};
        } else if(value.equals("CRNL")) {
            return new byte[]{'\r','\n'};
        } else if(value.equals("NLCR")) {
            return new byte[]{'\n','\r'};
        }
        pvString.message("unsupported End Of String", MessageType.error);
        return new byte[0];
    }
    
    static private class IPSerialDriver implements PortDriver {
        private String host;
        private int ipport;
        private String portName;
        private Port port;
        private Trace trace;
        private Socket server = null;
        private byte[] eosInput = null;
        private int eosInputLength = 0;
        private byte[] eosOutput = null;
        private int eosOutputLength = 0;
        
        private IPSerialDriver(String portName,
            boolean autoConnect,ThreadPriority priority,String host, int ipport,
            byte[] eosInput,byte[] eosOutput)
        {
            this.host = host;
            this.ipport = ipport;
            this.portName = portName;
            this.eosInput = eosInput;
            this.eosOutput = eosOutput;
            eosInputLength = eosInput.length;
            eosOutputLength = eosOutput.length;
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

            private void init(Device device) {
               
                this.device = device;
                trace = device.getTrace();
                new IPSerialOctet(device);
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

            private class IPSerialOctet extends  AbstractSerial{
                private byte[] buffer = new byte[80];

                private IPSerialOctet(Device device) {
                    super(device);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#flush(org.epics.ioc.pdrv.User)
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
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#getInputEos(org.epics.ioc.pdrv.User, byte[])
                 */
                public Status getInputEos(User user, byte[] eos) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() +  " getInputEos but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    int length = eosInputLength;
                    user.setAuxStatus(length);
                    if(eos==null || eos.length<eosInput.length) {
                        user.setMessage("eos.length < eosInput.length");
                        return Status.error;
                    }
                    for(int i=0; i<eosInputLength; i++) eos[i] = eosInput[i];
                    trace.printIO(Trace.FLOW ,
                            eosInput,length,
                            device.getFullName() +  " getInputEos");
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#getOutputEos(org.epics.ioc.pdrv.User, byte[])
                 */
                public Status getOutputEos(User user, byte[] eos) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR ,device.getFullName() +  " getOutputEos but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    int length = eosOutputLength;
                    user.setAuxStatus(length);
                    if(eos==null || eos.length<eosOutput.length) {
                        user.setMessage("eos.length < eosOutput.length");
                        return Status.error;
                    }
                    eos[0] = eosOutput[0];
                    eos[1] = eosOutput[1];
                    trace.printIO(Trace.FLOW ,
                            eosOutput,length,
                            device.getFullName() +  " getOutputEos");
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#read(org.epics.ioc.pdrv.User, byte[], int)
                 */
                public Status read(User user, byte[] data, int nbytes) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR ,device.getFullName() +  " read but not connected");
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
                    int numForUser = got;
                    user.setAuxStatus(Serial.EOM_END);
                    if(got==nbytes) {
                        user.setAuxStatus(Serial.EOM_CNT);
                    } else if(got>=1){
                        if(eosInputLength>0) {
                            if(eosInputLength==1) {
                                if(data[got-1]==eosInput[0]) {
                                    numForUser -= 1;
                                    user.setAuxStatus(Serial.EOM_EOS);
                                }
                            } else if(got>=2){
                                if(data[got-2]==eosInput[0] && data[got-1]==eosInput[1]) {
                                    numForUser -= 2;
                                    user.setAuxStatus(Serial.EOM_EOS);
                                }
                            }
                        }
                    }
                    user.setInt(numForUser);
                    super.interruptOccurred(data, got);
                    trace.printIO(Trace.DRIVER ,data,got,device.getFullName() +  " read");
                    return status;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#setInputEos(org.epics.ioc.pdrv.User, byte[], int)
                 */
                public Status setInputEos(User user, byte[] eos, int eosLen) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR ,device.getFullName() + " setInputEos but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    if(eosLen<0) {
                        user.setMessage("illegal eosLen");
                        return Status.error;
                    }
                    if(eosLen>eosInput.length) {
                        eosInput = new byte[eosLen];
                    }
                    eosInputLength = eosLen;
                    for(int i=0; i<eosLen; i++) eosInput[i] = eos[i];
                    trace.printIO(Trace.FLOW ,eosInput,eosLen,device.getFullName() + " setInputEos");
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#setOutputEos(org.epics.ioc.pdrv.User, byte[], int)
                 */
                public Status setOutputEos(User user, byte[] eos, int eosLen) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR ,device.getFullName() + " setOutputEos but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    if(eosLen<0) {
                        user.setMessage("illegal eosLen");
                        return Status.error;
                    }
                    if(eosLen>eosOutput.length) {
                        eosOutput = new byte[eosLen];
                    }
                    eosOutputLength = eosLen;
                    for(int i=0; i<eosLen; i++) eosOutput[i] = eos[i];
                    trace.printIO(Trace.FLOW ,eosOutput,eosLen,device.getFullName() + " setInputEos");
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractSerial#write(org.epics.ioc.pdrv.User, byte[], int)
                 */
                public Status write(User user, byte[] data, int nbytes) {
                    if(!device.isConnected()) {
                        String message = " write but not connected";
                        trace.print(Trace.ERROR ,device.getFullName() + message);
                        user.setMessage(message);
                        return Status.error;
                    }
                    int numout = nbytes;
                    if(nbytes > (data.length + eosOutputLength)) {
                        String message = " data buffer not large enough for end of string";
                        trace.print(Trace.ERROR ,device.getFullName() + message);
                        user.setMessage(message);
                        return Status.error;
                    }
                    for(int i=0; i<eosOutputLength; i++) {
                        data[nbytes+i] = eosOutput[i];
                    }
                    numout = nbytes + eosOutputLength;
                    try {
                        out.write(data, 0, numout);
                    } catch (IOException e) {
                        user.setMessage("IOException " + e.getMessage());
                        return Status.error;
                    }
                    user.setInt(nbytes);
                    trace.printIO(Trace.DRIVER ,data,numout,device.getFullName() + " write");
                    super.interruptOccurred(buffer, nbytes);
                    return Status.success;
                }
            }
        }
    }
}

