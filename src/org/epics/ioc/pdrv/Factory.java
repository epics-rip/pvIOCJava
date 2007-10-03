/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.util.*;

/**
 * Factory for creating PDRV (Port Driver) objects.
 * @author mrk
 *
 */
public class Factory {
    /**
     * Create a User, which is the "handle" for communicating with port and device drivers.
     * @param queueRequestCallback The user callback for port.queueRequest and device.queueRequest.
     * @return The interface to a User object.
     */
    public static User createUser(QueueRequestCallback queueRequestCallback) {
        return new UserImpl(queueRequestCallback);
    }
    
    /**
     * Create a port.
     * This is called by a portDriver.
     * @param portName The portName.
     * @param portDriver The portDriver.
     * @param driverName The driver name.
     * @param isMultiDevicePort Does this port support multiple devices?
     * @param canBlock Can this port block while performing I/O.
     * @param autoConnect Initial state for autoConnect for the port and its devices.
     * @param priority If the port can block this is the priority for the portThread.
     * @return The interface for the Port instance.
     */
    public static Port createPort(String portName,PortDriver portDriver,String driverName,
            boolean isMultiDevicePort,boolean canBlock,
            boolean autoConnect,ScanPriority priority)
    { 
        PortImpl port = new PortImpl(
                portName,portDriver,driverName,isMultiDevicePort,canBlock,autoConnect,priority);
        globalLock.lock();
        try {
            if(!portList.add(port)) {
                throw new IllegalStateException("port already exists"); 
            }
            return port;
        } finally {
            globalLock.unlock();
        }
        
    }
    /**
     * Get an array of the portNames.
     * @return The array.
     */
    public static String[] getPortNames() {
        globalLock.lock();
        try {
            String[] portNames = new String[portList.size()];
            ListIterator<PortImpl> iter = portList.listIterator();
            int index = 0;
            while(iter.hasNext()) {
                Port port = iter.next();
                portNames[index++] = port.getPortName();
            }
            return portNames;
        } finally {
            globalLock.unlock();
        }
    }
    
    public static Port getPort(String portName) {
        for(Port port : portList) {
            if(port.getPortName().equals(portName)) return port;
        }
        return null;
    }
    
    private static ReentrantLock globalLock = new ReentrantLock();
    private static List<PortImpl> portList = new LinkedList<PortImpl>();
    
    private static class UserImpl implements User {
        
        private UserImpl(QueueRequestCallback queueRequestCallback) {
            this.queueRequestCallback = queueRequestCallback;
        }

        private QueueRequestCallback queueRequestCallback = null;
        private PortImpl port = null;
        private DeviceImpl device = null;
        private boolean isQueued = false;
        
        private Object userPvt;
        private Object portDriverPvt;
        private Object deviceDriverPvt;
        private int reason;
        private double timeout;
        
        private int auxStatus;
        private String message;
        private int intValue;
        private double doubleValue;
        private String stringValue;
        
        private QueueRequestCallback getQueueRequestCallback() {
            return queueRequestCallback;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#duplicateUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.QueueRequestCallback)
         */
        public User duplicateUser(QueueRequestCallback queueRequestCallback) {
            UserImpl newUser = new UserImpl(queueRequestCallback);
            newUser.port = port;
            newUser.device = device;
            newUser.reason = reason;
            newUser.timeout = timeout;
            return newUser;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#connectPort(java.lang.String)
         */
        public Port connectPort(String portName) {
            if(port!=null) {
                setMessage("already connected");
                return null;
            }
            globalLock.lock();
            try {               
                ListIterator<PortImpl> iter = portList.listIterator();
                while(iter.hasNext()) {
                    PortImpl port =iter.next();
                    if(port.getPortName().equals(portName)) {
                        this.port = port;
                        return port;
                    }
                }
                if(port==null) setMessage("portName " + portName + " not registered");
            } finally {
                globalLock.unlock();
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#disconnectPort()
         */
        public void disconnectPort() {
            disconnectDevice();
            port = null;
            portDriverPvt = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getPort()
         */
        public Port getPort() {
            return port;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#connectDevice(int)
         */
        public Device connectDevice(int addr) {
            if(device!=null) {
                setMessage("already connected");
                return null;
            }
            if(port==null) {
                setMessage("not connected to a port");
                return null;
            }
            device = (DeviceImpl)port.getDevice(this, addr);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#disconnectDevice()
         */
        public void disconnectDevice() {
            if(device==null) return;
            cancelRequest();
            device = null;
            deviceDriverPvt = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getDevice()
         */
        public Device getDevice() {
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#queueRequest(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.QueuePriority)
         */
        public void queueRequest(QueuePriority queuePriority) {            
            Trace trace = port.getTrace();
            String portName = port.getPortName();
            trace.print(Trace.FLOW, "%s queueRequest",portName);
            if(queueRequestCallback==null) {
                throw new IllegalStateException("queueRequestCallback is null");
            }
            PortThread portThread = port.portThread;
            if(portThread!=null) {
                portThread.queueRequest(this,queuePriority);
            } else {
                Status status = null;
                if(device!=null) {
                    status = device.lockPort(this);
                } else {
                    status = lockPort();
                }
                if(status!=Status.success) {
                    queueRequestCallback.callback(status, this);
                    return;
                }
                try {  
                    trace.print(Trace.FLOW, "%s queueRequest calling queueRequestCallback", portName);
                    queueRequestCallback.callback(Status.success,this);
                } finally {
                    unlockPort();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#cancelRequest(org.epics.ioc.pdrv.User)
         */
        public void cancelRequest() {
            if(port==null) return;
            Trace trace = port.getTrace();
            String portName = port.portName;
            trace.print(Trace.FLOW, "%s cancelRequest", portName);
            PortThread portThread = port.portThread;
            if(portThread!=null) portThread.cancelRequest(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#lockPort(org.epics.ioc.pdrv.User)
         */
        public Status lockPort() {
            if(port==null) {
                setMessage("not connected to a port");
                return Status.error;
            }
            if(device!=null) {
                return device.lockPort(this);
            } else {
                return port.lockPort(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#unlockPort(org.epics.ioc.pdrv.User)
         */
        public void unlockPort() {
            if(port==null) return;
            port.unlockPort(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getAuxStatus()
         */
        public int getAuxStatus() {
            return auxStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getDouble()
         */
        public double getDouble() {
            return doubleValue;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getInt()
         */
        public int getInt() {
            return intValue;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getMessage()
         */
        public String getMessage() {
            return message;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getReason()
         */
        public int getReason() {
            return reason;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getString()
         */
        public String getString() {
            return stringValue;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getTimeout()
         */
        public double getTimeout() {
            return timeout;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getUserPvt()
         */
        public Object getUserPvt() {
            return userPvt;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getDeviceDriverPvt()
         */
        public Object getDeviceDriverPvt() {
            return deviceDriverPvt;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getPortDriverPvt()
         */
        public Object getPortDriverPvt() {
            return portDriverPvt;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setAuxStatus(int)
         */
        public void setAuxStatus(int auxStatus) {
            this.auxStatus = auxStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setDouble(double)
         */
        public void setDouble(double value) {
            doubleValue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setInt(int)
         */
        public void setInt(int value) {
            this.intValue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setMessage(java.lang.String)
         */
        public void setMessage(String message) {
            this.message = message;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setReason(int)
         */
        public void setReason(int reason) {
            this.reason = reason;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setString(java.lang.String)
         */
        public void setString(String value) {
            this.stringValue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setTimeout(double)
         */
        public void setTimeout(double timeout) {
            this.timeout = timeout;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setUserPvt(java.lang.Object)
         */
        public void setUserPvt(Object userPvt) {
            this.userPvt = userPvt;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setDeviceDriverPvt(java.lang.Object)
         */
        public void setDeviceDriverPvt(Object deviceDriverPvt) {
            this.deviceDriverPvt = deviceDriverPvt;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setPortDriverPvt(java.lang.Object)
         */
        public void setPortDriverPvt(Object portDriverPvt) {
            this.portDriverPvt = portDriverPvt;
        }
    }
    
    private static class PortImpl implements Port {
        
        private PortImpl(String portName,PortDriver portDriver,String driverName,
            boolean isMultiDevicePort,boolean canBlock,
            boolean autoConnect,ScanPriority priority)
        {
            this.portName = portName;
            this.portDriver = portDriver;
            this.driverName = driverName;
            this.isMultiDevicePort = isMultiDevicePort;
            this.autoConnect = autoConnect;
            if(canBlock) {
                portThread = new PortThread(this,priority);
            }
        }
        private ReentrantLock portLock = new ReentrantLock();
        private Trace trace = new TraceImpl();
        private String portName;
        private PortDriver portDriver;
        private String driverName;
        private boolean isMultiDevicePort;
        private boolean autoConnect;        
        private PortThread portThread = null;        
        
        private boolean connected = false;
        private boolean enabled = true;
        
        private List<DeviceImpl> deviceList = new LinkedList<DeviceImpl>();
                
        private List<ConnectExceptionListener> exceptionListenerList
            = new LinkedList<ConnectExceptionListener>();
        private List<ConnectExceptionListener> exceptionListenerListNew = null;
        private boolean exceptionActive = true;
        
        private UserImpl lockPortUser = null;
        private ReentrantLock lockPortLock = new ReentrantLock();
        private Condition portUnlock = lockPortLock.newCondition();
        
        private LockPortNotify lockPortNotify = null;   

        
        private static class PortInterface {
            private Interface iface;
            private Interface interposeInterface = null;
            
            private PortInterface(Interface iface) {
                this.iface = iface;
            }
        }
        private List<PortInterface> interfaceList = new LinkedList<PortInterface>();
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#report(boolean, int)
         */
        public String report(boolean reportDevices,int details) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    "port %s driver %s isLocked %b%n",
                    portName,driverName,(lockPortUser==null ? false : true)
            ));
            if(details>0) {
                builder.append(String.format(
                        "    isMultiDevicePort %b autoConnect %b connected %b enabled %b%n",
                        isMultiDevicePort,autoConnect,connected,enabled
                ));
                Interface[] interfaces = getInterfaces();
                int length = interfaces.length;
                if(length>0) {
                    builder.append("    Interfaces: ");
                    for(int i=0; i<interfaces.length; i++) {
                        Interface iface = interfaces[i];
                        builder.append(iface.getInterfaceName() + " ");
                    }
                    builder.append(String.format("%n"));
                }

            }
            if(reportDevices) {
                Device[] devices = getDevices();
                for(int i=0; i<devices.length; i++) {
                    builder.append(devices[i].report(details));
                }
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getDevices()
         */
        public Device[] getDevices() {
            Device[] devices = null;
            portLock.lock();
            try {
                int size = deviceList.size();
                devices = new Device[size];
                for(int i=0; i<size; i++) {
                    devices[i] = deviceList.get(i);
                }
            } finally {
                portLock.unlock();
            }
            return devices;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getInterfaces()
         */
        public Interface[] getInterfaces() {
            Interface[] interfaces = null;
            portLock.lock();
            try {
                int size = interfaceList.size();
                interfaces = new Interface[size];
                for(int i=0; i<size; i++) {
                    interfaces[i] = interfaceList.get(i).iface;
                }
            } finally {
                portLock.unlock();
            }
            return interfaces;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getDriverName()
         */
        public String getDriverName() {
            return driverName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getPortName()
         */
        public String getPortName() {
            return portName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getTrace()
         */
        public Trace getTrace() {
            return trace;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#isMultiDevicePort()
         */
        public boolean isMultiDevicePort() {
            return isMultiDevicePort;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#canBlock()
         */
        public boolean canBlock() {
            return ((portThread==null) ? true : false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#enable(boolean)
         */
        public void enable(boolean trueFalse) {
            trace.print(Trace.FLOW, "%s enable %b", portName,trueFalse);
            portLock.lock();
            try {
                if(enabled==trueFalse) return;
                enabled = trueFalse;
            } finally {
                portLock.unlock();
            }
            raiseException(ConnectException.enable);
            if(trueFalse) scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#autoConnect(boolean)
         */
        public void autoConnect(boolean trueFalse) {
            trace.print(Trace.FLOW, "%s autoConnect %b ", portName,trueFalse);
            portLock.lock();
            try {
                if(autoConnect==trueFalse) return;
                autoConnect = trueFalse;
            } finally {
                portLock.unlock();
            }
            raiseException(ConnectException.autoConnect);
            if(trueFalse) scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getDevice(org.epics.ioc.pdrv.User, int)
         */
        public Device getDevice(User user, int addr) {
            portLock.lock();
            try {
                int size = deviceList.size();
                for(int i=0; i<size; i++) {
                    Device device = deviceList.get(i);
                    if(device.getAddr()==addr) return device;
                }
            } finally {
                portLock.unlock();
            }
            lockPort((UserImpl)user);
            try {
                return portDriver.createDevice(user, addr);
            } finally {
                unlockPort((UserImpl)user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#connect(org.epics.ioc.pdrv.User)
         */
        public Status connect(User theUser) {
            UserImpl user = (UserImpl)theUser;
            lockPortLock.lock();
            try {
                if(lockPortUser!=null) {
                    if(lockPortUser==user) {
                        user.setMessage("Illegal to call connect while owning the port");
                        return Status.error;
                    }
                    try {
                        portUnlock.await();
                    } catch (InterruptedException e){}
                }
                if(connected) {
                    user.setMessage("already connected");
                    return Status.error;
                }
                lockPortUser = user;
            } finally {
                lockPortLock.unlock();
            }
            trace.print(Trace.FLOW, "%s connect", portName);
            Status status = Status.success;
            lockPort(user);
            try {
                status = portDriver.connect(user);
            } finally {
                unlockPort(user);
            }
            if(portThread!=null) portThread.scanQueues();
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#disconnect(org.epics.ioc.pdrv.User)
         */
        public Status disconnect(User theUser) {
            UserImpl user = (UserImpl)theUser;
            lockPortLock.lock();
            try {
                if(lockPortUser!=null) {
                    if(lockPortUser==user) {
                        user.setMessage("Illegal to call disconnect while owning the port");
                        return Status.error;
                    }
                    try {
                        portUnlock.await();
                    } catch (InterruptedException e){}
                }
                if(!connected) {
                    user.setMessage("not connected");
                    return Status.error;
                }
                lockPortUser = user;
            } finally {
                lockPortLock.unlock();
            }
            trace.print(Trace.FLOW, "%s disconnect", portName);
            Status status = Status.success;
            lockPort(user);
            try {
                status = portDriver.disconnect(user);
            } finally {
                unlockPort(user);
            }
            if(portThread!=null) portThread.scanQueues();
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#isConnected()
         */
        public boolean isConnected() {
            return connected;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#isEnabled()
         */
        public boolean isEnabled() {
            return enabled;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#isAutoConnect()
         */
        public boolean isAutoConnect() {
            return autoConnect;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#exceptionListenerAdd(org.epics.ioc.pdrv.ConnectExceptionListener)
         */
        public void exceptionListenerAdd(ConnectExceptionListener connectExceptionListener)
        {
            trace.print(Trace.FLOW, "%s exceptionListenerAdd", portName);
            portLock.lock();
            try {
                if(!exceptionActive) {   
                    if(!exceptionListenerList.add(connectExceptionListener)) {
                        trace.print(Trace.ERROR, "%s exceptionListenerAdd failed", portName);
                    }
                    return;
                }
                if(exceptionListenerListNew==null) {
                    exceptionListenerListNew =
                        new LinkedList<ConnectExceptionListener>(exceptionListenerList);
                }
                if(!exceptionListenerListNew.add(connectExceptionListener)) {
                    trace.print(Trace.ERROR, "%s exceptionListenerAdd failed", portName);
                }                
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#exceptionListenerRemove(org.epics.ioc.pdrv.ConnectExceptionListener)
         */
        public void exceptionListenerRemove(ConnectExceptionListener connectExceptionListener) { 
            trace.print(Trace.FLOW, "%s exceptionListenerRemove", portName);
            portLock.lock();
            try {
                if(!exceptionActive) {   
                    if(!exceptionListenerList.remove(connectExceptionListener)) {
                        trace.print(Trace.ERROR, "%s exceptionListenerRemove failed", portName);
                    }
                    return;
                }
                if(exceptionListenerListNew==null) {
                    exceptionListenerListNew =
                        new LinkedList<ConnectExceptionListener>(exceptionListenerList);
                }
                if(!exceptionListenerListNew.remove(connectExceptionListener)) {
                    trace.print(Trace.ERROR, "%s exceptionListenerRemove failed", portName);
                }                
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#findInterface(org.epics.ioc.pdrv.User, java.lang.String, boolean)
         */
        public Interface findInterface(User user,
                String interfaceName,boolean interposeInterfaceOK)
        {
            portLock.lock();
            try {
                ListIterator<PortInterface> iter = interfaceList.listIterator();
                while(iter.hasNext()) {
                    PortInterface portIface = iter.next();
                    Interface iface = portIface.iface;
                    int compare = interfaceName.compareTo(iface.getInterfaceName());
                    if(compare==0) {
                        if(interposeInterfaceOK && portIface.interposeInterface!=null) {
                            return portIface.interposeInterface;
                        }
                        return iface;
                    }
                    if(compare<0) break;
                }                
            } finally {
                portLock.unlock();
            }
            user.setMessage("interface " + interfaceName + " not found");
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortUser#scanQueue()
         */
        public void scanQueues() {
            if(portThread!=null) portThread.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#registerLockPortNotify(org.epics.ioc.pdrv.LockPortNotify)
         */
        public void registerLockPortNotify(LockPortNotify lockPortNotify) {
            trace.print(Trace.FLOW, "%s registerLockPortNotify", portName);
            portLock.lock();
            try {
                if(this.lockPortNotify!=null) {
                    throw new IllegalStateException("lockPortNotify already registered");
                }
                this.lockPortNotify = lockPortNotify;
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#unregisterLockPortNotify()
         */
        public void unregisterLockPortNotify() {
            trace.print(Trace.FLOW, "%s unregisterLockPortNotify", portName);
            portLock.lock();
            try {
                if(this.lockPortNotify==null) {
                    throw new IllegalStateException("not registered");
                }
                lockPortNotify = null;
            } finally {
                portLock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#createDevice(org.epics.ioc.pdrv.DeviceDriver, int)
         */
        public Device createDevice(DeviceDriver deviceDriver, int addrNew) {
            trace.print(Trace.FLOW, "%s createDevice addr %d", portName,addrNew);
            DeviceImpl deviceNew = new DeviceImpl(this,deviceDriver,addrNew);
            portLock.lock();
            try {
                if(!isMultiDevicePort) {
                    if(deviceList.size()!=0) {
                        throw new IllegalStateException("device already created");
                    }
                    deviceList.add(deviceNew);
                    return deviceNew;
                }
                ListIterator<DeviceImpl> iter = deviceList.listIterator();
                while(iter.hasNext()) {
                    Device device = iter.next();
                    if(deviceNew==device) {
                        throw new IllegalStateException("already registered");
                    }
                    int addr = device.getAddr();
                    if(addrNew==addr) {
                        throw new IllegalStateException(
                                "addr " + addr + " already registered");
                    }
                    if(addrNew<addr) {
                        iter.add(deviceNew);
                        return deviceNew;
                    }
                }
                deviceList.add(deviceNew);
            } finally {
                portLock.unlock();
            }
            return deviceNew;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortDriver#registerInterface(org.epics.ioc.asyn.Interface)
         */
        public void registerInterface(Interface ifaceNew) {
            String interfaceName = ifaceNew.getInterfaceName();
            trace.print(Trace.FLOW, "%s registerInterface %s", portName,interfaceName);
            PortInterface portInterfaceNew = new PortInterface(ifaceNew);
            portLock.lock();
            try {
                ListIterator<PortInterface> iter = interfaceList.listIterator();
                while(iter.hasNext()) {
                    PortInterface portInterface = iter.next();
                    Interface iface = portInterface.iface;
                    if(iface==ifaceNew) {
                        throw new IllegalStateException("interface already registered");
                    }
                    int compare = interfaceName.compareTo(iface.getInterfaceName());
                    if(compare==0) {
                        throw new IllegalStateException(
                                "interface " + interfaceName + " already registered");
                    }
                    if(compare<0) {
                        iter.add(portInterfaceNew);
                        return;
                    }
                }
                interfaceList.add(portInterfaceNew);
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortDriver#interposeInterface(org.epics.ioc.asyn.Interface)
         */
        public Interface interposeInterface(Interface interposeInterface) {
            String interfaceName = interposeInterface.getInterfaceName();
            trace.print(Trace.FLOW, "%s interposeInterface %s", portName,interfaceName);
            portLock.lock();
            try {
                ListIterator<PortInterface> iter = interfaceList.listIterator();
                while(iter.hasNext()) {
                    PortInterface portInterface = iter.next();
                    Interface iface = portInterface.iface;
                    int compare = interfaceName.compareTo(iface.getInterfaceName());
                    if(compare==0) {
                        Interface returnInterface = portInterface.interposeInterface;
                        portInterface.interposeInterface = interposeInterface;
                        if(returnInterface==null) returnInterface = iface;
                        return returnInterface;
                    }
                }
                interfaceList.add(new PortInterface(interposeInterface));
            } finally {
                portLock.unlock();
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortDriver#exceptionConnect()
         */
        public void exceptionConnect() {
            trace.print(Trace.FLOW, "%s exceptionConnect", portName);
            portLock.lock();
            try {
                if(connected) {
                    throw new IllegalStateException("already connected");
                }
                connected = true;
            } finally {
                portLock.unlock();
            }
            raiseException(ConnectException.connect);
            scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortDriver#exceptionDisconnect()
         */
        public void exceptionDisconnect() {
            trace.print(Trace.FLOW, "%s exceptionDisconnect", portName);
            portLock.lock();
            try {
                if(connected) {
                    throw new IllegalStateException("already connected");
                }
                connected = false;
            } finally {
                portLock.unlock();
            }
            raiseException(ConnectException.connect);
        }
        
        
        private Status lockPort(UserImpl user) {
            trace.print(Trace.FLOW, "%s lockPort", portName);
            lockPortLock.lock();
            try {
                if(!enabled) {
                    user.message = "port disabled";
                    return Status.error;
                }
                if(lockPortUser!=null) {
                    try {
                        portUnlock.await();
                    } catch (InterruptedException e){}
                }
                lockPortUser = user;
            } finally {
                lockPortLock.unlock();
            }
            if(!connected) {
                if(autoConnect) {
                    Status status = portDriver.connect(user);
                    if(status!=Status.success) {
                        unlockPort(user);
                        return status;
                    }
                }
                if(!connected) {
                    unlockPort(user);
                    user.message = "not connected";
                    return Status.error;
                }
            }
            LockPortNotify notify = lockPortNotify;
            if(notify!=null) notify.lock(user);
            return Status.success;
        }
        
        private void unlockPort(UserImpl user) {
            trace.print(Trace.FLOW, "%s unlockPort", portName);
            lockPortLock.lock();
            try {
                if(lockPortUser!=user) {
                    trace.print(Trace.ERROR, "%s unlockPort but not the lockPortUser", portName);
                }
                lockPortUser = null;;
                portUnlock.signal();
            } finally {
                lockPortLock.unlock();
            }
            // notice possible race condition, i.e. lockPortNotify.lock may again be called before this
            LockPortNotify notify = lockPortNotify;
            if(notify!=null) notify.unlock();
        }   
        
        private void raiseException(ConnectException connectException) {
            portLock.lock();
            try {
                exceptionActive = true;
            } finally {
                portLock.unlock();
            }
            ListIterator<ConnectExceptionListener> iter =  exceptionListenerList.listIterator();
            while(iter.hasNext()) {
                ConnectExceptionListener asynConnectExceptionListener = iter.next();
                asynConnectExceptionListener.exception(connectException);
            }
            portLock.lock();
            try {
                if(exceptionListenerListNew!=null) {
                    exceptionListenerList = exceptionListenerListNew;
                    exceptionListenerListNew = null;
                }
                exceptionActive = false; 
            } finally {
                portLock.unlock();
            }
        }
    }
    
    private static class DeviceImpl implements Device {

        private DeviceImpl(PortImpl port,DeviceDriver deviceDriver,int addr) {
            this.port = port;
            this.deviceDriver = deviceDriver;
            this.addr = addr;
            portName = port.getPortName();
            autoConnect = port.autoConnect;
        }

        private static class DeviceInterface {
            private Interface iface;
            private Interface interposeInterface = null;

            private DeviceInterface(Interface iface) {
                this.iface = iface;
            }
        }
        
        private ReentrantLock deviceLock = new ReentrantLock();
        private PortImpl port;
        private String portName;
        private DeviceDriver deviceDriver;
        private int addr;
        private Trace trace = new TraceImpl();
        private List<DeviceInterface> interfaceList = new LinkedList<DeviceInterface>();

        private List<ConnectExceptionListener> exceptionListenerList = new LinkedList<ConnectExceptionListener>();
        private List<ConnectExceptionListener> exceptionListenerListNew = null;
        private boolean exceptionActive = true;
        
        private User blockingUser = null;

        private boolean autoConnect = false;
        private boolean connected = false;
        private boolean enabled = true;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#report(int)
         */
        public String report(int details) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    "    addr %d isBlocked %b autoConnect %b connected %b enabled %b%n",
                    addr,isBlockedByOtherUser(null),autoConnect,connected,enabled
            ));
            if(details>0) {
                Interface[] interfaces = getInterfaces();
                int length = interfaces.length;
                if(length>0) {
                    builder.append("    Interfaces: ");
                    for(int i=0; i<interfaces.length; i++) {
                        Interface iface = interfaces[i];
                        builder.append(iface.getInterfaceName() + " ");
                    }
                    builder.append(String.format("%n"));
                }
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#getInterfaces()
         */
        public Interface[] getInterfaces() {
            Interface[] interfaces = null;
            deviceLock.lock();
            try {
                int size = interfaceList.size();
                interfaces = new Interface[size];
                for(int i=0; i<size; i++) {
                    interfaces[i] = interfaceList.get(i).iface;
                }
            } finally {
                deviceLock.unlock();
            }
            return interfaces;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#getAddr()
         */
        public int getAddr() {
            return addr;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#getPort()
         */
        public Port getPort() {
            return port;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#getTrace()
         */
        public Trace getTrace() {
            return trace;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#enable(boolean)
         */
        public void enable(boolean trueFalse) {
            trace.print(Trace.FLOW, "%s[%s] enable %b", portName,addr,trueFalse);
            deviceLock.lock();
            try {
                enabled = trueFalse;
            } finally {
                deviceLock.unlock();
            }
            raiseException(ConnectException.enable);
            if(trueFalse) port.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#autoConnect(boolean)
         */
        public void autoConnect(boolean trueFalse) {
            trace.print(Trace.FLOW, "%s[%s] autoConnect %b", portName,addr,trueFalse);
            deviceLock.lock();
            try {
                autoConnect = trueFalse;
            } finally {
                deviceLock.unlock();
            }
            raiseException(ConnectException.autoConnect);
            if(trueFalse) port.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#connect(org.epics.ioc.pdrv.User)
         */
        public Status connect(User theUser) {
            UserImpl user = (UserImpl)theUser;
            trace.print(Trace.FLOW, "%s[%s] connect", portName,addr);
            PortImpl port = (PortImpl)user.getPort();
            Status status = port.lockPort(user);
            if(status!=Status.success) return status;
            try {
                if(connected) {
                    user.setMessage("already connected");
                    return Status.error;
                }
                return deviceDriver.connect(user);
            } finally {
                port.unlockPort(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#disconnect(org.epics.ioc.pdrv.User)
         */
        public Status disconnect(User theUser) {
            UserImpl user = (UserImpl)theUser;
            trace.print(Trace.FLOW, "%s[%s] disconnect", portName,addr);
            PortImpl port = (PortImpl)user.getPort();
            Status status = port.lockPort(user);
            if(status!=Status.success) return status;
            try {
                if(!connected) {
                    user.setMessage("not connected");
                    return Status.error;
                }
                return deviceDriver.disconnect(user);
            } finally {
                port.unlockPort(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#isAutoConnect()
         */
        public boolean isAutoConnect() {
            return autoConnect;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#isConnected()
         */
        public boolean isConnected() {
            return connected;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#isEnabled()
         */
        public boolean isEnabled() {
            return enabled;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#exceptionListenerAdd(org.epics.ioc.asyn.ConnectExceptionListener)
         */
        public void exceptionListenerAdd(ConnectExceptionListener asynConnectExceptionListener) {
            trace.print(Trace.FLOW, "%s[%s] exceptionListenerAdd", portName,addr);
            deviceLock.lock();
            try {
                if(!exceptionActive) {   
                    if(!exceptionListenerList.add(asynConnectExceptionListener)) {
                        throw new IllegalStateException("add failed");
                    }
                    return;
                }
                if(exceptionListenerListNew==null) {
                    exceptionListenerListNew =
                        new LinkedList<ConnectExceptionListener>(exceptionListenerList);
                }
                if(!exceptionListenerListNew.add(asynConnectExceptionListener)) {
                    throw new IllegalStateException("add failed");
                }
            } finally {
                deviceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#exceptionListenerRemove(org.epics.ioc.asyn.ConnectExceptionListener)
         */
        public void exceptionListenerRemove(ConnectExceptionListener asynConnectExceptionListener) {
            trace.print(Trace.FLOW, "%s[%s] exceptionListenerRemove", portName,addr);
            deviceLock.lock();
            try {
                if(!exceptionActive) {   
                    if(!exceptionListenerList.remove(asynConnectExceptionListener)) {
                        throw new IllegalStateException("add failed");
                    }
                    return;
                }
                if(exceptionListenerListNew==null) {
                    exceptionListenerListNew =
                        new LinkedList<ConnectExceptionListener>(exceptionListenerList);
                }
                if(!exceptionListenerListNew.remove(asynConnectExceptionListener)) {
                    throw new IllegalStateException("add failed");
                }
            } finally {
                deviceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#findInterface(org.epics.ioc.asyn.User, java.lang.String, boolean)
         */
        public Interface findInterface(User user, String interfaceName, boolean interposeInterfaceOK) {
            deviceLock.lock();
            try {
                ListIterator<DeviceInterface> iter = interfaceList.listIterator();
                while(iter.hasNext()) {
                    DeviceInterface iface = iter.next();
                    Interface asynInterface = iface.iface;
                    int compare = interfaceName.compareTo(asynInterface.getInterfaceName());
                    if(compare==0) {
                        if(interposeInterfaceOK && iface.interposeInterface!=null) {
                            return iface.interposeInterface;
                        }
                        return asynInterface;
                    }
                    if(compare<0) break;
                }
                user.setMessage("interface " + interfaceName + " not found");
                return null;
            } finally {
                deviceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#blockOtherUsers(org.epics.ioc.pdrv.User)
         */
        public Status blockOtherUsers(User user) {
            trace.print(Trace.FLOW, "%s[%s] blockOtherUsers", portName,addr);
            if(blockingUser!=null) {
                user.setMessage("already blocked");
                return Status.error;
            }
            blockingUser = user;
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#isBlockedByUser(org.epics.ioc.pdrv.User)
         */
        public boolean isBlockedByOtherUser(User user) {
            if(blockingUser==null) return false;
            return ((blockingUser==user) ? false : true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#unblockOtherUsers(org.epics.ioc.pdrv.User)
         */
        public void unblockOtherUsers(User user) {
            trace.print(Trace.FLOW, "%s[%s] unblockOtherUsers", portName,addr);
            if(user!=blockingUser) {
                throw new IllegalStateException("not the blocking asynUser");
            }
            blockingUser = null;
            PortThread portThread = ((PortImpl)port).portThread;
            if(portThread!=null) portThread.isUnblocked();
            port.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceDriver#registerInterface(org.epics.ioc.asyn.Interface)
         */
        public void registerInterface(Interface ifaceNew) {
            String interfaceName = ifaceNew.getInterfaceName();
            trace.print(Trace.FLOW, "%s[%s] registerInterface %s", portName,addr,interfaceName);
            DeviceInterface deviceInterfaceNew = new DeviceInterface(ifaceNew);

            ListIterator<DeviceInterface> iter = interfaceList.listIterator();
            while(iter.hasNext()) {
                DeviceInterface deviceInterface = iter.next();
                Interface iface = deviceInterface.iface;
                if(iface==ifaceNew) {
                    throw new IllegalStateException("interface already registered");
                }
                int compare = interfaceName.compareTo(iface.getInterfaceName());
                if(compare==0) {
                    throw new IllegalStateException(
                            "interface " + interfaceName + " already registered");
                }
                if(compare<0) {
                    iter.add(deviceInterfaceNew);
                    return;
                }
            }
            interfaceList.add(deviceInterfaceNew);

        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceDriver#interposeInterface(org.epics.ioc.asyn.Interface)
         */
        public Interface interposeInterface(Interface interposeInterface) {
            String interfaceName = interposeInterface.getInterfaceName();
            trace.print(Trace.FLOW, "%s[%s] interposeInterface %s", portName,addr,interfaceName);
            ListIterator<DeviceInterface> iter = interfaceList.listIterator();
            while(iter.hasNext()) {
                DeviceInterface deviceInterface = iter.next();
                Interface iface = deviceInterface.iface;

                int compare = interfaceName.compareTo(iface.getInterfaceName());
                if(compare==0) {
                    Interface returnInterface = deviceInterface.interposeInterface;
                    deviceInterface.interposeInterface = interposeInterface;
                    if(returnInterface==null) returnInterface = iface;
                    return returnInterface;
                }
            }
            interfaceList.add(new DeviceInterface(interposeInterface));
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceDriver#exceptionConnect()
         */
        public void exceptionConnect() {
            trace.print(Trace.FLOW, "%s[%s] exceptionConnect", portName,addr);
            deviceLock.lock();
            try {
                if(connected) {
                    throw new IllegalStateException("already connected");
                }
                connected = true;
            } finally {
                deviceLock.unlock();
            }
            raiseException(ConnectException.connect);
            port.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceDriver#exceptionDisconnect()
         */
        public void exceptionDisconnect() {
            trace.print(Trace.FLOW, "%s[%s] exceptionDisconnect", portName,addr);
            deviceLock.lock();
            try {
                if(!connected) {
                    throw new IllegalStateException("not connected");
                }
                connected = false;
            } finally {
                deviceLock.unlock();
            }
            raiseException(ConnectException.connect);
        }
        
        private Status lockPort(UserImpl user) {
            trace.print(Trace.FLOW, "%s[%s] device.lockPort", portName,addr);
            Status status = port.lockPort((UserImpl)user);
            if(status!=Status.success) return status;
            if(!connected) {
                if(autoConnect) {
                    status = deviceDriver.connect(user);
                    if(status!=Status.success)  {
                        port.unlockPort((UserImpl)user);
                        return status;
                    }
                }
                if(!connected) {
                    port.unlockPort((UserImpl)user);
                    user.setMessage("device not connected");
                    return Status.error;
                }
            }
            if(blockingUser!=null&&blockingUser!=user) {
                port.unlockPort((UserImpl)user);
                user.setMessage("device blocked by other user");
                return Status.error;
            }
            return Status.success;
        }

        private void raiseException(ConnectException asynException) {
            deviceLock.lock();
            try {
                exceptionActive = true; 
            } finally {
                deviceLock.unlock();
            }
            ListIterator<ConnectExceptionListener> iter =  exceptionListenerList.listIterator();
            while(iter.hasNext()) {
                ConnectExceptionListener asynConnectExceptionListener = iter.next();
                asynConnectExceptionListener.exception(asynException);
            }
            deviceLock.lock();
            try {
                if(exceptionListenerListNew!=null) {
                    exceptionListenerList = exceptionListenerListNew;
                    exceptionListenerListNew = null;
                }
                exceptionActive = false; 
            } finally {
                deviceLock.unlock();
            }
        }
    }
    
    private static class TraceImpl implements Trace {
        private TraceImpl() {
            
        }
        
        private static final int DEFAULT_TRACE_TRUNCATE_SIZE = 80;
        
        private ReentrantLock traceLock = new ReentrantLock();
        private Writer file = new BufferedWriter(new OutputStreamWriter(System.out));
        private TimeStamp timeStamp = new TimeStamp();
        private int mask = Trace.ERROR;//|Trace.SUPPORT|Trace.FLOW|Trace.DRIVER;
        private int iomask = Trace.IO_NODATA;//|Trace.IO_ASCII;
        private int truncateSize = DEFAULT_TRACE_TRUNCATE_SIZE;
        
        private List<TraceOptionChangeListener> optionChangeListenerList = new LinkedList<TraceOptionChangeListener>();
        private List<TraceOptionChangeListener> optionChangeListenerListNew = null;
        private boolean optionChangeActive = true;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceFile()
         */
        public Writer getFile() {
            traceLock.lock();
            try {
                return file;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceIOMask()
         */
        public int getIOMask() {
            traceLock.lock();
            try {
                return iomask;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceIOTruncateSize()
         */
        public int getIOTruncateSize() {
            traceLock.lock();
            try {
                return truncateSize;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceMask()
         */
        public int getMask() {
            traceLock.lock();
            try {
                return mask;
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceFile(java.io.Writer)
         */
        public void setFile(Writer file) {
            traceLock.lock();
            try {
                this.file = file;
                raiseException();
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOMask(int)
         */
        public void setIOMask(int mask) {
            traceLock.lock();
            try {
                iomask = mask;
                raiseException();
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOTruncateSize(long)
         */
        public void setIOTruncateSize(int size) {
            traceLock.lock();
            try {
                truncateSize = size;
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceMask(int)
         */
        public void setMask(int mask) {
            traceLock.lock();
            try {
                this.mask = mask;
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#optionChangeListenerAdd(org.epics.ioc.pdrv.TraceOptionChangeListener)
         */
        public void optionChangeListenerAdd(TraceOptionChangeListener traceOptionChangeListener) {
            traceLock.lock();
            try {
                if(!optionChangeActive) {   
                    if(!optionChangeListenerList.add(traceOptionChangeListener)) {
                        throw new IllegalStateException("add failed");
                    }
                    return;
                }
                if(optionChangeListenerListNew==null) {
                    optionChangeListenerListNew =
                        new LinkedList<TraceOptionChangeListener>(optionChangeListenerList);
                }
                if(!optionChangeListenerListNew.add(traceOptionChangeListener)) {
                    throw new IllegalStateException("add failed");
                }
            } finally {
                traceLock.unlock();
            }
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#optionChangeListenerRemove(org.epics.ioc.pdrv.TraceOptionChangeListener)
         */
        public void optionChangeListenerRemove(TraceOptionChangeListener traceOptionChangeListener) {
            traceLock.lock();
            try {
                if(!optionChangeActive) {   
                    if(!optionChangeListenerList.remove(traceOptionChangeListener)) {
                        throw new IllegalStateException("add failed");
                    }
                    return;
                }
                if(optionChangeListenerListNew==null) {
                    optionChangeListenerListNew =
                        new LinkedList<TraceOptionChangeListener>(optionChangeListenerList);
                }
                if(!optionChangeListenerListNew.remove(traceOptionChangeListener)) {
                    throw new IllegalStateException("add failed");
                }
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#print(int, java.lang.String)
         */
        public void print(int reason, String message) {
            print(reason,"%s",message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#print(int, java.lang.String, java.lang.Object[])
         */
        public void print(int reason, String format, Object... args) {
            if((reason&mask)==0) return;
            traceLock.lock();
            try {
                file.write(getTime() + String.format(format, args));
                file.write(String.format("%n"));
                file.flush();
            }catch (IOException e) {
                System.err.println(e.getMessage());
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#printIO(int, byte[], long, java.lang.String)
         */
        public void printIO(int reason, byte[] buffer, long len, String message) {
            printIO(reason,buffer,len,"%s",message);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#printIO(int, byte[], long, java.lang.String, java.lang.Object[])
         */
        public void printIO(int reason, byte[] buffer, long len, String format, Object... args) {
            if((reason&mask)==0) return;
            traceLock.lock();
            try {
                file.write(getTime() + String.format(format, args));
                if(iomask!=0) {
                    int index = 0;
                    StringBuilder builder = new StringBuilder();
                    builder.append(' ');
                    while(builder.length()<truncateSize && index<len) {
                        if((iomask&Trace.IO_ASCII)!=0) {
                            char value = (char)buffer[index];
                            builder.append(value);
                        } else if((iomask&Trace.IO_ESCAPE)!=0) {
                            char value = (char)buffer[index];
                            if(Character.isISOControl(value)) {
                                builder.append(getEscaped(value));
                            } else {
                                builder.append(buffer[index]);
                            }
                        } else if((iomask&Trace.IO_HEX)!=0) {
                            builder.append(String.format("%x ", buffer[index]));
                        }
                        ++index;
                    }
                    file.write(builder.toString());
                }
                file.write(String.format("%n"));
                file.flush();
            }catch (IOException e) {
                System.err.println(e.getMessage());
            }  finally {
                traceLock.unlock();
            }
        }

        private void raiseException() {
            traceLock.lock();
            try {
                optionChangeActive = true; 
            } finally {
                traceLock.unlock();
            }
            ListIterator<TraceOptionChangeListener> iter =  optionChangeListenerList.listIterator();
            while(iter.hasNext()) {
                TraceOptionChangeListener asynOptionChangeListener = iter.next();
                asynOptionChangeListener.optionChange();
            }
            traceLock.lock();
            try {
                if(optionChangeListenerListNew!=null) {
                    optionChangeListenerList = optionChangeListenerListNew;
                    optionChangeListenerListNew = null;
                }
                optionChangeActive = false; 
            } finally {
                traceLock.unlock();
            }
        }
        
        private String getTime() {
            TimeUtility.set(timeStamp,System.currentTimeMillis());
            long secondPastEpochs = timeStamp.secondsPastEpoch;
            int nano = timeStamp.nanoSeconds;
            long milliPastEpoch = nano/1000000 + secondPastEpochs*1000;
            Date date = new Date(milliPastEpoch);
            return String.format("%tF %tT.%tL ", date,date,date);
        }
        private static HashMap<Character,String> escapedMap = new HashMap<Character,String> ();
        
        static {
            escapedMap.put('\b', "\\b");
            escapedMap.put('\t', "\\t");
            escapedMap.put('\n', "\\n");
            escapedMap.put('\f', "\\f");
            escapedMap.put('\r', "\\r");
            escapedMap.put('\"', "\\\"");
            escapedMap.put('\'', "\\\'");
            escapedMap.put('\\', "\\\\");
        }
        
        String getEscaped(char key) {
            String value = escapedMap.get(key);
            if(value==null) {
                value = String.format("\\%2.2x", key);
            }
            return value;
        }
    }
    
    private static final int queuePriorityHigh = QueuePriority.high.ordinal();
    private static final int numQueuePriorities = queuePriorityHigh + 1;
    private static final int queuePriorityLow = QueuePriority.low.ordinal();
    private static final int queuePriorityMedium = QueuePriority.medium.ordinal();
    
    private static class PortThread implements Runnable  {
        private Thread thread = new Thread(this);
        private ReentrantLock queueLock = new ReentrantLock();
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        private PortImpl port;
        private boolean isRunning = false;
        
        private List<UserImpl>[] queueListArray = new ArrayList[numQueuePriorities];  
        private List<UserImpl> waitUnblockList = new ArrayList<UserImpl>();   
        
        private PortThread(PortImpl port,ScanPriority scanPriority) {
            this.port = port;
            for(int i=0; i<queueListArray.length; i++) {
                queueListArray[i] = new ArrayList<UserImpl>();
            }
            thread.setPriority(scanPriority.getJavaPriority());
            thread.start();
            while(!isRunning) {
                try {
                Thread.sleep(1);
                } catch(InterruptedException e) {}
            }
        }
        
        private void queueRequest(UserImpl user,QueuePriority asynQueuePriority)
        {
            queueLock.lock();
            try {
                if(user.isQueued) {
                    throw new IllegalStateException("prevous queueRequest not complete");
                }
                user.isQueued = true;
                List<UserImpl> list = queueListArray[asynQueuePriority.ordinal()];
                list.add(user);
            } finally {
                queueLock.unlock();
            }
            scanQueues();
        }
        
        private void cancelRequest(UserImpl asynUserPvtCancel)
        {
            UserImpl user = null;
            queueLock.lock();
            try {
                List<UserImpl> list = null;
                ListIterator<UserImpl> iter = null;
                for(int i=queuePriorityHigh; i>=queuePriorityLow; i--) {
                    list = queueListArray[i];
                    iter = list.listIterator();
                    while(iter.hasNext()) {
                        user = iter.next();
                        if(user==asynUserPvtCancel) {
                            iter.remove();
                            user.isQueued = false;
                            return;
                        }
                    }
                }
                iter = waitUnblockList.listIterator();
                while(iter.hasNext()) {
                    user = iter.next();
                    if(user==asynUserPvtCancel) {
                        iter.remove();
                        user.isQueued = false;
                        return;
                    }
                }
            } finally {
                queueLock.unlock();
            }
        }
        
        private void isUnblocked() {
            queueLock.lock();
            try {
                ListIterator<UserImpl> iter = waitUnblockList.listIterator();
                while(iter.hasNext()) {
                    UserImpl user = iter.next();
                    iter.remove();
                    queueListArray[queuePriorityMedium].add(user);
                }
            } finally {
                queueLock.unlock();
            }
            scanQueues();
        }
        
        private void scanQueues() {
            lock.lock();
            try {
                moreWork.signal();
            } finally {
                lock.unlock();
            }
        }
        
        public void run() {
            isRunning = true;
            try {
                while(true) {
                    lock.lock();
                    try {                        
                        moreWork.await();
                    } finally {
                        lock.unlock();
                    }
                    scanQueue();
                }
            } catch(InterruptedException e) {
                
            }
        }
                
        private void scanQueue() {
            UserImpl user = null;
            DeviceImpl device = null;
            while(true) {                                
                queueLock.lock();
                try {
                    for(int queue=queuePriorityHigh; queue>=queuePriorityLow; queue--) {
                        List<UserImpl> list = queueListArray[queue];
                        int size = list.size();
                        if(size<=0) continue;
                        user = list.get(0);
                        list.remove(0);
                        device = (DeviceImpl)user.getDevice();
                        if(device!=null) {
                            if(device.isBlockedByOtherUser(user)) {
                                waitUnblockList.add(user);
                                continue;
                            }
                        }
                        user.isQueued = false;
                        break;
                    }
                    if(user==null) return;
                } finally {
                    queueLock.unlock();
                }
                QueueRequestCallback queueRequestCallback = user.getQueueRequestCallback();
                Status status = null;
                if(device!=null) {
                    status = device.lockPort(user);
                } else {
                    status = port.lockPort(user);
                    
                }
                if(status==Status.success) {
                    try {
                        queueRequestCallback.callback(Status.success,user);
                    } finally {
                        port.unlockPort(user);
                    }
                    user = null;
                } else {
                    queueRequestCallback.callback(status,user);
                }
            }
        }
    }
}
