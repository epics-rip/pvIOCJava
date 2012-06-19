/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;

import java.io.Writer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.misc.RunnableReady;
import org.epics.pvdata.misc.ThreadCreate;
import org.epics.pvdata.misc.ThreadCreateFactory;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.ThreadReady;
import org.epics.pvdata.misc.Timer;
import org.epics.pvdata.misc.TimerFactory;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.install.AfterStartFactory;
import org.epics.pvioc.install.AfterStartNode;
import org.epics.pvioc.install.AfterStartRequester;
import org.epics.pvioc.install.NewAfterStartRequester;
import org.epics.pvioc.pdrv.interfaces.Interface;


/**
 * Factory for portDriver.
 * @author mrk
 *
 */
public class Factory {
    /**
     * Create a User, which is the "handle" for communicating with port and device drivers.
     * @param queueRequestCallback The user callback for user.queueRequest.
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
            boolean canBlock,boolean autoConnect,ThreadPriority priority)
    { 
        PortImpl port = new PortImpl(
                portName,portDriver,driverName,canBlock,autoConnect,priority);
        synchronized(portList) {
            LinkedListNode<PortImpl> node = portList.getHead();
            while(node!=null) {
                if(node.getObject().portName.equals(portName)) {
                	System.err.printf("port %s already exists%n",portName);
                    throw new IllegalStateException("port already exists");
                }
                node = portList.getNext(node);
            }
            portList.addTail(port.portListNode);
        }
        if(autoConnect) port.startAutoConnect(.1);
        return port;
    }
    /**
     * Get an array of the portNames.
     * @return The array.
     */
    public static String[] getPortNames() {
        synchronized(portList) {
            String[] portNames = new String[portList.getLength()];
            LinkedListNode<PortImpl> node = portList.getHead();
            int index = 0;
            while(node!=null) {
                Port port = node.getObject();
                portNames[index++] = port.getPortName();
                node = portList.getNext(node);
            }
            return portNames;
        }
    }
    /**
     * Get the port interface.
     * @param portName The name of the port.
     * @return The interface or null if the port does not exits.
     */
    public static Port getPort(String portName) {
        synchronized(portList) {
            LinkedListNode<PortImpl> node = portList.getHead();
            while(node!=null) {
                PortImpl port = node.getObject();
                if(port.getPortName().equals(portName)) return port;
                node = portList.getNext(node);
            }
            return null;
        }
    }
    
    private static LinkedListCreate<PortImpl> portListCreate = new LinkedListCreate<PortImpl>();
    private static LinkedList<PortImpl> portList = portListCreate.create();
    private static LinkedListCreate<DeviceImpl> deviceListCreate = new LinkedListCreate<DeviceImpl>();
    private static LinkedListCreate<Interface> interfaceListCreate = new LinkedListCreate<Interface>();
    private static LinkedListCreate<UserImpl> userListCreate = new LinkedListCreate<UserImpl>();
    private static Timer timer = TimerFactory.create("portDriverAutoconnectTimer",ThreadPriority.lower);
    private static final double autoConnectPeriod = 10.0; // 10 seconds
    
    private static class UserImpl implements User {
        
        private UserImpl(QueueRequestCallback queueRequestCallback) {
            this.queueRequestCallback = queueRequestCallback;
        }
        
        
        private QueueRequestCallback queueRequestCallback = null;
        // listNode and isQueued are for PortThread
        private LinkedListNode<UserImpl> listNode = userListCreate.createNode(this);
        private boolean isQueued = false;
        
        private PortImpl port = null;
        private DeviceImpl device = null;
        
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
         * @see org.epics.pvioc.pdrv.User#duplicateUser(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.QueueRequestCallback)
         */
        @Override
        public User duplicateUser(QueueRequestCallback queueRequestCallback) {
            UserImpl newUser = new UserImpl(queueRequestCallback);
            newUser.port = port;
            newUser.device = device;
            newUser.reason = reason;
            newUser.timeout = timeout;
            return newUser;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#connectPort(java.lang.String)
         */
        @Override
        public Port connectPort(String portName) {
        	clearErrorParms();
            if(port!=null) {
                setMessage("already connected to port");
                return null;
            }
            synchronized(portList) {
                LinkedListNode<PortImpl> node = portList.getHead();
                while(node!=null) {
                    PortImpl port = node.getObject();
                    if(port.getPortName().equals(portName)) {
                        this.port = port;
                        return port;
                    }
                    node = portList.getNext(node);
                }
                setMessage("portName " + portName + " not registered");
                return null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#disconnectPort()
         */
        @Override
        public void disconnectPort() {
            disconnectDevice();
            if(port==null) return;
            port.exceptionListenerRemove(this);
            port.trace.optionChangeListenerRemove(this);
            port = null;
            portDriverPvt = null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getPort()
         */
        @Override
        public Port getPort() {
            return port;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#connectDevice(java.lang.String)
         */
        @Override
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
         * @see org.epics.pvioc.pdrv.User#disconnectDevice()
         */
        @Override
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
         * @see org.epics.pvioc.pdrv.User#getDevice()
         */
        @Override
        public Device getDevice() {
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#queueRequest(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.QueuePriority)
         */
        @Override
        public void queueRequest(QueuePriority queuePriority) { 
            if(queueRequestCallback==null) {
                throw new IllegalStateException("queueRequestCallback is null");
            }
        	clearErrorParms();
        	if(port==null) {
        	    setMessage("not connected to a port");
        	    queueRequestCallback.callback(Status.error,this);
                return;
        	}
            Trace trace = port.getTrace();
            String portName = port.getPortName();
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s User.queueRequest",portName);
            }
            boolean notConnected = true;
            if(!port.isConnected()) {
                setMessage("port is not connected");
            } else if(device==null) {
                setMessage("not connected to a device");
            } else if(!device.isConnected()) {
                setMessage("device is not connected");
            } else {
                notConnected = false;
            }
            if(notConnected) {
                if((trace.getMask()&Trace.FLOW)!=0) {
                    trace.print(Trace.FLOW, "%s User.queueRequest calling callback with not connected message",portName);
                }
                queueRequestCallback.callback(Status.error,this);
                return;
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
                    if((trace.getMask()&Trace.FLOW)!=0) {
                        trace.print(Trace.FLOW, "%s User.queueRequest calling queueRequestCallback", portName);
                    }
                    queueRequestCallback.callback(Status.success,this);
                } finally {
                    unlockPort();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#cancelRequest(org.epics.pvioc.pdrv.User)
         */
        @Override
        public void cancelRequest() {
            if(port==null) return;
            Trace trace = port.getTrace();
            String portName = port.portName;
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s User.cancelRequest", portName);
            }
            PortThread portThread = port.portThread;
            if(portThread!=null) portThread.cancelRequest(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#lockPort(org.epics.pvioc.pdrv.User)
         */
        @Override
        public Status lockPort() {
            clearErrorParms();
            if(port==null) {
                setMessage("not connected to a port");
                return Status.error;
            }
            Trace trace = port.getTrace();
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s User.lockPort", port.getPortName());
            }
            Status status = Status.success;
            if(device!=null) {
                status = device.lockPort(this);
            } else {
                status = port.lockPort(this);
            }
            if(status!=Status.success) return status;
            if(!port.isConnected()) {
                setMessage("port is not connected");
                port.unlockPort(this);
                return Status.error;
            }
            if(device!=null && !device.isConnected()) {
                setMessage("device is not connected");
                port.unlockPort(this);
                return Status.error;
            }
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#lockPortForConnect()
         */
        @Override
        public Status lockPortForConnect() {
            clearErrorParms();
            if(port==null) {
                setMessage("not connected to a port");
                return Status.error;
            }
            Trace trace = port.getTrace();
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s User.lockPortForConnect", port.getPortName());
            }
            Status status = port.lockPort(this);
            if(status==Status.success) {
                if(port.connected) {
                    setMessage("port already connected");
                    status = Status.error;
                    port.unlockPort(this);
                }
                if(port.autoConnect) {
                    setMessage("port is autoConnect");
                    status = Status.error;
                    port.unlockPort(this);
                }
            }
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#lockDeviceForConnect()
         */
        @Override
        public Status lockDeviceForConnect() {
            clearErrorParms();
            if(device==null) {
                setMessage("not connected to a device");
                return Status.error;
            }
            Trace trace = port.getTrace();
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s User.lockDeviceForConnect", port.getPortName());
            }
            Status status = device.lockPort(this);
            if(status==Status.success) {
                if(device.connected) {
                    setMessage("device already connected");
                    status = Status.error;
                    port.unlockPort(this);
                }
                if(device.autoConnect) {
                    setMessage("device is autoConnect");
                    status = Status.error;
                    port.unlockPort(this);
                }
            }
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#unlockPort(org.epics.pvioc.pdrv.User)
         */
        @Override
        public void unlockPort() {
            if(port==null) return;
            port.unlockPort(this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getAuxStatus()
         */
        @Override
        public int getAuxStatus() {
            return auxStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getBoolean()
         */
        @Override
        public boolean getBoolean() {
            return booleanvalue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setBoolean(boolean)
         */
        @Override
        public void setBoolean(boolean value) {
            booleanvalue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getDouble()
         */
        @Override
        public double getDouble() {
            return doubleValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getInt()
         */
        @Override
        public int getInt() {
            return intValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getMessage()
         */
        @Override
        public String getMessage() {
            return message;
        }
		/* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getReason()
         */
        @Override
        public int getReason() {
            return reason;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getString()
         */
        @Override
        public String getString() {
            return stringValue;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getTimeout()
         */
        @Override
        public double getTimeout() {
            return timeout;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getUserPvt()
         */
        @Override
        public Object getUserPvt() {
            return userPvt;
        }       
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getDeviceDriverPvt()
         */
        @Override
        public Object getDeviceDriverPvt() {
            return deviceDriverPvt;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#getPortDriverPvt()
         */
        @Override
        public Object getPortDriverPvt() {
            return portDriverPvt;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setAuxStatus(int)
         */
        @Override
        public void setAuxStatus(int auxStatus) {
            this.auxStatus = auxStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setDouble(double)
         */
        @Override
        public void setDouble(double value) {
            doubleValue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setInt(int)
         */
        @Override
        public void setInt(int value) {
            this.intValue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setMessage(java.lang.String)
         */
        @Override
        public void setMessage(String message) {
            this.message = message;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setReason(int)
         */
        @Override
        public void setReason(int reason) {
            this.reason = reason;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setString(java.lang.String)
         */
        @Override
        public void setString(String value) {
            this.stringValue = value;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setTimeout(double)
         */
        @Override
        public void setTimeout(double timeout) {
            this.timeout = timeout;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setUserPvt(java.lang.Object)
         */
        @Override
        public void setUserPvt(Object userPvt) {
            this.userPvt = userPvt;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setDeviceDriverPvt(java.lang.Object)
         */
        @Override
        public void setDeviceDriverPvt(Object deviceDriverPvt) {
            this.deviceDriverPvt = deviceDriverPvt;
        }        
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.User#setPortDriverPvt(java.lang.Object)
         */
        @Override
        public void setPortDriverPvt(Object portDriverPvt) {
            this.portDriverPvt = portDriverPvt;
        }
        
        private void clearErrorParms() {
        	auxStatus = 0;
        	message = null;
        }
    }
    
    private static class PortImpl implements Port,TraceOptionChangeListener, QueueRequestCallback,Timer.TimerCallback {
        
        private PortImpl(String portName,PortDriver portDriver,String driverName,
            boolean canBlock,boolean autoConnect,ThreadPriority priority)
        {
            this.portName = portName;
            this.portDriver = portDriver;
            this.driverName = driverName;
            this.autoConnect = autoConnect;
            if(canBlock) {
                portThread = new PortThread(this,priority);
            }
            trace.optionChangeListenerAdd(portImplUser, this);
            new InitialConnectTask(this);
        }
        
        private LinkedListNode<PortImpl> portListNode = portListCreate.createNode(this);
        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
        private Trace trace = TraceFactory.create();
        
        private String portName;
        private PortDriver portDriver;
        private String driverName;
        private boolean autoConnect; 
        private UserImpl portImplUser = (UserImpl)Factory.createUser(this);
        private PortThread portThread = null;        
        
        private boolean connected = false;
        private boolean enabled = true;
        
        private LinkedList<DeviceImpl> deviceList = deviceListCreate.create();
        private ConnectExceptionList exceptionList = new ConnectExceptionList();
        private boolean exceptionActive = false;
        
        private UserImpl lockPortUser = null;
        private ReentrantLock lockPortLock = new ReentrantLock();
        private Condition portUnlock = lockPortLock.newCondition();
        
        private LockPortNotify lockPortNotify = null;   

        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#report(boolean, int)
         */
        @Override
        public String report(boolean reportDevices,int details) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    "port %s driver %s isLocked %b%n",
                    portName,driverName,(lockPortUser==null ? false : true)
            ));
            builder.append(String.format(
                    "    autoConnect %b connected %b enabled %b%n",autoConnect,connected,enabled));
            String driverReport = portDriver.report(details);
            if(driverReport!=null) {
                builder.append(String.format("    %s%n",driverReport));
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
         * @see org.epics.pvioc.pdrv.Port#getDevices()
         */
        @Override
        public Device[] getDevices() {
            synchronized(deviceList) {
                Device[] devices = new Device[deviceList.getLength()];
                LinkedListNode<DeviceImpl> node = deviceList.getHead();
                int index = 0;
                while(node!=null) {
                    Device device = node.getObject();
                    devices[index++] = device;
                    node = deviceList.getNext(node);
                }
                return devices;
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#getDriverName()
         */
        @Override
        public String getDriverName() {
            return driverName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#getPortName()
         */
        @Override
        public String getPortName() {
            return portName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#getTrace()
         */
        @Override
        public Trace getTrace() {
            return trace;
        }
       
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#canBlock()
         */
        @Override
        public boolean canBlock() {
            return ((portThread==null) ? false : true);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#enable(boolean)
         */
        @Override
        public void enable(boolean trueFalse) {
            boolean changed = false;
            synchronized(this){
                if(enabled!=trueFalse) {
                    enabled = trueFalse;
                    changed = true;
                }
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.enable %b wasChanged %b", portName,trueFalse,changed);
            }
            if(changed) {
                raiseException(ConnectExceptionType.enable);
                if(trueFalse) scanQueues();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#autoConnect(boolean)
         */
        @Override
        public void autoConnect(boolean trueFalse) {
            boolean changed = false;
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.autoConnect %b", portName,trueFalse);
            }
            synchronized(this){
                if(autoConnect!=trueFalse) {
                    autoConnect = trueFalse;
                    changed = true;
                }
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.autoConnect %b was changed %b", portName,trueFalse,changed);
            }
            if(changed) {
                raiseException(ConnectExceptionType.autoConnect);
                if(trueFalse) {
                    startAutoConnect(0.0);
                } else {
                    timerNode.cancel();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#getDevice(org.epics.pvioc.pdrv.User, java.lang.String)
         */
        @Override
        public Device getDevice(User user, String deviceName) {
            synchronized(deviceList) {
                LinkedListNode<DeviceImpl> node = deviceList.getHead();
                while(node!=null) {
                    Device device = node.getObject();
                    if(device.getDeviceName().equals(deviceName)) return device;
                    node = deviceList.getNext(node);
                }
            }
            return portDriver.createDevice(user, deviceName);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#connect(org.epics.pvioc.pdrv.User)
         */
        @Override
        public Status connect(User user) { 
            if(lockPortUser!=user) {
                user.setMessage("Illegal to call connect without owning the port");
                return Status.error;
            }
            if(connected) {
                user.setMessage("port is already connected");
                return Status.error;
            }
            if(autoConnect) {
                user.setMessage("port is autoConnect");
                return Status.error;
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.connect", portName);
            }
            Status status = portDriver.connect(user);
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#disconnect(org.epics.pvioc.pdrv.User)
         */
        @Override
        public Status disconnect(User user) {
            if(lockPortUser!=user) {
                user.setMessage("Illegal to call disconnect without owning the port");
                return Status.error;
            }
            if(!connected) {
                user.setMessage("port is alreadt disconnected");
                return Status.error;
            }
            if(autoConnect) {
                user.setMessage("port is autoConnect");
                return Status.error;
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.disconnect", portName);
            }
            Status status = portDriver.disconnect(user);
            if(portThread!=null) portThread.scanQueues();
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#isConnected()
         */
        @Override
        public boolean isConnected() {
            return connected;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#isEnabled()
         */
        @Override
        public boolean isEnabled() {
            return enabled;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#isAutoConnect()
         */
        @Override
        public boolean isAutoConnect() {
            return autoConnect;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#exceptionListenerAdd(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.ConnectExceptionListener)
         */
        @Override
        public Status exceptionListenerAdd(User user,ConnectExceptionListener listener)
        {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.exceptionListenerAdd", portName);
            }
            synchronized(this){
                if(!exceptionActive) {
                    return exceptionList.add(user, listener);
                } else {
                    return exceptionList.addNew(user, listener);
                }          
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#exceptionListenerRemove(org.epics.pvioc.pdrv.User)
         */
        @Override
        public void exceptionListenerRemove(User user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.exceptionListenerRemove", portName);
            }
            synchronized(this){
                exceptionList.remove(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.PortUser#scanQueue()
         */
        @Override
        public void scanQueues() {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.scanQueues", portName);
            }
            if(portThread!=null) portThread.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#registerLockPortNotify(org.epics.pvioc.pdrv.LockPortNotify)
         */
        @Override
        public void registerLockPortNotify(LockPortNotify lockPortNotify) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.registerLockPortNotify", portName);
            }
            synchronized(this){
                if(this.lockPortNotify!=null) {
                    throw new IllegalStateException("lockPortNotify already registered");
                }
                this.lockPortNotify = lockPortNotify;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#unregisterLockPortNotify()
         */
        @Override
        public void unregisterLockPortNotify() {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.unregisterLockPortNotify", portName);
            }
            synchronized(this){
                if(this.lockPortNotify==null) {
                    throw new IllegalStateException("not registered");
                }
                lockPortNotify = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Port#createDevice(org.epics.pvioc.pdrv.DeviceDriver, java.lang.String)
         */
        @Override
        public Device createDevice(DeviceDriver deviceDriver, String deviceName) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.createDevice %s", portName,deviceName);
            }
            DeviceImpl device = new DeviceImpl(this,deviceDriver,deviceName);
            synchronized(deviceList) {
                LinkedListNode<DeviceImpl> node = deviceList.getHead();
                while(node!=null) {
                    if(node.getObject().deviceName.equals(deviceName)) {
                        throw new IllegalStateException("port already exists");
                    }
                    node = deviceList.getNext(node);
                }
                deviceList.addTail(device.deviceListNode);
            }
            if(connected) device.startAutoConnect(.01);
            return device;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.PortDriver#exceptionConnect()
         */
        @Override
        public void exceptionConnect() {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.exceptionConnect", portName);
            }
            synchronized(this){
                if(connected) {
                    throw new IllegalStateException("already connected");
                }
                connected = true;
            }
            synchronized(deviceList) {
                LinkedListNode<DeviceImpl> node = deviceList.getHead();
                while(node!=null) {
                    DeviceImpl device = node.getObject();
                    if(!device.connected)device.startAutoConnect(0.0);
                    node = deviceList.getNext(node);
                }
            }
            raiseException(ConnectExceptionType.connect);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.PortDriver#exceptionDisconnect()
         */
        @Override
        public void exceptionDisconnect() {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.exceptionDisconnect", portName);
            }
            synchronized(this){
                if(!connected) {
                    throw new IllegalStateException("already disconnected");
                }
                connected = false;
            }
            scanQueues();
            if(autoConnect) startAutoConnect(1.0);
            raiseException(ConnectExceptionType.connect);
        }
        
        
        private Status lockPort(UserImpl user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.lockPort", portName);
            }
            lockPortLock.lock();
            try {
                while(true) {
                    if(!enabled) {
                        user.message = "port disabled";
                        return Status.error;
                    }
                    if(lockPortUser==user) {
                        user.message = "already locked by user. Illegal request";
                        return Status.error;
                    }
                    if(lockPortUser==null)  {
                        lockPortUser = user;
                        break;
                    }
                    try {
                        portUnlock.await();
                    } catch (InterruptedException e){}
                }
                
            } finally {
                lockPortLock.unlock();
            }
            LockPortNotify notify = null;
            synchronized(this) {
                notify = lockPortNotify;
            }
            if(notify!=null) notify.lock(user);
            return Status.success;
        }
        
        private void unlockPort(UserImpl user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.unlockPort", portName);
            }
            lockPortLock.lock();
            try {
                if(lockPortUser!=user) {
                    trace.print(Trace.ERROR, "%s unlockPort but not the lockPortUser", portName);
                    return;
                }
                lockPortUser = null;;
                portUnlock.signal();
            } finally {
                lockPortLock.unlock();
            }
            LockPortNotify notify = null;
            synchronized(this) {
                notify = lockPortNotify;
            }
            // notice possible race condition, i.e. lockPortNotify.lock may again be called before this
            if(notify!=null) notify.unlock();
        }   
        
        private void raiseException(ConnectExceptionType connectException) {
            synchronized(this){
                exceptionActive = true;
            }
            exceptionList.raiseException(connectException);
            synchronized(this){
                exceptionList.merge();
                exceptionActive = false; 
            }
        } 
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.TraceOptionChangeListener#optionChange()
         */
        @Override
        public void optionChange() {
            int mask = trace.getMask();
            int iomask = trace.getIOMask();
            Writer writer = trace.getFile();
            int size = trace.getIOTruncateSize();
            synchronized(deviceList) {
                LinkedListNode<DeviceImpl> node = deviceList.getHead();
                while(node!=null) {
                    Device device = node.getObject();
                    Trace trace = device.getTrace();
                    trace.setFile(writer);
                    trace.setMask(mask);
                    trace.setIOMask(iomask);
                    trace.setIOTruncateSize(size);
                    node = deviceList.getNext(node);
                }
            }
        }
        private void startAutoConnect(double initialDelay) {
            if(portImplUser.getPort()==null && portImplUser.connectPort(portName)==null) {
                throw new IllegalStateException("Logic error");
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.startAutoConnect", portName);
            }
            if(connected || !autoConnect) return;
            timerNode.cancel();
            timer.schedulePeriodic(timerNode, initialDelay, autoConnectPeriod);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.QueueRequestCallback#callback(org.epics.pvioc.pdrv.Status, org.epics.pvioc.pdrv.User)
         */
        @Override
        public void callback(Status status, User user) {
            if(!connected) portDriver.connect(portImplUser);
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Port.callback connect result %b", portName,connected);
            }
            if(connected) {
                timerNode.cancel();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
         */
        @Override
        public void callback() {
            if(portThread!=null) {
                if(!portImplUser.isQueued) {
                    portThread.queueRequest(portImplUser,QueuePriority.medium);
                } 
            } else {
                Status status = lockPort(portImplUser);
                if(status==Status.success) {
                    if(!connected) portDriver.connect(portImplUser);  
                    unlockPort(portImplUser);
                }
                if(connected) {
                    timerNode.cancel();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
         */
        @Override
        public void timerStopped() {}
    }
    
    private static class DeviceImpl implements Device, QueueRequestCallback,Timer.TimerCallback {

        private DeviceImpl(PortImpl port,DeviceDriver deviceDriver,String deviceName) {
            this.port = port;
            this.deviceDriver = deviceDriver;
            this.deviceName = deviceName;
            fullName = port.getPortName() + "[" + deviceName + "]";
            autoConnect = port.autoConnect;
            deviceImplUser = (UserImpl)Factory.createUser(this);
        }
        
        private LinkedListNode<DeviceImpl> deviceListNode = deviceListCreate.createNode(this);
        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
        private PortImpl port;
        private DeviceDriver deviceDriver;
        private String deviceName;
        private String fullName;
        private Trace trace = TraceFactory.create();
        private LinkedList<Interface> interfaceList = interfaceListCreate.create();
        private ConnectExceptionList exceptionList = new ConnectExceptionList();
        private boolean exceptionActive = false;
        private User blockingUser = null;
        private boolean autoConnect = false;
        private UserImpl deviceImplUser = null;
        private boolean connected = false;
        private boolean enabled = true;
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceUser#report(int)
         */
        @Override
        public String report(int details) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    "    %s isBlocked %b autoConnect %b connected %b enabled %b%n",
                    fullName,isBlockedByOtherUser(null),autoConnect,connected,enabled
            ));
            String deviceReport = deviceDriver.report(details);
            if(deviceReport!=null) {
                builder.append(String.format("    %s%n",deviceReport));
            }
            if(details>0) {
                Interface[] interfaces = getInterfaces();
                int length = interfaces.length;
                if(length>0) {
                    builder.append("       Interfaces: ");
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
         * @see org.epics.pvioc.pdrv.Device#getInterfaces()
         */
        @Override
        public Interface[] getInterfaces() {
            synchronized(interfaceList) {
                Interface[] interfaces = new Interface[interfaceList.getLength()];
                LinkedListNode<Interface> node = interfaceList.getHead();
                int index = 0;
                while(node!=null) {
                    Interface iface = node.getObject();
                    interfaces[index++] = iface;
                    node = interfaceList.getNext(node);
                }
                return interfaces;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#getDeviceName()
         */
        @Override
        public String getDeviceName() {
            return deviceName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#getFullName()
         */
        @Override
        public String getFullName() {
            return fullName;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#getPort()
         */
        @Override
        public Port getPort() {
            return port;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#getTrace()
         */
        @Override
        public Trace getTrace() {
            return trace;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceUser#enable(boolean)
         */
        @Override
        public void enable(boolean trueFalse) {
            boolean changed = false;
            synchronized(this) {
                if(enabled!=trueFalse) {
                    enabled = trueFalse;
                    changed = true;
                }
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.enable %b was changed %b", fullName,trueFalse,changed);
            }
            if(changed) {
                raiseException(ConnectExceptionType.enable);
                if(trueFalse) port.scanQueues();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceUser#autoConnect(boolean)
         */
        @Override
        public void autoConnect(boolean trueFalse) {
            boolean changed = false;
            synchronized(this) {
                if(autoConnect!=trueFalse) {
                    autoConnect = trueFalse;
                    changed = true;
                }
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.autoConnect %b was changed %b", fullName,trueFalse,changed);
            }
            if(changed) {
                raiseException(ConnectExceptionType.autoConnect);
                if(trueFalse) startAutoConnect(0.0);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#connect(org.epics.pvioc.pdrv.User)
         */
        @Override
        public Status connect(User user) {
            PortImpl port = (PortImpl)user.getPort();
            if(port.lockPortUser!=user) {
                user.setMessage("Illegal to call connect without owning the port");
                return Status.error;
            }
            if(connected) {
                user.setMessage("already connected");
                return Status.error;
            }
            if(autoConnect) {
                user.setMessage("device is autoConnect");
                return Status.error;
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.connect", fullName);
            }
            return deviceDriver.connect(user);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#disconnect(org.epics.pvioc.pdrv.User)
         */
        @Override
        public Status disconnect(User user) {
            PortImpl port = (PortImpl)user.getPort();
            if(port.lockPortUser!=user) {
                user.setMessage("Illegal to call disconnect without owning the port");
                return Status.error;
            }
            if(!connected) {
                user.setMessage("not connected");
                return Status.error;
            }
            if(autoConnect) {
                user.setMessage("port is autoConnect");
                return Status.error;
            }
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.disconnect", fullName);
            }
            Status status =  deviceDriver.disconnect(user);
            if(port.portThread!=null) port.portThread.scanQueues();
            return status;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceUser#isAutoConnect()
         */
        @Override
        public boolean isAutoConnect() {
            return autoConnect;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceUser#isConnected()
         */
        @Override
        public boolean isConnected() {
            return connected;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceUser#isEnabled()
         */
        @Override
        public boolean isEnabled() {
            return enabled;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#exceptionListenerAdd(org.epics.pvioc.pdrv.User, org.epics.pvioc.pdrv.ConnectExceptionListener)
         */
        @Override
        public Status exceptionListenerAdd(User user,ConnectExceptionListener listener)
        {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.exceptionListenerAdd", fullName);
            }
            synchronized(this) {
                if(!exceptionActive) {
                    return exceptionList.add(user, listener);
                } else {
                    return exceptionList.addNew(user, listener);
                }          
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#exceptionListenerRemove(org.epics.pvioc.pdrv.User)
         */
        @Override
        public void exceptionListenerRemove(User user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.exceptionListenerRemove", fullName);
            }
            synchronized(this) {
                exceptionList.remove(user);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#findInterface(org.epics.pvioc.pdrv.User, java.lang.String)
         */
        @Override
        public Interface findInterface(User user, String interfaceName) {
            synchronized(interfaceList) {
                LinkedListNode<Interface> node = interfaceList.getHead();
                while(node!=null) {
                    Interface iface = node.getObject();
                    if(iface.getInterfaceName().equals(interfaceName)) return iface;
                    node = interfaceList.getNext(node);
                }
            }
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#blockOtherUsers(org.epics.pvioc.pdrv.User)
         */
        @Override
        public Status blockOtherUsers(User user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.blockOtherUsers", fullName);
            }
            if(blockingUser!=null) {
                user.setMessage("already blocked");
                return Status.error;
            }
            blockingUser = user;
            return Status.success;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#isBlockedByUser(org.epics.pvioc.pdrv.User)
         */
        @Override
        public boolean isBlockedByOtherUser(User user) {
            if(blockingUser==null) return false;
            return ((blockingUser==user) ? false : true);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.Device#unblockOtherUsers(org.epics.pvioc.pdrv.User)
         */
        @Override
        public void unblockOtherUsers(User user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.unblockOtherUsers", fullName);
            }
            if(user!=blockingUser) {
                throw new IllegalStateException("not the blocking asynUser");
            }
            blockingUser = null;
            PortThread portThread = ((PortImpl)port).portThread;
            if(portThread!=null) portThread.unblock();
            port.scanQueues();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceDriver#registerInterface(org.epics.pvioc.asyn.Interface)
         */
        @Override
        public void registerInterface(Interface newIface) {
            String name = newIface.getInterfaceName();
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.registerInterface %s", fullName,name);
            }
            synchronized(interfaceList) {
                LinkedListNode<Interface> newNode = interfaceListCreate.createNode(newIface);
                LinkedListNode<Interface> node = interfaceList.getHead();
                while(node!=null) {
                    Interface iface = node.getObject();
                    int compare = name.compareTo(iface.getInterfaceName());
                    if(compare==0) {
                        throw new IllegalStateException(
                                "interface " + name + " already registered");
                    }
                    if(compare<0) {
                        interfaceList.insertAfter(interfaceList.getPrev(node),newNode);
                        return;
                    }
                    node = interfaceList.getNext(node);
                }
                interfaceList.addTail(newNode);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceDriver#exceptionConnect()
         */
        @Override
        public void exceptionConnect() {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.exceptionConnect", fullName);
            }
            synchronized(this){
                if(connected) {
                    throw new IllegalStateException("already connected");
                }
                connected = true;
            }
            raiseException(ConnectExceptionType.connect);
            
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.asyn.DeviceDriver#exceptionDisconnect()
         */
        @Override
        public void exceptionDisconnect() {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.exceptionDisconnect", fullName);
            }
            synchronized(this){
                if(!connected) {
                    throw new IllegalStateException("not connected");
                }
                connected = false;
            }
            port.scanQueues();
            raiseException(ConnectExceptionType.connect);
        }
        
        private Status lockPort(UserImpl user) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.lockPort", fullName);
            }
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
        
        private void raiseException(ConnectExceptionType connectException) {
            synchronized(this){
                exceptionActive = true;
            }
            exceptionList.raiseException(connectException);
            synchronized(this){
                exceptionList.merge();
                exceptionActive = false; 
            }
        }
        
        private void startAutoConnect(double initialDelay) {
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Device.startAutoConnect", fullName);
            }
            if(deviceImplUser.getPort()==null && deviceImplUser.connectPort(port.portName)==null) {
                throw new IllegalStateException("Logic error");
            }
            if(deviceImplUser.getDevice()==null && deviceImplUser.connectDevice(deviceName)==null) {
                throw new IllegalStateException("Logic error");
            }
            if(connected || !autoConnect) return;
            timerNode.cancel();
            timer.schedulePeriodic(timerNode, initialDelay, autoConnectPeriod);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.pdrv.QueueRequestCallback#callback(org.epics.pvioc.pdrv.Status, org.epics.pvioc.pdrv.User)
         */
        @Override
        public void callback(Status status, User user) {
            if(!connected) deviceDriver.connect(deviceImplUser);
            if((trace.getMask()&Trace.FLOW)!=0) {
                trace.print(Trace.FLOW, "%s Deviice.callback connect %b", fullName,connected);
            }
            if(connected) {
                timerNode.cancel();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
         */
        @Override
        public void callback() {
            if(port.portThread!=null) {
                if(!deviceImplUser.isQueued) {
                    port.portThread.queueRequest(deviceImplUser,QueuePriority.medium);
                }
            }else {
                if(!connected) deviceDriver.connect(deviceImplUser);
                if(connected) {
                    timerNode.cancel();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
         */
        @Override
        public void timerStopped() {}
    }
    
    private static class ConnectExceptionList {
      
        private static LinkedListCreate<ConnectExceptionNode> connectExceptionListCreate = new LinkedListCreate<ConnectExceptionNode>();
        private static class ConnectExceptionNode {
            private User user;
            private ConnectExceptionListener listener;
            private LinkedListNode<ConnectExceptionNode> linkedListNode = null;
            private ConnectExceptionNode(User user,ConnectExceptionListener listener) {
                this.user = user;
                this.listener = listener;
                linkedListNode = connectExceptionListCreate.createNode(this);
            }
        }
        private LinkedList<ConnectExceptionNode> list = connectExceptionListCreate.create();
        private LinkedList<ConnectExceptionNode> listNew = connectExceptionListCreate.create();
        
        private Status add(User user, ConnectExceptionListener listener) {
            Status status = checkNotAlreadyListener(user,listener);
            if(status!=Status.success) return status;
            ConnectExceptionNode node = new ConnectExceptionNode(user,listener);
            list.addTail(node.linkedListNode);
            return Status.success;
        }
        private Status addNew(User user, ConnectExceptionListener listener) {
            Status status = checkNotAlreadyListener(user,listener);
            if(status!=Status.success) return status;
            ConnectExceptionNode node = new ConnectExceptionNode(user,listener);
            listNew.addTail(node.linkedListNode);
            return Status.success;
        }
        private Status checkNotAlreadyListener(User user, ConnectExceptionListener listener) {
            LinkedListNode<ConnectExceptionNode> linkedListNode = list.getHead();
            while(linkedListNode!=null) {
                ConnectExceptionNode node = linkedListNode.getObject();
                if(node.user==user) {
                    user.setMessage("already a ConnectExceptionListener");
                    return Status.error;
                }
                linkedListNode = list.getNext(linkedListNode);
            }
            linkedListNode = listNew.getHead();
            while(linkedListNode!=null) {
                ConnectExceptionNode node = linkedListNode.getObject();
                if(node.user==user) {
                    user.setMessage("already a ConnectExceptionListener");
                    return Status.error;
                }
                linkedListNode = listNew.getNext(linkedListNode);
            }
            return Status.success;
        }
        private void raiseException(ConnectExceptionType connectException) {
            LinkedListNode<ConnectExceptionNode> linkedListNode = list.getHead();
            while(linkedListNode!=null) {
                ConnectExceptionNode node = linkedListNode.getObject();
                node.listener.exception(connectException);
                linkedListNode = list.getNext(linkedListNode);
            }
        }
        private void merge() {
            LinkedListNode<ConnectExceptionNode> node = listNew.removeHead();
            while(node!=null) {
                list.addTail(node);
                node = listNew.removeHead();
            }
        }
        private void remove(User user) {
            LinkedListNode<ConnectExceptionNode> linkedListNode = list.getHead();
            while(linkedListNode!=null) {
                ConnectExceptionNode node = linkedListNode.getObject();
                if(node.user==user) {
                    list.remove(linkedListNode);
                    return;
                }
                linkedListNode = list.getNext(linkedListNode);
            }
            linkedListNode = listNew.getHead();
            while(linkedListNode!=null) {
                ConnectExceptionNode node = linkedListNode.getObject();
                if(node.user==user) {
                    listNew.remove(linkedListNode);
                    return;
                }
                linkedListNode = listNew.getNext(linkedListNode);
            }
        }
    }
    
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static final int queuePriorityHigh = QueuePriority.high.ordinal();
    private static final int numQueuePriorities = queuePriorityHigh + 1;
    private static final int queuePriorityLow = QueuePriority.low.ordinal();
    private static final int queuePriorityMedium = QueuePriority.medium.ordinal();
    
    private static class PortThread implements RunnableReady  {
        
        private PortThread(PortImpl port,ThreadPriority threadPriority) {
            this.port = port;
            for(int i=0; i<queueListArray.length; i++) {
                queueListArray[i] = userListCreate.create();
            }
            threadCreate.create(port.getPortName(), threadPriority.getJavaPriority(), this);
        }
        
        private ReentrantLock lock = new ReentrantLock();
        private Condition moreWork = lock.newCondition();
        private PortImpl port;
        private LinkedList<UserImpl>[] queueListArray = new LinkedList[numQueuePriorities];  
        private LinkedList<UserImpl> waitUnblockList =  userListCreate.create();
        
        private void queueRequest(UserImpl user,QueuePriority asynQueuePriority)
        {
            synchronized(this){
                if(user.isQueued) {
                    throw new IllegalStateException("prevous queueRequest not complete");
                }
                user.isQueued = true;
                LinkedList<UserImpl> list = queueListArray[asynQueuePriority.ordinal()];
                list.addTail(user.listNode);
            }
            scanQueues();
        }
        
        private void cancelRequest(UserImpl asynUserPvtCancel)
        {
            UserImpl user = null;
            synchronized(this){
                LinkedList<UserImpl> list = null;
                for(int i=queuePriorityHigh; i>=queuePriorityLow; i--) {
                    list = queueListArray[i];
                    LinkedListNode<UserImpl> listNode = list.getHead();
                    while(listNode!=null) {
                        user = listNode.getObject();
                        if(user==asynUserPvtCancel) {
                            list.remove(listNode);
                            user.isQueued = false;
                            return;
                        }
                        listNode = list.getNext(listNode);
                    }
                }
                LinkedListNode<UserImpl> listNode = waitUnblockList.getHead();
                while(listNode!=null) {
                    user = listNode.getObject();
                    if(user==asynUserPvtCancel) {
                        list.remove(listNode);
                        user.isQueued = false;
                        return;
                    }
                    listNode = waitUnblockList.getNext(listNode);
                }
            }
        }
        
        private void unblock() {
            synchronized(this){
                while(waitUnblockList.getLength()>0) {
                    LinkedListNode<UserImpl> listNode = waitUnblockList.removeHead();
                    queueListArray[queuePriorityMedium].addTail(listNode);
                }
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
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
         */
        @Override
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
                synchronized(this){
                    for(int queue=queuePriorityHigh; queue>=queuePriorityLow; queue--) {
                        LinkedList<UserImpl> list = queueListArray[queue];
                        LinkedListNode<UserImpl> listNode = list.removeHead();
                        if(listNode==null) continue;
                        user = listNode.getObject();
                        device = (DeviceImpl)user.getDevice();
                        if(device!=null) {
                            if(device.isBlockedByOtherUser(user)) {
                                waitUnblockList.addTail(user.listNode);
                                continue;
                            }
                        }
                        user.isQueued = false;
                        break;
                    }
                    if(user==null) return;
                }
                QueueRequestCallback queueRequestCallback = user.getQueueRequestCallback();
                Status status = null;
                if(device!=null) {
                    status = device.lockPort(user);
                } else {
                    status = port.lockPort(user);
                }
                if(status==Status.success) {
                    boolean checkConnect = true;
                    if((device!=null && user==device.deviceImplUser) || (user==port.portImplUser)) {
                        checkConnect = false;
                    }
                    if(checkConnect) {
                        if(!port.isConnected()) {
                            user.setMessage("port is not connected");
                            status = Status.error;
                        }
                        if(status==Status.success && device!=null && !device.isConnected()) {
                            user.setMessage("device is not connected");
                            status = Status.error;
                        }
                    }
                }
                Trace trace = port.getTrace();
                if((trace.getMask()&Trace.FLOW)!=0) {
                    trace.print(Trace.FLOW, "%s portThread calling queueRequestCallback status %s", port.getPortName(),status.toString());
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
    
    private static class InitialConnectTask implements NewAfterStartRequester,AfterStartRequester,Timer.TimerCallback  {
        private AfterStartNode afterStartNode = AfterStartFactory.allocNode(this);
        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
        private AfterStart afterStart = null;
        private int numTimes = 0;
        private InitialConnectTask(PortImpl port) {
            this.port = port;
            AfterStartFactory.newAfterStartRegister(this);
        }
        private PortImpl port;
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.NewAfterStartRequester#callback(org.epics.pvioc.install.AfterStart)
         */
        @Override
        public void callback(AfterStart afterStart) {
            this.afterStart = afterStart;
            afterStart.requestCallback(afterStartNode, false, ThreadPriority.high);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStartRequester#callback(org.epics.pvioc.install.AfterStartNode)
         */
        @Override
        public void callback(AfterStartNode node) {
            timer.schedulePeriodic(timerNode, 0.0,.1);
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
         */
        @Override
        public void callback() {
            if(port.connected || numTimes>50) {
                timerNode.cancel();
                afterStart.done(afterStartNode);
            }
            numTimes++;
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
         */
        @Override
        public void timerStopped() {}
    }
}