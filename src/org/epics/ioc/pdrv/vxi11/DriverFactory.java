/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.vxi11;

import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.DeviceDriver;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.PortDriver;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pdrv.interfaces.*;
import org.epics.ioc.pdrv.interfaces.GpibController;
import org.epics.ioc.pdrv.interfaces.GpibDevice;
import org.epics.ioc.pdrv.interfaces.GpibSrqHandler;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.ScanPriority;
/**
 * The factory for vxi11Driver.
 * vxi11Driver is a portDriver for communication with VXI11 devices.
 * It requires the vxi11Driver structure, which holds the following configuration parameters:
 * <ul>
 *    <li>hostName<br/>
 *       The hostName or IP address on the device.
 *     </li>
 *     <li>vxiName<br/>
 *       The vxiName. Some examples are:
 *       <ul>
 *          <li>E2050 - hpib</li>
 *          <li>E5810 - gpib0</li>
 *          <li>TDS5054B - inst0</li>
 *          <li>E5810 serial port = COM1,1</li>
 *       </ul>
 *      </li>
 *      <li>recoverWithIFC - false or true</li>
 * </ul>
 *     
 * @author mrk
 *
 */
public class DriverFactory {

    /**
     * Create a new instance of vxi11Driver.
     * @param portName The portName.
     * @param autoConnect Initial value for autoConnect.
     * @param priority The thread priority if asynchronous, i.e. delay > 0.0.
     * @param pvStructure The interface for structure vxi11Driver.
     */
    static public void create(
            String portName,boolean autoConnect,ScanPriority priority,PVStructure pvStructure)
    {
        PVString pvHostName = pvStructure.getStringField("hostName");
        if(pvHostName==null) {
            throw new IllegalStateException("field hostName not found");
        }
        PVString pvVxiName = pvStructure.getStringField("vxiName");
        if(pvVxiName==null) {
            throw new IllegalStateException("field vxiName not found");
        }
        PVInt pvLockTimeout = pvStructure.getIntField("lockTimeout");
        if(pvLockTimeout==null) {
            throw new IllegalStateException("field lockTimeout not found");
        }
        new PortDriverImpl(portName,autoConnect,priority,pvStructure,
                pvHostName.get(),pvVxiName.get(),pvLockTimeout.get());
    }

    static private class PortDriverImpl implements PortDriver {

        private String portName;
        private int lockTimeout = 1000;
        private VXI11Controller vxiController;
        private Port port = null;
        private Trace trace = null;;
        private User user = null;
        private VXI11User vxiUser = VXI11UserFactory.create();


        private PortDriverImpl(String portName,boolean autoConnect,ScanPriority priority,PVStructure pvStructure,
                String hostName,String vxiName,int lockTimeout)
        {
            this.portName = portName;
            this.lockTimeout = lockTimeout;
            vxiController = VXI11Factory.create(hostName, vxiName);
            port = Factory.createPort(portName, this, "VXI11",true, autoConnect, priority);
            trace = port.getTrace();
            user = Factory.createUser(null);
            user.connectPort(portName);
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
            Status status = vxiController.connect(vxiUser);
            if(status!=Status.success) {
                String message = vxiUser.getError().name() + " " + vxiUser.getString();
                user.setMessage(message);
                trace.print(Trace.ERROR ,portName + message);
                return Status.error;
            }
            vxiController.setLockTimeout(lockTimeout);
            port.exceptionConnect();
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.PortDriver#createDevice(org.epics.ioc.pdrv.User, int)
         */
        public Device createDevice(User user, String deviceName) {
            DeviceDriverImpl deviceImpl = new DeviceDriverImpl();
            Device device = port.createDevice(deviceImpl, deviceName);
            deviceImpl.init(device);
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

        private class DeviceDriverImpl implements DeviceDriver {   
            private Device device = null;
            private VXI11Device vxiDevice = null;
            private int inputTermChar = -1;
            private int outputTermChar = -1;
            

            private void init(Device device) { 
                this.device = device;
                String deviceName = device.getDeviceName();
                int pad = -1;
                int sad = -1;
                int indexComma = deviceName.indexOf(",");
                if(indexComma>0) {
                    pad = Integer.parseInt(deviceName.substring(0, indexComma));
                    sad = Integer.parseInt(deviceName.substring(indexComma));
                } else {
                    try {
                        pad = Integer.parseInt(deviceName);
                    } catch (NumberFormatException e) {
                        pad = -1;
                    }
                }
                vxiDevice = vxiController.createDevice(pad, sad);
                new OctetImpl(device);
                if(pad==-1) {
                    new GpibControllerImpl(device);
                } else {
                    new GpibDeviceImpl(device);
                }
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
            
            private Status returnStatus(User user,Status status,String traceMessage) {
                if(status!=Status.success) {
                    String message = vxiUser.getError().name() + " " + vxiUser.getString();
                    user.setAlarm(AlarmSeverity.major, message);
                    trace.print(Trace.ERROR ,device.getFullName() + " " + traceMessage + " " + user.getAlarmMessage());
                } else {
                    trace.print(Trace.FLOW ,device.getFullName() + " " + traceMessage);
                }
                vxiUser.clear();
                return status;
            }

            private class OctetImpl extends  AbstractOctet{

                private OctetImpl(Device device) {
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
                    trace.print(Trace.FLOW ,"flush is noop");
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
                    int chr = inputTermChar;
                    if(chr==-1) {
                        user.setAuxStatus(0);
                    } else {
                        user.setAuxStatus(1);
                        eos[0] = (byte)chr;
                    }
                    trace.print(Trace.FLOW ,device.getFullName() +  " getInputEos ");
                    return Status.success;
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.AbstractOctet#getOutputEos(org.epics.ioc.pdrv.User, byte[])
                 */
                public Status getOutputEos(User user, byte[] eos) {
                    if(!device.isConnected()) {
                        trace.print(Trace.ERROR,device.getFullName() +  " getOutoutEos but not connected");
                        user.setMessage("not connected");
                        return Status.error;
                    }
                    int chr = outputTermChar;
                    if(chr==-1) {
                        user.setAuxStatus(0);
                    } else {
                        user.setAuxStatus(1);
                        eos[0] = (byte)chr;
                    }
                    trace.print(Trace.FLOW ,device.getFullName() +  " getOutputEos ");
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
                    int msec = (int)(timeout*1000.0);
                    vxiController.setIoTimeout(msec);
                    vxiDevice.setTermChar(vxiUser, inputTermChar);
                    Status status = vxiDevice.read(vxiUser, data, nbytes);
                    if(status==Status.success) {
                        user.setAuxStatus(vxiUser.getReason());
                        user.setInt(vxiUser.getInt());
                        trace.printIO(Trace.DRIVER ,data,user.getInt(),device.getFullName() + " read");
                    }
                    return returnStatus(user,status,"read");
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
                    int msec = (int)(timeout*1000.0);
                    vxiController.setIoTimeout(msec);
                    vxiDevice.setTermChar(vxiUser, -1);
                    Status status = vxiDevice.read(vxiUser, data, nbytes);
                    if(status==Status.success) {
                        user.setAuxStatus(vxiUser.getReason());
                        user.setInt(vxiUser.getInt());
                        trace.printIO(Trace.DRIVER ,data,user.getInt(),device.getFullName() + " readRaw");
                    }
                    return returnStatus(user,status,"read");
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
                    Status status = Status.success;
                    if(eosLen<0 || eosLen>1) {
                        user.setMessage("illegal eosLen");
                        status = Status.error;
                    } else {
                        if(eosLen==0) {
                            inputTermChar = -1;
                        } else {
                            inputTermChar = eos[0];
                        }
                        status = vxiDevice.setTermChar(vxiUser, inputTermChar);
                    }
                    return returnStatus(user,status,"setInputEos");
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
                    Status status = Status.success;
                    if(eosLen<0 || eosLen>1) {
                        user.setMessage("illegal eosLen");
                        status = Status.error;
                    } else {
                        if(eosLen==0) {
                            outputTermChar = -1;
                        } else {
                            outputTermChar = eos[0];
                        }
                        status = vxiDevice.setTermChar(vxiUser, outputTermChar);
                    }
                    return returnStatus(user,status,"setInputEos");
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
                    byte[] buffer = new byte[nbytes];
                    System.arraycopy(data, 0, buffer, 0, nbytes);
                    trace.printIO(Trace.DRIVER ,data,nbytes,device.getFullName() + " write");
                    double timeout = user.getTimeout();
                    int msec = (int)(timeout*1000.0);
                    vxiController.setIoTimeout(msec);
                    vxiDevice.setTermChar(vxiUser, outputTermChar);
                    Status status = vxiDevice.write(vxiUser, data);
                    if(status==Status.success) {
                        super.interruptOccured(buffer, nbytes);
                    }
                    return returnStatus(user,status,"write");
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
                    byte[] buffer = new byte[nbytes];
                    System.arraycopy(data, 0, buffer, 0, nbytes);
                    trace.printIO(Trace.DRIVER ,data,nbytes,device.getFullName() + " write");
                    double timeout = user.getTimeout();
                    int msec = (int)(timeout*1000.0);
                    vxiController.setIoTimeout(msec);
                    vxiDevice.setTermChar(vxiUser,-1);
                    Status status = vxiDevice.write(vxiUser, data);
                    if(status==Status.success) {
                        super.interruptOccured(buffer, nbytes);
                    }
                    return returnStatus(user,status,"write");
                }
            }
            private class GpibControllerImpl extends AbstractInterface implements GpibController, VXI11SrqHandler {
                
                GpibControllerImpl(Device device) {
                    super(device,"gpibController");
                }
                GpibSrqHandler srqHandler = null;
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#enableSRQ(org.epics.ioc.pdrv.User, boolean)
                 */
                public Status enableSRQ(User user, boolean state) {
                    Status status = vxiController.enableSRQ(vxiUser, state);
                    return returnStatus(user,status,"enableSRQ");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#getBusAddress(org.epics.ioc.pdrv.User)
                 */
                public Status getBusAddress(User user) {
                    Status status = vxiController.getBusAddress(vxiUser);
                    user.setInt(vxiUser.getInt());
                    return returnStatus(user,status,"getBusAddress");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#ifc(org.epics.ioc.pdrv.User)
                 */
                public Status ifc(User user) {
                    Status status = vxiController.ifc(vxiUser);
                    return returnStatus(user,status,"ifc");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isControllerInCharge(org.epics.ioc.pdrv.User)
                 */
                public Status isControllerInCharge(User user) {
                    Status status = vxiController.isControllerInCharge(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isControllerInCharge");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isListener(org.epics.ioc.pdrv.User)
                 */
                public Status isListener(User user) {
                    Status status = vxiController.isListener(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isListener");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isRemote(org.epics.ioc.pdrv.User)
                 */
                public Status isRemote(User user) {
                    Status status = vxiController.isRemote(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isListener");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isSRQ(org.epics.ioc.pdrv.User)
                 */
                public Status isSRQ(User user) {
                    Status status = vxiController.isSRQ(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isSRQ");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isNDAC(org.epics.ioc.pdrv.User)
                 */
                public Status isNDAC(User user) {
                    Status status = vxiController.isNDAC(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isNDAC");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isSystemController(org.epics.ioc.pdrv.User)
                 */
                public Status isSystemController(User user) {
                    Status status = vxiController.isSystemController(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isSystemController");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#isTalker(org.epics.ioc.pdrv.User)
                 */
                public Status isTalker(User user) {
                    Status status = vxiController.isTalker(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"isTalker");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#passControl(org.epics.ioc.pdrv.User, int)
                 */
                public Status passControl(User user, int addr) {
                    Status status = vxiController.passControl(vxiUser,addr);
                    return returnStatus(user,status,"passControl");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#registerSrqHandler(org.epics.ioc.pdrv.interfaces.GpibSrqHandler)
                 */
                public void registerSrqHandler(GpibSrqHandler srqHandler) {
                    this.srqHandler = srqHandler;
                    vxiController.registerSrqHandler(this);
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#sendCommand(org.epics.ioc.pdrv.User, byte[])
                 */
                public Status sendCommand(User user, byte[] data) {
                    Status status = vxiController.sendCommand(vxiUser,data);
                    return returnStatus(user,status,"sendCommand");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#setATN(org.epics.ioc.pdrv.User, boolean)
                 */
                public Status setATN(User user, boolean state) {
                    Status status = vxiController.setATN(vxiUser,state);
                    return returnStatus(user,status,"setATN");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#setBusAddress(org.epics.ioc.pdrv.User, int)
                 */
                public Status setBusAddress(User user,int address) {
                    Status status = vxiController.setBusAddress(vxiUser,address);
                    return returnStatus(user,status,"setBusAddress");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibController#setREN(org.epics.ioc.pdrv.User, boolean)
                 */
                public Status setREN(User user, boolean state) {
                    Status status = vxiController.setREN(vxiUser,state);
                    return returnStatus(user,status,"setREN");
                }
                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.vxi11.VXI11SrqHandler#srqHandler(org.epics.ioc.pdrv.vxi11.VXI11Controller)
                 */
                public void srqHandler(VXI11Controller controller) {
                    srqHandler.srqHandler(this);
                }

            }
            
            private class GpibDeviceImpl extends AbstractInterface implements GpibDevice {
                GpibDeviceImpl(Device device) {
                    super(device,"gpibDevice");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#clear(org.epics.ioc.pdrv.User)
                 */
                public Status clear(User user) {
                    Status status = vxiDevice.clear(vxiUser);
                    return returnStatus(user,status,"clear");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#getEOI()
                 */
                public boolean getEOI() {
                    return vxiDevice.getEOI();
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#getStatusByte(org.epics.ioc.pdrv.User)
                 */
                public Status getStatusByte(User user) {
                    Status status = vxiDevice.getStatusByte(vxiUser);
                    user.setBoolean(vxiUser.getBoolean());
                    return returnStatus(user,status,"getStatusByte");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#local(org.epics.ioc.pdrv.User)
                 */
                public Status local(User user) {
                    Status status = vxiDevice.local(vxiUser);
                    return returnStatus(user,status,"local");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#lock(org.epics.ioc.pdrv.User)
                 */
                public Status lock(User user) {
                    Status status = vxiDevice.lock(vxiUser);
                    return returnStatus(user,status,"lock");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#remote(org.epics.ioc.pdrv.User)
                 */
                public Status remote(User user) {
                    Status status = vxiDevice.remote(vxiUser);
                    return returnStatus(user,status,"remote");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#setEOI(boolean)
                 */
                public void setEOI(boolean eoi) {
                    vxiDevice.setEOI(eoi);
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#trigger(org.epics.ioc.pdrv.User)
                 */
                public Status trigger(User user) {
                    Status status = vxiDevice.trigger(vxiUser);
                    return returnStatus(user,status,"trigger");
                }

                /* (non-Javadoc)
                 * @see org.epics.ioc.pdrv.interfaces.GpibDevice#unlock(org.epics.ioc.pdrv.User)
                 */
                public Status unlock(User user) {
                    Status status = vxiDevice.unlock(vxiUser);
                    return returnStatus(user,status,"unlock");
                }

            }
        }
    }
}
