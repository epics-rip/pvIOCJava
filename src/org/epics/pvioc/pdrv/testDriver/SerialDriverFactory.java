/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.testDriver;

import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.DeviceDriver;
import org.epics.pvioc.pdrv.Factory;
import org.epics.pvioc.pdrv.Port;
import org.epics.pvioc.pdrv.PortDriver;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.User;
import org.epics.pvioc.pdrv.interfaces.AbstractSerial;
import org.epics.pvioc.pdrv.interfaces.Serial;
/**
 * The factory for serialDriver.
 * serialDriver is a portDriver for testing PDRV components.
 * It requires the serialDriver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>multiDevice<br/>
 *       If true serialDriver supports multiple devices.
 *       If false it supports a single device.
 *     </li>
 *     <li>delay<br/>
 *       If 0.0 then serialDriver is synchronous.
 *       If > 0.0 serialDriver is asynchronous and delays delay seconds after each read/write request.
 *      </li>
 * </ul>
 * serialDriver implements interface serial by keeping an internal buffer.
 * A read request returns the value written by the previous write and
 * and also empties the buffer.
 *     
 * @author mrk
 *
 */
public class SerialDriverFactory {

    /**
     * Create a new instance of serialDriver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure serialDriver.
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
        new SerialDriver(portName,autoConnect,canBlock,priority,delay,eosInput,eosOutput);
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

    static private class SerialDriver implements PortDriver {
        private double delay;
        private long milliseconds;
        private String portName;
        private Port port;
        private Trace trace;
        private byte[] eosInput = null;
        private int eosInputLength = 0;
        private byte[] eosOutput = null;
        private int eosOutputLength = 0;

        private SerialDriver(String portName,
                boolean autoConnect,boolean canBlock,ThreadPriority priority,double delay,
                byte[] eosInput,byte[] eosOutput)
        {
            this.delay = delay;
            milliseconds = (long)(delay * 1000.0);
            this.portName = portName;
            this.eosInput = eosInput;
            this.eosOutput = eosOutput;
            eosInputLength = eosInput.length;
            eosOutputLength = eosOutput.length;
            port = Factory.createPort(portName, this, "serialDriver",
                    canBlock, autoConnect, priority);
            trace = port.getTrace();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#report(boolean, int)
         */
        public String report(int details) {
            if(details==0) return null;
            return "delay " + delay;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#connect(org.epics.pvioc.pdrv.User)
         */
        public Status connect(User user) {
            trace.print(Trace.FLOW ,portName + " connect");
            if(port.isConnected()) {
                user.setMessage("already connected");
                trace.print(Trace.ERROR ,portName + " already connected");
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

            EchoDevice echoDevice = new EchoDevice(delay);
            Device device = port.createDevice(echoDevice, deviceName);
            echoDevice.init(device);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.PortDriver#disconnect(org.epics.pvioc.pdrv.User)
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
                new EchoSerial(device);
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

            private class EchoSerial extends  AbstractSerial{
                private static final int BUFFERSIZE = 4096;
                private byte[] buffer = new byte[BUFFERSIZE];
                private int size;

                private EchoSerial(Device device) {
                    super(device);
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#flush(org.epics.pvioc.pdrv.User)
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
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#getInputEos(org.epics.pvioc.pdrv.User, byte[])
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
                    if((trace.getMask()&Trace.FLOW)!=0) {
                        trace.printIO(Trace.FLOW ,
                            eosInput,eosInputLength,
                            device.getFullName() +  " getInputEos");
                    }
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#getOutputEos(org.epics.pvioc.pdrv.User, byte[])
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
                    if((trace.getMask()&Trace.FLOW)!=0) {
                        trace.printIO(Trace.FLOW ,
                            eosOutput,eosOutputLength,
                            device.getFullName() + " getOutputEos");
                    }
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#read(org.epics.pvioc.pdrv.User, byte[], int)
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
                    int auxStatus = Serial.EOM_END;
                    int nForUser = size;
                    int nRead = size;
                    if(nbytes<size){
                        nRead = nForUser = nbytes;
                        auxStatus = Serial.EOM_CNT;
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
                            auxStatus = Serial.EOM_EOS;
                        }
                    }
                    Status status = Status.success;
                    for(int i=0; i<nRead; i++) {
                        data[i] = buffer[i];
                    }
                    user.setAuxStatus(auxStatus);
                    user.setInt(nForUser);
                    if(((trace.getMask()&Trace.DRIVER)!=0) && trace.getIOMask()!=0) {
                        trace.printIO(Trace.DRIVER ,data,nRead,device.getFullName() +  " read");
                    }
                    size = 0;
                    return status;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#setInputEos(org.epics.pvioc.pdrv.User, byte[], int)
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
                    if(((trace.getMask()&Trace.DRIVER)!=0) && trace.getIOMask()!=0) {
                        trace.printIO(Trace.DRIVER ,eosInput,eosInputLength,device.getFullName() + " setInputEos");
                    }
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#setOutputEos(org.epics.pvioc.pdrv.User, byte[], int)
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
                    if(((trace.getMask()&Trace.DRIVER)!=0) && trace.getIOMask()!=0) {
                        trace.printIO(Trace.DRIVER ,eosOutput,eosOutputLength,device.getFullName() + " setOutputEos");
                    }
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.pvioc.pdrv.interfaces.AbstractSerial#write(org.epics.pvioc.pdrv.User, byte[], int)
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
                    if(((trace.getMask()&Trace.DRIVER)!=0) && trace.getIOMask()!=0) {
                        trace.printIO(Trace.DRIVER ,data,n,device.getFullName() + " write");
                    }
                    super.interruptOccurred(buffer, nbytes);
                    return status;
                }
            }
        }
    }
}

