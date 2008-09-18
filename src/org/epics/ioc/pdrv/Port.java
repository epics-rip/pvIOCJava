/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;


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
     * Get the device for the specified deviceName.
     * This can be called without owning the port.
     * If a device deviceName does not exist than the
     * portDriver is asked to create one.
     * @param user The user.
     * @param deviceName The name of the device.
     * @return The Device interface or null if no device is available for the
     * specified deviceName.
     */
    Device getDevice(User user, String deviceName);
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
     * @param user The user.
     * @param connectExceptionListener The listener interface.
     * @return Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status exceptionListenerAdd(User user,ConnectExceptionListener connectExceptionListener);
    /**
     * Remove a listener for connect/disconnect events.
     * This can be called without owning the port.
     * @param user The user.
     */
    void exceptionListenerRemove(User user);
    /**
     * Scan the queues.
     * Can be called with or without owning the port.
     */
    void scanQueues();
    /**
     * Attempt to connect.
     * This must be called with the port owned by the user.
     * @param user The user.
     * @return Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status connect(User user);
    /**
     * Attempt to disconnect.
     * This must be called with the port owned by the user.
     * @param user The requester.
     * @return Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status disconnect(User user);
    /**
     * Register to receive notice when lockPort, unLock port are called.
     * This should only be called by a driver.
     * @param lockPortNotify The notification interface.
     */
    void registerLockPortNotify(LockPortNotify lockPortNotify);
    /**
     * Unregister to receive notice when lockPort, unLock port are called.
     * Caller must call lockPort before calling this method.
     */
    void unregisterLockPortNotify();
    /**
     * Called by driver to create a new device. Normally it is called as
     * a result of a call to PortDriver.createDevice, which is called with the port locked.
     * If it is called for some other reason than it must be called with the port locked.
     * @param deviceDriver The deviceDriver interface.
     * @param deviceName The deviceName.
     * @return The deviceDriver interface or null if the driver can not create a device.
     */
    Device createDevice(DeviceDriver deviceDriver, String deviceName);
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
