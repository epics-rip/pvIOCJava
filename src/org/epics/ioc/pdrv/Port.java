/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

import org.epics.ioc.pdrv.interfaces.Interface;

/**
 * Interface for a port.
 * If a method must be called with the port owned by the caller then it must be called
 * either from a queueRequest callback or via the following pattern:
 * <pre>
 *     port.lockport(user)
 *     try {
 *         // calls to port and/or device
 *     } finally {
 *         port.unlock(user)
 *     }
 * </pre>
 * @author mrk
 *
 */
public interface Port {
    /**
     * Generate a report for this port.
     * This should be called without owning the port.
     * It can cause unnecessary delays if it is called with the port locked.
     * @param reportDevices Also report all devices connected to the report.
     * @param details How much to report.
     * @return A String that is the report.
     */
    String report(boolean reportDevices,int details);
    /**
     * Get an array of the port devices.
     * This can be called without owning the port.
     * @return The array of devices.
     */
    Device[] getDevices();
    /**
     * Get an array of the port interfaces.
     * This can be called without owning the port.
     * @return The array of interfaces.
     */
    Interface[] getInterfaces();
    /**
     * Get the Trace object for this port.
     * This can be called without owning the port.
     * @return The interface.
     */
    Trace getTrace();
    /**
     * Get the driverName.
     * This can be called without owning the port.
     * @return The name.
     */
    String getDriverName();
    /**
     * Get the portName.
     * This can be called without owning the port.
     * @return The name.
     */
    String getPortName();
    /**
     * Does this port support multiple devices.
     * This can be called without owning the port.
     * @return (false,true) is it (does not, does) support multiple devices.
     */
    boolean isMultiDevicePort();
    /**
     * Can this port block while performing I/O.
     * This can be called without owning the port.
     * @return (false,true) if it is (synchronous,asynchronous)
     */
    boolean canBlock();
    /**
     * Set the enable state.
     * This can be called without owning the port.
     * @param trueFalse The new state.
     */
    void enable(boolean trueFalse);
    /**
     * Set the autoConnect state.
     * This can be called without owning the port.
     * @param trueFalse The new state.
     */
    void autoConnect(boolean trueFalse);
    /**
     * Get the device for the specified address.
     * This can be called without owning the port.
     * If a device at the specified address does not exist than the
     * portDriver is asked to create one.
     * @param user The user.
     * @param addr The address.
     * @return The Device interface or null if no device is available for the
     * specified  addrsss.
     */
    Device getDevice(User user, int addr);
    /**
     * Attempt to connect.
     * This must be called without owning the port.
     * @param User The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status connect(User user);
    /**
     * Attempt to disconnect.
     * This must be called without owning the port.
     * @param user The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status disconnect(User user);
    /**
     * Is the port connected to hardware.
     * This can be called without owning the port.
     * @return (false,true) is it (is not, is connected)
     */
    boolean isConnected();
    /**
     * Is the port enabled.
     * This can be called without owning the port.
     * If it is not enabled nothing will be taken from the queue and lockPort will fail.
     * @return (false,true) if the port (is not, is) enabled,
     */
    boolean isEnabled();
    /**
     * Is autoConnect active.
     * This can be called without owning the port.
     * If it is than when the queue is scanned or when lockPort is called and the port
     * is not connected, port.connect is called.
     * @return (false,true) if autoConnect (is not, is) active.
     */
    boolean isAutoConnect();
    /**
     * Add a listener for connect/disconnect events.
     * This can be called without owning the port.
     * @param connectExceptionListener The listener interface.
     */
    void exceptionListenerAdd(ConnectExceptionListener connectExceptionListener);
    /**
     * Remove a listener for connect/disconnect events.
     * This can be called without owning the port.
     * @param connectExceptionListener The listener interface.
     */
    void exceptionListenerRemove(ConnectExceptionListener connectExceptionListener);
    /**
     * Find an interface for the port.
     * This can be called without owning the port.
     * @param user The user.
     * @param interfaceName The name of the interface.
     * @param interposeInterfaceOK Can an interpose interface be returned.
     * If not then only an interface implemented by the portDriver will be returned.
     * @return The interface or null if an interface with this name does not exist.
     */
    Interface findInterface(User user,String interfaceName,boolean interposeInterfaceOK);
    /**
     * Queue a request for a port.
     * @param user The user.
     * @param queuePriority The priority.
     */
    void queueRequest(User user,QueuePriority queuePriority);
    /**
     * Cancel a queueRequest.
     * This must be called with the port unlocked.
     * @param user
     */
    void cancelRequest(User user);
    /**
     * lockPort with permission to perform IO.
     * The request will fail for any of the following reasons:
     * <ul>
     *    <li>The port is not enabled</li>
     *    <li>The port is blocked by another user</li>
     *    <li>The port is not connected.
     * </ul>
     * It will attempt to connect if autoConnect is true. 
     * @param user The user.
     * @return Status.sucess if the port is connected, enabled, and not blocked by another user.
     */
    Status lockPort(User user);
    /**
     * Unlock the port.
     * @param user The user that called lockPort.
     */
    void unlockPort(User user);
    /**
     * Scan the queues.
     * Can be called without owning the port.
     */
    void scanQueues();
    /**
     * Register to receive notice when lockPort, unLock port are called.
     * Caller must call lockPort before calling this method.
     * @param user The user.
     * @param lockPortNotify The notification interface.
     * @return TODO
     */
    boolean registerLockPortNotify(User user, LockPortNotify lockPortNotify);
    /**
     * Unregister to receive notice when lockPort, unLock port are called.
     * Caller must call lockPort before calling this method.
     * @param user The user.
     */
    void unregisterLockPortNotify(User user);
    /**
     * Called by driver to create a new device. Normally it is called as
     * a result of a call to PortDriver.createDevice, which is called with the port locked.
     * If it is called for some other reason than it must be called with the port locked.
     * @param deviceDriver The deviceDriver interface.
     * @param addr The address.
     * @return The deviceDriver interface or null if the driver can not create a device.
     */
    Device createDevice(DeviceDriver deviceDriver, int addr);
    /**
     * Register an interface for accessing the port.
     * Called by portDriver to register an interface.
     * This must be called with the port locked.
     * @param iface The interface.
     */
    void registerInterface(Interface iface);
    /**
     * Called by any code that wants to interpose an interface.
     * This must be called with the port locked.
     * The interpose interface can take special action and also call the lower level interface if it exists.
     * @param iface The new interface.
     * @return The previous interface.
     */
    Interface interposeInterface(Interface iface);
    /**
     * A connect exception. This is called by the portDriver.
     * It is normally called as a result of Port calling portDriver.connect.
     */
    void exceptionConnect();
    /**
     * A disconnect exception.This is called by the portDriver.
     * It is normally called as a result of Port calling portDriver.disconnect.
     */
    void exceptionDisconnect(); 
}
