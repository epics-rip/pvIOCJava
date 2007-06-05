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

import org.epics.ioc.util.*;

/**
 * Factory for creating PDRV (Port Driver) objects.
 * @author mrk
 *
 */
public class Factory {
    /**
     * Create a User, which is the "handle" for communicating with port and device drivers.
     * @param asynQueueRequestCallback The user callback for port.QueueRequest.
     * @return The interface to a User object.
     */
    public static User createUser(QueueRequestCallback asynQueueRequestCallback) {
        return new UserImpl(asynQueueRequestCallback);
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
    
    private static ReentrantLock globalLock = new ReentrantLock();
    private static List<PortImpl> portList = new LinkedList<PortImpl>();
    
    private static class UserImpl implements User {
        
        private UserImpl(QueueRequestCallback queueRequestCallback) {
            this.queueRequestCallback = queueRequestCallback;
        }
        
        private QueueRequestCallback queueRequestCallback = null;
        private PortImpl port = null;
        private DeviceImpl device = null;
        private boolean isDeleted = false;
        private boolean isQueued = false;
        
        private Object userPvt;
        private Object portDriverUserPvt;
        private Object deviceDriverUserPvt;
        private int reason;
        private double timeout;
        
        private int auxStatus;
        private String message;
        private int intValue;
        private double doubleValue;
        private String stringValue;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#connectPort(java.lang.String)
         */
        public Port connectPort(String portName) {
            globalLock.lock();
            try {
                if(port!=null) {
                    setMessage("already connected");
                    return null;
                }
                ListIterator<PortImpl> iter = portList.listIterator();
                while(iter.hasNext()) {
                    PortImpl port =iter.next();
                    if(port.getPortName().equals(portName)) {
                        this.port = port;
                        return port;
                    }
                }
                setMessage("portName " + portName + " not registered");
            } finally {
                globalLock.unlock();
            }
            return null;
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
            port.lockPort(this);
            try {
                List<DeviceImpl> deviceList = port.deviceList;
                ListIterator<DeviceImpl> iter = deviceList.listIterator();
                while(iter.hasNext()) {
                    DeviceImpl device = iter.next();
                    if(device.getAddr() == addr) {
                        this.device = device;
                        return device;
                    }
                }

                return port.portDriver.createDevice(this, addr);
            } finally {
                port.unlockPort(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#disconnectDevice()
         */
        public void disconnectDevice() {
            if(device==null) return;
            if(isQueued) {
                port.lockPort(this);
                try {
                    port.cancelRequest(this);
                } finally {
                    port.unlockPort(this);
                }
            }
            device = null;
            deviceDriverUserPvt = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#disconnectPort()
         */
        public void disconnectPort() {
            disconnectDevice();
            port = null;
            portDriverUserPvt = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#duplicateUser(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.QueueRequestCallback)
         */
        public User duplicateUser(User user, QueueRequestCallback queueRequestCallback) {
            UserImpl newUser = new UserImpl(queueRequestCallback);
            newUser.port = port;
            newUser.device = device;
            newUser.reason = reason;
            newUser.timeout = timeout;
            return newUser;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getAuxStatus()
         */
        public int getAuxStatus() {
            return auxStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getDevice()
         */
        public Device getDevice() {
            return device;
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
         * @see org.epics.ioc.pdrv.User#getPort()
         */
        public Port getPort() {
            return port;
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
         * @see org.epics.ioc.pdrv.User#getDeviceDriverUserPvt()
         */
        public Object getDeviceDriverUserPvt() {
            return deviceDriverUserPvt;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#getPortDriverUserPvt()
         */
        public Object getPortDriverUserPvt() {
            return portDriverUserPvt;
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
         * @see org.epics.ioc.pdrv.User#setDeviceDriverUserPvt(java.lang.Object)
         */
        public void setDeviceDriverUserPvt(Object deviceDriverUserPvt) {
            this.deviceDriverUserPvt = deviceDriverUserPvt;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setPortDriverUserPvt(java.lang.Object)
         */
        public void setPortDriverUserPvt(Object portDriverUserPvt) {
            this.portDriverUserPvt = portDriverUserPvt;
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
        private static ReentrantLock portLock = new ReentrantLock();
        private String portName;
        private PortDriver portDriver;
        private String driverName;
        private boolean isMultiDevicePort;
        private boolean autoConnect;
        private PortThread portThread = null;
        private Trace trace = new TraceImpl();
        private List<DeviceImpl> deviceList = new LinkedList<DeviceImpl>();
        
        private static class PortInterface {
            private Interface iface;
            private Interface interposeInterface = null;
            
            private PortInterface(Interface iface) {
                this.iface = iface;
            }
        }
        private List<PortInterface> interfaceList = new LinkedList<PortInterface>();
        
        private List<ConnectExceptionListener> exceptionListenerList = new LinkedList<ConnectExceptionListener>();
        private List<ConnectExceptionListener> exceptionListenerListNew = null;
        private boolean exceptionActive = true;
        
        private User lockPortUser = null;
        private ReentrantLock lockPortLock = new ReentrantLock();
        private Condition portUnlock = lockPortLock.newCondition();
        
        private LockPortNotify lockPortNotify = null;   
        private User blockingUser = null;
        private boolean connected = false;
        private boolean enabled = true;
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
         * @see org.epics.ioc.pdrv.Port#blockOtherUsers(org.epics.ioc.pdrv.User)
         */
        public Status blockOtherUsers(User theUser) {
            UserImpl user = (UserImpl)theUser;
            if(blockingUser!=null) {
                user.message = "already blocked";
                trace.print(Trace.ERROR, "%s blockOtherUsers but already blocked", portName);
                return Status.error;
            }
            trace.print(Trace.FLOW, "%s blockOtherUsers", portName);
            blockingUser = theUser;
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#isBlockedByUser(org.epics.ioc.pdrv.User)
         */
        public boolean isBlockedByUser(User user) {
            if(blockingUser==null) return false;
            return ((blockingUser==user) ? false : true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#unblockOtherUsers(org.epics.ioc.pdrv.User)
         */
        public void unblockOtherUsers(User user) {
            if(user!=blockingUser) {
                throw new IllegalStateException("not the blocking asynUser");
            }
            blockingUser = null;
            trace.print(Trace.FLOW, "%s unblockOtherUsers", portName);
            if(portThread!=null) portThread.scanQueues();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#report(boolean, int)
         */
        public String report(boolean reportDevices,int details) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                "port %s driver %s isBlocked %b isLocked %b%n",
                portName,driverName,isBlockedByUser(null),(lockPortUser==null ? false : true)
                ));
            if(details>0) {
                builder.append(String.format(
                    "isMultiDevicePort %b autoConnect %b connected %b enabled %b",
                    isMultiDevicePort,autoConnect,connected,enabled
                    ));
            }
            builder.append(String.format("%n"));
            if(reportDevices) {
                ListIterator<DeviceImpl> iter = deviceList.listIterator();
                while(iter.hasNext()) {
                    Device asynDevice = iter.next();
                    builder.append(asynDevice.report(details));
                }
            }
            return builder.toString();
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
        public void exceptionListenerAdd(
                ConnectExceptionListener asynConnectExceptionListener)
        {
            portLock.lock();
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
                trace.print(Trace.FLOW, "%s exceptionListenerAdd", portName);
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#exceptionListenerRemove(org.epics.ioc.pdrv.ConnectExceptionListener)
         */
        public void exceptionListenerRemove(ConnectExceptionListener asynConnectExceptionListener) { 
            portLock.lock();
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
                trace.print(Trace.FLOW, "%s exceptionListenerRemove", portName);
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
                    PortInterface iface = iter.next();
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
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortUser#queuePortRequest(org.epics.ioc.asyn.User)
         */
        public Status queuePortRequest(User theUser) {
            UserImpl user = (UserImpl)theUser;
            if(user.queueRequestCallback==null) {
                user.message = "queueRequestCallback is null";
                return Status.error;
            }
            if(user.port==null) {
                user.message = "not connected to a port";
                return Status.error;
            }
            if(portThread!=null) {
                portThread.queuePortRequest(user);
            } else {
                lockPort(theUser);
                try {
                    trace.print(Trace.FLOW, "%s queueRequestCallback", portName);
                    user.queueRequestCallback.callback(theUser);
                } finally {
                    unlockPort(theUser);
                }
            }
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortUser#queueDeviceRequest(org.epics.ioc.asyn.User, org.epics.ioc.asyn.QueuePriority)
         */
        public Status queueDeviceRequest(User theUser,QueuePriority asynQueuePriority)
        {
            UserImpl user = (UserImpl)theUser;
            if(user.queueRequestCallback==null) {
                user.message = "queueRequestCallback is null";
                return Status.error;
            }
            if(user.port==null) {
                user.message = "not connected to a port";
                return Status.error;
            }
            if(user.device==null) {
                user.message = "not connected to a device";
                return Status.error;
            }
            if(portThread!=null) {
                portThread.queueDeviceRequest(user, asynQueuePriority);
            } else {
                lockPort(theUser);
                try {
                    trace.print(Trace.FLOW, "%s queueRequestCallback", portName);
                    user.queueRequestCallback.callback(theUser);
                } finally {
                    unlockPort(theUser);
                }
            }
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortUser#scanQueue()
         */
        public void scanQueues() {
            if(portThread!=null) portThread.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#cancelRequest(org.epics.ioc.pdrv.User)
         */
        public void cancelRequest(User theUser) {
            trace.print(Trace.FLOW, "%s cancelRequest", portName);
            UserImpl user = (UserImpl)theUser;
            if(user.queueRequestCallback==null) {
                throw new IllegalStateException("queueRequestCallback is null");
            }
            if(portThread!=null) portThread.cancelRequest(user);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#lockPort(org.epics.ioc.pdrv.User)
         */
        public void lockPort(User theUser) {
            trace.print(Trace.FLOW, "%s lockPort", portName);
            UserImpl user = (UserImpl)theUser;
            lockPortLock.lock();
            try {
                if(lockPortUser!=null) {
                    try {
                        portUnlock.await();
                    } catch (InterruptedException e){}
                }
                lockPortUser = user;
            } finally {
                lockPortLock.unlock();
            }
            if(portThread!=null) portThread.lockPort();
            if(lockPortNotify!=null) {
                portLock.lock();
                try {
                    if(lockPortNotify!=null) lockPortNotify.lock(user); 
                } finally {
                    portLock.unlock();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#lockPortForIO(org.epics.ioc.pdrv.User)
         */
        public Status lockPortForIO(User theUser) {
            trace.print(Trace.FLOW, "%s lockPortForIO", portName);
            UserImpl user = (UserImpl)theUser;
            lockPortLock.lock();
            try {
                if(!enabled) {
                    user.message = "disabled";
                    return Status.error;
                }
                if(theUser!=blockingUser) {
                    user.message = "blocked";
                    return Status.error;
                }
                lockPortUser = user;
            } finally {
                lockPortLock.unlock();
            }
            if(portThread!=null) portThread.lockPort();
            if(!connected) {
                autoConnect(true);
                if(!connected) {
                    unlockPort(theUser);
                    user.message = "not connected";
                    return Status.error;
                }
            }
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#unlockPort(org.epics.ioc.pdrv.User)
         */
        public void unlockPort(User user) {
            trace.print(Trace.FLOW, "%s unlockPort", portName);
            if(portThread!=null) portThread.unlockPort();
            lockPortLock.lock();
            try {
                if(lockPortUser!=user) {
                    throw new IllegalStateException("not the lockPortUser");
                }
                lockPortUser = null;;
                portUnlock.signal();
            } finally {
                lockPortLock.unlock();
            }
            if(lockPortNotify!=null) {
                portLock.lock();
                try {
                    if(lockPortNotify!=null) lockPortNotify.unlock(); 
                } finally {
                    portLock.unlock();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#registerLockPortNotify(org.epics.ioc.pdrv.LockPortNotify)
         */
        public void registerLockPortNotify(LockPortNotify lockPortNotify) {
            trace.print(Trace.FLOW, "%s registerLockPortNotify", portName);
            portLock.lock();
            try {
                if(lockPortUser==null) throw new IllegalStateException("port not locked");
                if(this.lockPortNotify!=null) {
                    throw new IllegalStateException("already registered");
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
                if(lockPortUser==null) throw new IllegalStateException("port not locked");
                if(this.lockPortNotify==null) {
                    throw new IllegalStateException("not registered");
                }
                lockPortNotify = null;
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#enable(boolean)
         */
        public void enable(boolean trueFalse) {
            trace.print(Trace.FLOW, "%s enable %b", portName,trueFalse);
            portLock.lock();
            try {
                if(lockPortUser==null) throw new IllegalStateException("port not locked");
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
                if(lockPortUser==null) throw new IllegalStateException("port not locked");
                autoConnect = trueFalse;
            } finally {
                portLock.unlock();
            }
            raiseException(ConnectException.autoConnect);
            if(trueFalse) scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#createDevice(org.epics.ioc.pdrv.DeviceDriver, int)
         */
        public Device createDevice(DeviceDriver deviceDriver, int addrNew) {
            trace.print(Trace.FLOW, "%s createDevice addr %d", portName,addrNew);
            DeviceImpl deviceNew = new DeviceImpl(this,deviceDriver,addrNew);
            if(!isMultiDevicePort) {
                if(deviceList.size()!=0) {
                    throw new IllegalStateException("device already created");
                }
                deviceList.add(deviceNew);
                return deviceNew;
            }
            ListIterator<DeviceImpl> iter = deviceList.listIterator();
            while(iter.hasNext()) {
                DeviceImpl device = iter.next();
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
            return deviceNew;

        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortDriver#registerInterface(org.epics.ioc.asyn.Interface)
         */
        public void registerInterface(Interface ifaceNew) {
            String interfaceName = ifaceNew.getInterfaceName();
            trace.print(Trace.FLOW, "%s registerInterface %s", portName,interfaceName);
            PortInterface portInterfaceNew = new PortInterface(ifaceNew);
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

        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.PortDriver#interposeInterface(org.epics.ioc.asyn.Interface)
         */
        public Interface interposeInterface(Interface interposeInterface) {
            String interfaceName = interposeInterface.getInterfaceName();
            trace.print(Trace.FLOW, "%s interposeInterface %s", portName,interfaceName);
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

        private DeviceImpl(Port port,DeviceDriver deviceDriver,int addr) {
            this.port = port;
            this.addr = addr;
            portName = port.getPortName();
        }

        private static class DeviceInterface {
            private Interface iface;
            private Interface interposeInterface = null;

            private DeviceInterface(Interface iface) {
                this.iface = iface;
            }
        }
        
        private static ReentrantLock deviceLock = new ReentrantLock();
        private Port port;
        private String portName;
        private int addr;
        private Trace trace = new TraceImpl();
        private List<DeviceInterface> interfaceList = new LinkedList<DeviceInterface>();

        private List<ConnectExceptionListener> exceptionListenerList = new LinkedList<ConnectExceptionListener>();
        private List<ConnectExceptionListener> exceptionListenerListNew = null;
        private boolean exceptionActive = true;
        
        private User blockingUser = null;

        private boolean autoConnect;
        private boolean connected = false;
        private boolean enabled = true;
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
         * @see org.epics.ioc.pdrv.Device#blockOtherUsers(org.epics.ioc.pdrv.User)
         */
        public Status blockOtherUsers(User user) {
            trace.print(Trace.FLOW, "%s addr %d blockOtherUsers", portName,addr);
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
        public boolean isBlockedByUser(User user) {
            if(blockingUser==null) return false;
            return ((blockingUser==user) ? false : true);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#unblockOtherUsers(org.epics.ioc.pdrv.User)
         */
        public void unblockOtherUsers(User user) {
            trace.print(Trace.FLOW, "%s addr %d unblockOtherUsers", portName,addr);
            if(user!=blockingUser) {
                throw new IllegalStateException("not the blocking asynUser");
            }
            blockingUser = null;
            port.scanQueues();
        }
         /* (non-Javadoc)
          * @see org.epics.ioc.asyn.DeviceUser#report(int)
          */
         public String report(int details) {
             StringBuilder builder = new StringBuilder();
             builder.append(String.format(
                     "    addr %d isBlocked %b autoConnect %b connected %b enabled %b%n",
                     addr,isBlockedByUser(null),autoConnect,connected,enabled
             ));
             return builder.toString();
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
             trace.print(Trace.FLOW, "%s addr %d exceptionListenerAdd", portName,addr);
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
             trace.print(Trace.FLOW, "%s addr %d exceptionListenerRemove", portName,addr);
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
          * @see org.epics.ioc.asyn.DeviceUser#enable(boolean)
          */
         public void enable(boolean trueFalse) {
             trace.print(Trace.FLOW, "%s addr %d enable %b", portName,addr,trueFalse);
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
             trace.print(Trace.FLOW, "%s addr %d autoConnect %b", portName,addr,trueFalse);
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
          * @see org.epics.ioc.asyn.DeviceDriver#registerInterface(org.epics.ioc.asyn.Interface)
          */
         public void registerInterface(Interface ifaceNew) {
             String interfaceName = ifaceNew.getInterfaceName();
             trace.print(Trace.FLOW, "%s addr %d registerInterface %s", portName,addr,interfaceName);
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
             trace.print(Trace.FLOW, "%s addr %d interposeInterface %s", portName,addr,interfaceName);
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
             trace.print(Trace.FLOW, "%s addr %d exceptionConnect", portName,addr);
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
             trace.print(Trace.FLOW, "%s addr %d exceptionDisconnect", portName,addr);
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
        
        private Writer file = new BufferedWriter(new OutputStreamWriter(System.out));
        private TimeStamp timeStamp = new TimeStamp();
        private int mask = Trace.ERROR;
        private int iomask = Trace.IO_NODATA;
        private int truncateSize = DEFAULT_TRACE_TRUNCATE_SIZE;
        
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceFile()
         */
        public synchronized Writer getTraceFile() {
            return file;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceIOMask()
         */
        public synchronized int getTraceIOMask() {
            return iomask;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceIOTruncateSize()
         */
        public synchronized int getTraceIOTruncateSize() {
            return truncateSize;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#getTraceMask()
         */
        public synchronized int getTraceMask() {
            return mask;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceFile(java.io.Writer)
         */
        public synchronized void setTraceFile(Writer file) {
            this.file = file;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOMask(int)
         */
        public synchronized void setTraceIOMask(int mask) {
            iomask = mask;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOTruncateSize(long)
         */
        public synchronized void setTraceIOTruncateSize(int size) {
            truncateSize = size;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceMask(int)
         */
        public synchronized void setTraceMask(int mask) {
            this.mask = mask;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#print(int, java.lang.String, java.lang.Object[])
         */
        public synchronized void print(int reason, String format, Object... args) {
            if((reason&mask)==0) return;
            try {
                file.write(getTime() + String.format(format, args));
            }catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#printIO(int, byte[], long, java.lang.String, java.lang.Object[])
         */
        public synchronized void printIO(int reason, byte[] buffer, long len, String format, Object... args) {
            if((reason&mask)==0) return;
            try {
                file.write(getTime() + String.format(format, args));
                if(iomask==0)return;
                int index = 0;
                StringBuilder builder = new StringBuilder();
                while(builder.length()<truncateSize && index<len) {
                    if((iomask&Trace.IO_ASCII)!=0) {
                        builder.append(buffer[index]);
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
            }catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        
        private String getTime() {
            TimeUtility.getMillis(timeStamp);
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
    
    private static final int numQueuePriorities = QueuePriority.connect.ordinal() + 1;
    private static final int queuePriorityConnect = QueuePriority.connect.ordinal();
    private static final int queuePriorityHigh = QueuePriority.high.ordinal();
    private static final int queuePriorityLow = QueuePriority.low.ordinal();
    
    private static class PortThread implements Runnable  {
        private static UserImpl portUser = new UserImpl(null);
        private Thread thread = new Thread(this);
        private ReentrantLock queueLock = new ReentrantLock();
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        private Condition waitScanDone = lock.newCondition();
        private PortImpl port;
        private boolean lockingPort = false;
        private boolean portLocked = false;
        private boolean scanActive = false;
        
        private List<UserImpl> queuePortList = new ArrayList<UserImpl>();
        private List<UserImpl>[] queueDeviceListArray = new ArrayList[numQueuePriorities];
        
        private PortThread(PortImpl port,ScanPriority scanPriority) {
            this.port = port;
            for(int i=0; i<queueDeviceListArray.length; i++) {
                queueDeviceListArray[i] = new ArrayList<UserImpl>();
            }
            thread.setPriority(scanPriority.getJavaPriority());
            thread.start();
        }
        
        private void lockPort() {
            lock.lock();
            try {
                if(portLocked) {
                    throw new IllegalStateException("already locked");
                }
                portLocked = true;
                lockingPort = true;
                if(scanActive) {
                    try {
                        waitScanDone.await();
                    } catch (InterruptedException e){}
                }
            } finally {
                lock.unlock();
            }
        }
        
        private void unlockPort() {
            lock.lock();
            try {
                if(!portLocked) {
                    throw new IllegalStateException("not locked");
                }
                portLocked = false;
                moreWork.signal();
            } finally {
                lock.unlock();
            }
        }
        
        private void queuePortRequest(UserImpl user) {
            queueLock.lock();
            try {
                if(user.isQueued) {
                    throw new IllegalStateException("prevous queueRequest not complete");
                }
                user.isQueued = true;
                queuePortList.add(user);
            } finally {
                queueLock.unlock();
            }
            scanQueues();
        }
        private void queueDeviceRequest(UserImpl user,QueuePriority asynQueuePriority)
        {
            queueLock.lock();
            try {
                if(user.isQueued) {
                    throw new IllegalStateException("prevous queueRequest not complete");
                }
                user.isQueued = true;
                List<UserImpl> list = queueDeviceListArray[asynQueuePriority.ordinal()];
                list.add(user);
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
        
        private void cancelRequest(UserImpl asynUserPvtCancel)
        {
            UserImpl user = null;
            queueLock.lock();
            try {
                List<UserImpl> list = null;
                ListIterator<UserImpl> iter = null;
                iter = queuePortList.listIterator();
                while(iter.hasNext()) {
                    user = iter.next();
                    if(user==asynUserPvtCancel) {
                        iter.remove();
                        user.isQueued = false;
                        return;
                    }
                }
                for(int i=queuePriorityHigh; i>=queuePriorityLow; i--) {
                    list = queueDeviceListArray[i];
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
            } finally {
                queueLock.unlock();
            }
        }
        
        public void run() {
            try {
                while(true) {
                    lock.lock();
                    try {
                        scanActive = false;
                        if(lockingPort) {
                            lockingPort = false;
                            waitScanDone.signal();
                            continue;
                        }
                        if(portLocked) return;
                        moreWork.await();
                        scanActive = true;
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
            boolean portList = true;
            int priority = queuePriorityConnect;
            outer:
            while(true) {     
                if(user!=null) {
                    user.queueRequestCallback.callback(user);
                }
                if(!portList) {
                    if(!port.connected && port.autoConnect) {
                        if(!autoConnect()) return;
                    }
                }
                queueLock.lock();
                try {
                    if(portLocked) return;
                    if(!port.enabled) return;
                    if(portList) {
                        user = nextPortQueueElement(queuePortList);
                        if(user!=null) continue outer;
                        portList = false;
                        if(!port.connected) continue outer;
                    }
                    for(int i=priority; i>=queuePriorityLow; i--) {
                        priority = i;
                        user = nextDeviceQueueElement(queueDeviceListArray[i]);
                        if(user!=null) continue outer;
                    }
                    return;
                } finally {
                    queueLock.unlock();
                }
            }
        }
        
        private UserImpl nextPortQueueElement(List<UserImpl> list) {
            int size = list.size();
            if(size<=0) return null;
            UserImpl user = list.get(0);
            Port port = user.port;
            if(port.isBlockedByUser(user)) {
                boolean returnNull = true;
                for(int i=1; i<size; i++) {
                    user = list.get(i);
                    port = user.port;
                    if(port.isBlockedByUser(user)) continue;
                    list.remove(i);
                    break;
                }
                if(returnNull) return null;
            } else {
                list.remove(0);
                
            }
            user.isQueued = false;
            if(user.isDeleted) return null;
            return user;
        }
        
        private UserImpl nextDeviceQueueElement(List<UserImpl> list) {
            int size = list.size();
            if(size<=0) return null;
            UserImpl user = list.get(0);
            Device asynDevice = user.device;
            if(asynDevice.isBlockedByUser(user)) {
                boolean returnNull = true;
                for(int i=1; i<size; i++) {
                    user = list.get(i);
                    asynDevice = user.device;
                    if(asynDevice.isBlockedByUser(user)) continue;
                    list.remove(i);
                    break;
                }
                if(returnNull) return null;
            } else {
                list.remove(0);
                
            }
            user.isQueued = false;
            if(user.isDeleted) return null;
            return user;
        }
        
        private boolean autoConnect() {
            Status status = port.portDriver.connect(portUser);
            if(status==Status.success) return true;
            return false;
        }
    }
}
