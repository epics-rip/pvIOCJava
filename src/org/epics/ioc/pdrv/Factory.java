/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.util.RunnableReady;
import org.epics.ioc.util.ScanPriority;
import org.epics.ioc.util.ThreadCreate;
import org.epics.ioc.util.ThreadCreateFactory;
import org.epics.ioc.util.ThreadReady;
import org.epics.ioc.util.TimeStamp;
import org.epics.ioc.util.TimeUtility;

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
     * @param canBlock Can this port block while performing I/O.
     * @param autoConnect Initial state for autoConnect for the port and its devices.
     * @param priority If the port can block this is the priority for the portThread.
     * @return The interface for the Port instance.
     */
    public static Port createPort(String portName,PortDriver portDriver,String driverName,
            boolean canBlock,boolean autoConnect,ScanPriority priority)
    { 
        PortImpl port = new PortImpl(
                portName,portDriver,driverName,canBlock,autoConnect,priority);
        globalLock.lock();
        try {
            if(portList.contains(port)){
                throw new IllegalStateException("port already exists"); 
            }
            portList.add(port);
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
    /**
     * Get the port interface.
     * @param portName The name of the port.
     * @return The interface or null if the port does not exits.
     */
    public static Port getPort(String portName) {
        globalLock.lock();
        try {
            for(Port port : portList) {
                if(port.getPortName().equals(portName)) return port;
            }
            return null;
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
        private boolean isQueued = false;
        
        private Object userPvt = null;
        private Object portDriverPvt = null;
        private Object deviceDriverPvt = null;
        private int reason;
        private double timeout;
        
        private int auxStatus;
        private String message = null;
        private boolean booleanvalue = false;
        private int intValue;
        private double doubleValue;
        private String stringValue = null;;
        
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
        	clearErrorParms();
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
            if(port==null) return;
            port.exceptionListenerRemove(this);
            port.trace.optionChangeListenerRemove(this);
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
         * @see org.epics.ioc.pdrv.User#connectDevice(java.lang.String)
         */
        public Device connectDevice(String deviceName) {
        	clearErrorParms();
            if(device!=null) {
                setMessage("already connected");
                return null;
            }
            if(port==null) {
                setMessage("not connected to a port");
                return null;
            }
            device = (DeviceImpl)port.getDevice(this, deviceName);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#disconnectDevice()
         */
        public void disconnectDevice() {
        	clearErrorParms();
            if(device==null) return;
            cancelRequest();
            device.exceptionListenerRemove(this);
            device.trace.optionChangeListenerRemove(this);
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
        	clearErrorParms();
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
                Status status = lockPort();
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
        	clearErrorParms();
            if(port==null) {
                setMessage("not connected to a port");
                return Status.error;
            }
            Status status = Status.success;
            if(device!=null) {
                status = device.lockPort(this);
            } else {
                status = port.lockPort(this);
            }
            if(status!=Status.success) return status;
            if(!port.isConnected()) {
                if(port.isAutoConnect()) {
                    status = port.connect(this);
                } else {
                    status = Status.error;
                }
                if(status!=Status.success) {
                    setMessage("port is not connected");
                    port.unlockPort(this);
                    return status;
                }
            }
            if(device!=null && !device.isConnected()) {
                if(device.isAutoConnect()) {
                    status = device.connect(this);
                } else {
                    status = Status.error;
                }
                if(status!=Status.success) {
                    setMessage("device is not connected");
                    port.unlockPort(this);
                    return status;
                }
            }
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#lockPortForConnect()
         */
        public Status lockPortForConnect() {
            clearErrorParms();
            if(port==null) {
                setMessage("not connected to a port");
                return Status.error;
            }
            Status status = port.lockPort(this);
            if(status==Status.success) {
                if(port.connected) {
                    setMessage("port already connected");
                    status = Status.error;
                    port.unlockPort(this);
                }
            }
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#lockDeviceForConnect()
         */
        public Status lockDeviceForConnect() {
            clearErrorParms();
            if(device==null) {
                setMessage("not connected to a device");
                return Status.error;
            }
            Status status = device.lockPort(this);
            if(status==Status.success) {
                if(device.connected) {
                    setMessage("device already connected");
                    status = Status.error;
                    port.unlockPort(this);
                }
            }
            return status;
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
         * @see org.epics.ioc.pdrv.User#getBoolean()
         */
        public boolean getBoolean() {
            return booleanvalue;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.User#setBoolean(boolean)
         */
        public void setBoolean(boolean value) {
            booleanvalue = value;
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
        
        private void clearErrorParms() {
        	auxStatus = 0;
        	message = null;
        }
    }
    
    private static class PortImpl implements Port {
        
        private PortImpl(String portName,PortDriver portDriver,String driverName,
            boolean canBlock,boolean autoConnect,ScanPriority priority)
        {
            this.portName = portName;
            this.portDriver = portDriver;
            this.driverName = driverName;
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
        private boolean autoConnect;        
        private PortThread portThread = null;        
        
        private boolean connected = false;
        private boolean enabled = true;
        
        private List<DeviceImpl> deviceList = new LinkedList<DeviceImpl>();
        private ConnectExceptionList exceptionList = new ConnectExceptionList();
        private boolean exceptionActive = false;
        
        private UserImpl lockPortUser = null;
        private ReentrantLock lockPortLock = new ReentrantLock();
        private Condition portUnlock = lockPortLock.newCondition();
        
        private LockPortNotify lockPortNotify = null;   

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
                        "    autoConnect %b connected %b enabled %b%n",autoConnect,connected,enabled));
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
         * @see org.epics.ioc.pdrv.Port#canBlock()
         */
        public boolean canBlock() {
            return ((portThread==null) ? true : false);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#enable(boolean)
         */
        public void enable(boolean trueFalse) {
            boolean changed = false;
            portLock.lock();
            try {
                if(enabled!=trueFalse) {
                    enabled = trueFalse;
                    changed = true;
                }

            } finally {
                portLock.unlock();
            }
            trace.print(Trace.FLOW, "%s enable %b wasChanged %b", portName,trueFalse,changed);
            if(changed) {
                raiseException(ConnectException.enable);
                if(trueFalse) scanQueues();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#autoConnect(boolean)
         */
        public void autoConnect(boolean trueFalse) {
            boolean changed = false;
            trace.print(Trace.FLOW, "%s autoConnect %b", portName,trueFalse);
            portLock.lock();
            try {
                if(autoConnect!=trueFalse) {
                    autoConnect = trueFalse;
                    changed = true;
                }
            } finally {
                portLock.unlock();
            }
            trace.print(Trace.FLOW, "%s autoConnect %b was changed %b", portName,trueFalse,changed);
            if(changed) {
                raiseException(ConnectException.autoConnect);
                if(trueFalse) scanQueues();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#getDevice(org.epics.ioc.pdrv.User, java.lang.String)
         */
        public Device getDevice(User user, String deviceName) {
            portLock.lock();
            try {
                int size = deviceList.size();
                for(int i=0; i<size; i++) {
                    Device device = deviceList.get(i);
                    if(device.getDeviceName().equals(deviceName)) return device;
                }
            } finally {
                portLock.unlock();
            }
            lockPort((UserImpl)user);
            try {
                return portDriver.createDevice(user, deviceName);
            } finally {
                unlockPort((UserImpl)user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#connect(org.epics.ioc.pdrv.User)
         */
        public Status connect(User user) {         
            if(lockPortUser!=user) {
                user.setMessage("Illegal to call connect without owning the port");
                return Status.error;
            }
            if(connected) {
                user.setMessage("already connected");
                return Status.error;
            }
            trace.print(Trace.FLOW, "%s connect", portName);
            Status status = portDriver.connect(user);
            if(status==Status.success && portThread!=null) portThread.scanQueues();
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#disconnect(org.epics.ioc.pdrv.User)
         */
        public Status disconnect(User user) {
            if(lockPortUser!=user) {
                user.setMessage("Illegal to call disconnect without owning the port");
                return Status.error;
            }
            if(!connected) {
                user.setMessage("not connected");
                return Status.error;
            }
            trace.print(Trace.FLOW, "%s disconnect", portName);
            Status status = portDriver.disconnect(user);
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
         * @see org.epics.ioc.pdrv.Port#exceptionListenerAdd(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.ConnectExceptionListener)
         */
        public Status exceptionListenerAdd(User user,ConnectExceptionListener listener)
        {
            trace.print(Trace.FLOW, "%s exceptionListenerAdd", portName);
            portLock.lock();
            try {
                if(!exceptionActive) {
                    return exceptionList.add(user, listener);

                } else {
                    return exceptionList.addNew(user, listener);
                }          
            } finally {
                portLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Port#exceptionListenerRemove(org.epics.ioc.pdrv.User)
         */
        public void exceptionListenerRemove(User user) {
            trace.print(Trace.FLOW, "%s exceptionListenerRemove", portName);
            portLock.lock();
            try {
                exceptionList.remove(user);
            } finally {
                portLock.unlock();
            }
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
         * @see org.epics.ioc.pdrv.Port#createDevice(org.epics.ioc.pdrv.DeviceDriver, java.lang.String)
         */
        public Device createDevice(DeviceDriver deviceDriver, String deviceName) {
            trace.print(Trace.FLOW, "%s createDevice %s", portName,deviceName);
            DeviceImpl deviceNew = new DeviceImpl(this,deviceDriver,deviceName);
            portLock.lock();
            try {
                ListIterator<DeviceImpl> iter = deviceList.listIterator();
                while(iter.hasNext()) {
                    Device device = iter.next();
                    if(deviceName.equals(device.getDeviceName())) {
                        throw new IllegalStateException(
                                deviceName + " already registered");
                    }
                }
                deviceList.add(deviceNew);
            } finally {
                portLock.unlock();
            }
            return deviceNew;
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
                if(!connected) {
                    throw new IllegalStateException("already disconnected");
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
                while(true) {
                    if(!enabled) {
                        user.message = "port disabled";
                        return Status.error;
                    }
                    if(lockPortUser==null) break;
                    if(lockPortUser==user) {
                        user.message = "already locked. Illegal request";
                        return Status.error;
                    }
                    try {
                        portUnlock.await();
                    } catch (InterruptedException e){}
                }
                lockPortUser = user;
            } finally {
                lockPortLock.unlock();
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
            exceptionList.raiseException(connectException);
            portLock.lock();
            try {
                exceptionList.merge();
                exceptionActive = false; 
            } finally {
                portLock.unlock();
            }
        }
    }
    
    private static class DeviceImpl implements Device {

        private DeviceImpl(PortImpl port,DeviceDriver deviceDriver,String deviceName) {
            this.port = port;
            this.deviceDriver = deviceDriver;
            this.deviceName = deviceName;
            portName = port.getPortName();
            fullName = portName + "[" + deviceName + "]";
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
        private String deviceName;
        private String fullName;
        private Trace trace = new TraceImpl();
        private List<DeviceInterface> interfaceList = new LinkedList<DeviceInterface>();
        private ConnectExceptionList exceptionList = new ConnectExceptionList();
        private boolean exceptionActive = false;
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
                    "     %s isBlocked %b autoConnect %b connected %b enabled %b%n",
                    fullName,isBlockedByOtherUser(null),autoConnect,connected,enabled
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
         * @see org.epics.ioc.pdrv.Device#getDeviceName()
         */
        public String getDeviceName() {
            return deviceName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#getFullName()
         */
        public String getFullName() {
            return fullName;
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
            boolean changed = false;
            deviceLock.lock();
            try {
                if(enabled!=trueFalse) {
                    enabled = trueFalse;
                    changed = true;
                }
            } finally {
                deviceLock.unlock();
            }
            trace.print(Trace.FLOW, "%s enable %b was changed %b", fullName,trueFalse,changed);
            if(changed) {
                raiseException(ConnectException.enable);
                if(trueFalse) port.scanQueues();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.DeviceUser#autoConnect(boolean)
         */
        public void autoConnect(boolean trueFalse) {
            boolean changed = false;
            deviceLock.lock();
            try {
                if(autoConnect!=trueFalse) {
                    autoConnect = trueFalse;
                    changed = true;
                }
            } finally {
                deviceLock.unlock();
            }
            trace.print(Trace.FLOW, "%s autoConnect %b was changed %b", fullName,trueFalse,changed);
            if(changed) {
                raiseException(ConnectException.autoConnect);
                if(trueFalse) port.scanQueues();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#connect(org.epics.ioc.pdrv.User)
         */
        public Status connect(User user) {
            trace.print(Trace.FLOW, "%s connect", fullName);
            PortImpl port = (PortImpl)user.getPort();
            if(port.lockPortUser!=user) {
                user.setMessage("Illegal to call connect without owning the port");
                return Status.error;
            }
            if(connected) {
                user.setMessage("already connected");
                return Status.error;
            }
            return deviceDriver.connect(user);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#disconnect(org.epics.ioc.pdrv.User)
         */
        public Status disconnect(User user) {
            trace.print(Trace.FLOW, "%s disconnect", fullName);
            PortImpl port = (PortImpl)user.getPort();
            if(port.lockPortUser!=user) {
                user.setMessage("Illegal to call disconnect without owning the port");
                return Status.error;
            }
            if(!connected) {
                user.setMessage("not connected");
                return Status.error;
            }
            return deviceDriver.disconnect(user);
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
         * @see org.epics.ioc.pdrv.Device#exceptionListenerAdd(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.ConnectExceptionListener)
         */
        public Status exceptionListenerAdd(User user,ConnectExceptionListener listener)
        {
            trace.print(Trace.FLOW, "%s exceptionListenerAdd", portName);
            deviceLock.lock();
            try {
                if(!exceptionActive) {
                    return exceptionList.add(user, listener);
                } else {
                    return exceptionList.addNew(user, listener);
                }          
            } finally {
                deviceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Device#exceptionListenerRemove(org.epics.ioc.pdrv.User)
         */
        public void exceptionListenerRemove(User user) {
            trace.print(Trace.FLOW, "%s exceptionListenerRemove", portName);
            deviceLock.lock();
            try {
                exceptionList.remove(user);
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
                    if(interfaceName.equals(asynInterface.getInterfaceName())) {
                        if(interposeInterfaceOK && iface.interposeInterface!=null) {
                            return iface.interposeInterface;
                        }
                        return asynInterface;
                    }
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
            trace.print(Trace.FLOW, "%s blockOtherUsers", fullName);
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
            trace.print(Trace.FLOW, "%s unblockOtherUsers", fullName);
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
            trace.print(Trace.FLOW, "%s registerInterface %s", fullName,interfaceName);
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
            trace.print(Trace.FLOW, "%s interposeInterface %s", fullName,interfaceName);
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
            trace.print(Trace.FLOW, "%s exceptionConnect", fullName);
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
            trace.print(Trace.FLOW, "%s exceptionDisconnect", fullName);
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
            trace.print(Trace.FLOW, "%s device.lockPort", fullName);
            Status status = port.lockPort((UserImpl)user);
            if(status!=Status.success) return status;
            if(!enabled) {
                port.unlockPort((UserImpl)user);
                user.setMessage("device disabled");
                return Status.error;
            }
            if(blockingUser!=null&&blockingUser!=user) {
                port.unlockPort((UserImpl)user);
                user.setMessage("device blocked by other user");
                return Status.error;
            }
            return Status.success;
        }
        
        private void raiseException(ConnectException connectException) {
            deviceLock.lock();
            try {
                exceptionActive = true;
            } finally {
                deviceLock.unlock();
            }
            exceptionList.raiseException(connectException);
            deviceLock.lock();
            try {
                exceptionList.merge();
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
        private int mask = Trace.ERROR;//|Trace.SUPPORT|Trace.DRIVER;//|Trace.FLOW;
        private int iomask = Trace.IO_NODATA;//|Trace.IO_ASCII;
        private int truncateSize = DEFAULT_TRACE_TRUNCATE_SIZE;
        
        private TraceOptionChangeList optionChangeList = new TraceOptionChangeList();
        private boolean optionChangeActive = false;
        
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
            } finally {
                traceLock.unlock();
            }
            raiseException();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.asyn.Trace#setTraceIOMask(int)
         */
        public void setIOMask(int mask) {
            traceLock.lock();
            try {
                iomask = mask;
            } finally {
                traceLock.unlock();
            }
            raiseException();
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
         * @see org.epics.ioc.pdrv.Trace#optionChangeListenerAdd(org.epics.ioc.pdrv.User, org.epics.ioc.pdrv.TraceOptionChangeListener)
         */
        public Status optionChangeListenerAdd(User user,TraceOptionChangeListener listener) {
            traceLock.lock();
            try {
                if(!optionChangeActive) {
                    return optionChangeList.add(user, listener);
                } else {
                    return optionChangeList.addNew(user, listener);
                }
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#optionChangeListenerRemove(org.epics.ioc.pdrv.User)
         */
        public void optionChangeListenerRemove(User user) {
            traceLock.lock();
            try {
                optionChangeList.remove(user);
            } finally {
                traceLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pdrv.Trace#print(int, java.lang.String)
         */
        public void print(int reason, String message) {
            print(reason," %s",message);
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
            optionChangeList.raiseException();
            traceLock.lock();
            try {
                optionChangeList.merge();
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
    
    private static class ConnectExceptionList {
        Status add(User user, ConnectExceptionListener listener) {
            for(ConnectExceptionNode node : list) {
                if(node.user==user) {
                    user.setMessage("already a ConnectExceptionListener");
                    return Status.error;
                }
            }
            ConnectExceptionNode node = new ConnectExceptionNode(user,listener);
            list.add(node);
            return Status.success;
        }
        Status addNew(User user, ConnectExceptionListener listener) {
            for(ConnectExceptionNode node : list) {
                if(node.user==user) {
                    user.setMessage("already a ConnectExceptionListener");
                    return Status.error;
                }
            }
            if(listNew==null) {
                listNew = new LinkedList<ConnectExceptionNode>();
            } else {
                for(ConnectExceptionNode node : listNew) {
                    if(node.user==user) {
                        user.setMessage("already a ConnectExceptionListener");
                        return Status.error;
                    }
                }
            }
            ConnectExceptionNode node = new ConnectExceptionNode(user,listener);
            listNew.add(node);
            return Status.success;
        }
        void raiseException(ConnectException connectException) {
            ListIterator<ConnectExceptionNode> iter =  list.listIterator();
            while(iter.hasNext()) {
                ConnectExceptionNode node = iter.next();
                node.listener.exception(connectException);
            }
        }
        void merge() {
            if(listNew!=null) {
                list.addAll(listNew);
                listNew = null;
            }
        }
        void remove(User user) {
            ListIterator<ConnectExceptionNode> iter =  list.listIterator();
            while(iter.hasNext()) {
                ConnectExceptionNode node = iter.next();
                if(node.user==user) {
                    iter.remove();
                    return;
                }
            }
            if(listNew==null) return;
            iter =  listNew.listIterator();
            while(iter.hasNext()) {
                ConnectExceptionNode node = iter.next();
                if(node.user==user) {
                    iter.remove();
                    return;
                }
            }
        }
        private static class ConnectExceptionNode {
            User user;
            ConnectExceptionListener listener;
            ConnectExceptionNode(User user,ConnectExceptionListener listener) {
                this.user = user;
                this.listener = listener;
            }
        }
        private List<ConnectExceptionNode> list = new LinkedList<ConnectExceptionNode>();
        private List<ConnectExceptionNode> listNew = null;
    }
    
    private static class TraceOptionChangeList {
        Status add(User user, TraceOptionChangeListener listener) {
            for(TraceOptionChangeNode node : list) {
                if(node.user==user) {
                    user.setMessage("already a TraceOptionChangeListener");
                    return Status.error;
                }
            }
            TraceOptionChangeNode node = new TraceOptionChangeNode(user,listener);
            list.add(node);
            return Status.success;
        }
        Status addNew(User user, TraceOptionChangeListener listener) {
            for(TraceOptionChangeNode node : list) {
                if(node.user==user) {
                    user.setMessage("already a TraceOptionChangeListener");
                    return Status.error;
                }
            }
            if(listNew==null) {
                listNew = new LinkedList<TraceOptionChangeNode>();
            } else {
                for(TraceOptionChangeNode node : listNew) {
                    if(node.user==user) {
                        user.setMessage("already a TraceOptionChangeListener");
                        return Status.error;
                    }
                }
            }
            TraceOptionChangeNode node = new TraceOptionChangeNode(user,listener);
            listNew.add(node);
            return Status.success;
        }
        void raiseException() {
            ListIterator<TraceOptionChangeNode> iter =  list.listIterator();
            while(iter.hasNext()) {
                TraceOptionChangeNode node = iter.next();
                node.listener.optionChange();
            }
        }
        void merge() {
            if(listNew!=null) {
                list.addAll(listNew);
                listNew = null;
            }
        }
        void remove(User user) {
            ListIterator<TraceOptionChangeNode> iter =  list.listIterator();
            while(iter.hasNext()) {
                TraceOptionChangeNode node = iter.next();
                if(node.user==user) {
                    iter.remove();
                    return;
                }
            }
            if(listNew==null) return;
            iter =  listNew.listIterator();
            while(iter.hasNext()) {
                TraceOptionChangeNode node = iter.next();
                if(node.user==user) {
                    iter.remove();
                    return;
                }
            }
        }
        private static class TraceOptionChangeNode {
            User user;
            TraceOptionChangeListener listener;
            TraceOptionChangeNode(User user,TraceOptionChangeListener listener) {
                this.user = user;
                this.listener = listener;
            }
        }
        private List<TraceOptionChangeNode> list = new LinkedList<TraceOptionChangeNode>();
        private List<TraceOptionChangeNode> listNew = null;
    }
    
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static final int queuePriorityHigh = QueuePriority.high.ordinal();
    private static final int numQueuePriorities = queuePriorityHigh + 1;
    private static final int queuePriorityLow = QueuePriority.low.ordinal();
    private static final int queuePriorityMedium = QueuePriority.medium.ordinal();
    
    private static class PortThread implements RunnableReady  {
        private ReentrantLock queueLock = new ReentrantLock();
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        private PortImpl port;
        
        private List<UserImpl>[] queueListArray = new ArrayList[numQueuePriorities];  
        private List<UserImpl> waitUnblockList = new ArrayList<UserImpl>();   
        
        private PortThread(PortImpl port,ScanPriority scanPriority) {
            this.port = port;
            for(int i=0; i<queueListArray.length; i++) {
                queueListArray[i] = new ArrayList<UserImpl>();
            }
            threadCreate.create(port.getPortName(), scanPriority.getJavaPriority(), this);
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
        /* (non-Javadoc)
         * @see org.epics.ioc.util.RunnableReady#run(org.epics.ioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            boolean firstTime = true;
            try {
                while(true) {
                    lock.lock();
                    try {  
                        if(firstTime) {
                            firstTime = false;
                            threadReady.ready();
                        }
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
                    if(!port.isConnected()) {
                        if(port.isAutoConnect()) {
                            status = port.connect(user);
                        } else {
                            user.setMessage("port is not connected");
                            status = Status.error;
                        }
                    }
                    if(status==Status.success && device!=null && !device.isConnected()) {
                        if(device.isAutoConnect()) {
                            status = device.connect(user);
                        } else {
                            user.setMessage("device is not connected");
                            status = Status.error;
                        }
                    }
                }
                try {
                    queueRequestCallback.callback(status,user);
                } finally {
                    port.unlockPort(user);
                }
                user = null;
            }
        }
        
    }
}
