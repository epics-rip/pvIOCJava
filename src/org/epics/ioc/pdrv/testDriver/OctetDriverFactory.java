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
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDouble;
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
        byte[] eosInput = getEOS(pvStructure.getStringField("eosInput"));
        byte[] eosOutput = getEOS(pvStructure.getStringField("eosOutput"));
        new OctetDriver(portName,autoConnect,canBlock,priority,delay,eosInput,eosOutput);
    }

    static byte[] getEOS(PVString pvString) {
        if(pvString==null) return new byte[0];
        String value = pvString.get();
        if(value==null) return new byte[0];
        if(value.equals("CR")) {
            return new byte[]{'\r'};
        } else if(value.equals("LF")) {
            return new byte[]{'\n'};
        } else if(value.equals("CRLF")) {
            return new byte[]{'\r','\n'};
        } else if(value.equals("LFCR")) {
            return new byte[]{'\n','\r'};
        }
        pvString.message("unsupported End Of String", MessageType.error);
        return new byte[0];
    }

    static private class OctetDriver implements PortDriver {
        private double delay;
        private String portName;
        private Port port;
        private Trace trace;
        private byte[] eosInput = null;
        private int eosInputLength = 0;
        private byte[] eosOutput = null;
        private int eosOutputLength = 0;

        private OctetDriver(String portName,
                boolean autoConnect,boolean canBlock,ThreadPriority priority,double delay,
                byte[] eosInput,byte[] eosOutput)
        {
            this.delay = delay;
            this.portName = portName;
            this.eosInput = eosInput;
            this.eosOutput = eosOutput;
            eosInputLength = eosInput.length;
            eosOutputLength = eosOutput.length;
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

        private class EchoDevice implements DeviceDriver {   
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
                    user.setAuxStatus(eosInputLength);
                    eos[0] = eosInput[0];
                    eos[1] = eosInput[1];
                    trace.printIO(Trace.FLOW ,
                            eosInput,eosInputLength,
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
                    user.setAuxStatus(eosOutputLength);
                    eos[0] = eosOutput[0];
                    eos[1] = eosOutput[1];
                    trace.printIO(Trace.FLOW ,
                            eosOutput,eosOutputLength,
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
                    int auxStatus = Octet.EOM_END;
                    int nForUser = size;
                    int nRead = size;
                    if(nbytes<size){
                        nRead = nForUser = nbytes;
                        auxStatus = Octet.EOM_CNT;
                    }
                    if(eosInputLength>0) {
                        again:
                        for(int i=0; i<nRead-eosInputLength; i++) {
                            for(int j=0; j<eosInputLength; j++) {
                                if(buffer[i+j+1]!=eosInput[j]) {
                                    continue again;
                                }
                            }
                            nForUser = i + 1;
                            nRead = nForUser + eosInputLength;
                            auxStatus = Octet.EOM_EOS;
                        }
                    }
                    Status status = Status.success;
                    for(int i=0; i<nRead; i++) {
                        data[i] = buffer[i];
                    }
                    user.setAuxStatus(auxStatus);
                    user.setInt(nForUser);
                    trace.printIO(Trace.DRIVER ,data,nRead,device.getFullName() +  " read");
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
                    eosInputLength = eosLen;
                    for(int i=0; i<eosLen; i++) eosInput[i] = eos[i];
                    trace.printIO(Trace.FLOW ,eosInput,eosInputLength,device.getFullName() + " setInputEos");
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
                    eosOutputLength = eosLen;
                    for(int i=0; i<eosLen; i++) eosOutput[i] = eos[i];
                    trace.printIO(Trace.FLOW ,eosOutput,eosOutputLength,device.getFullName() + " setOutputEos");
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
                    if(nbytes>(data.length + eosOutputLength)) {
                        String message = " data buffer not large enough for end of string";
                        trace.print(Trace.ERROR ,device.getFullName() + message);
                        user.setMessage(message);
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
                    for(int i=0; i<eosOutputLength; i++) {
                        data[nbytes+i] = eosOutput[i];
                    }
                    int numout = nbytes + eosOutputLength;
                    
                    Status status = Status.success;
                    int n = numout;
                    int maxbytes = BUFFERSIZE;
                    if(n>maxbytes) {
                        status = Status.overflow;
                        n = nbytes = maxbytes;
                    }
                    size = n;
                    for(int i=0; i<n; i++) {
                        buffer[i] = data[i];
                    }
                    user.setInt(nbytes);
                    trace.printIO(Trace.DRIVER ,data,n,device.getFullName() + " write");
                    super.interruptOccured(buffer, nbytes);
                    return status;
                }
            }
        }
    }
}

