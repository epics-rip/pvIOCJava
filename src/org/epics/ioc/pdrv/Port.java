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
     * This should be called without owning the port even though some report
     * results may be bad. It can cause unnecessary delays if it is called with the port locked.
     * @param reportDevices Also report all devices connected to the report.
     * @param details How much to report.
     * @return A String that is the report.
     */
    String report(boolean reportDevices,int details);
    /**
     * Get the Trace object for this port.
     * @return The interface.
     */
    Trace getTrace();
    /**
     * Get the driverName.
     * @return The name.
     */
    String getDriverName();
    /**
     * Get the portName.
     * @return The name.
     */
    String getPortName(); 
    /**
     * Block other users.
     * Must be called with the port owned by the caller.
     * No queueRequestCallback.callback for any other user will be called until
     * until unblockOtherUsers is called.
     * @param user The user making the request.
     * @return The status. The request will fail if another user has the port blocked.
     */
    Status blockOtherUsers(User user);
    /**
     * Unblock other users.
     * Must be called with the port owned by the caller.
     * @param user The user that called blockOtherUsers.
     */
    void unblockOtherUsers(User user);
    /**
     * Is the port blocked by someone other than the caller.
     * Must be called with the port owned by the caller.
     * @param user The user.
     * @return (false,true) if the port (is not, is) blocked.
     */
    boolean isBlockedByUser(User user);
    /**
     * Does this port support multiple devices.
     * @return (false,true) is it (does not, does) support multiple devices.
     */
    boolean isMultiDevicePort();
    /**
     * Can this port block while performing I/O.
     * @return (false,true) if it is (synchronous,asynchronous)
     */
    boolean canBlock();
    /**
     * Is the port connected to hardware.
     * @return (false,true) is it (is not, is connected)
     */
    boolean isConnected();
    /**
     * Is the port enabled.
     * If it is not enabled nothing will be taken from the queue and lockPort will fail.
     * @return (false,true) if the port (is not, is) enabled,
     */
    boolean isEnabled();
    /**
     * Is autoConnect active.
     * If it is than when the queue is scanned or when lockPort is called and the port
     * is not connected, port.connect is called.
     * @return (false,true) if autoConnect (is not, is) active.
     */
    boolean isAutoConnect();
    /**
     * Add a listener for connect/disconnect events.
     * @param connectExceptionListener The listener interface.
     */
    void exceptionListenerAdd(ConnectExceptionListener connectExceptionListener);
    /**
     * Remove a listener for connect/disconnect events.
     * @param connectExceptionListener The listener interface.
     */
    void exceptionListenerRemove(ConnectExceptionListener connectExceptionListener);
    /**
     * Find an interface for the port.
     * @param user The user.
     * @param interfaceName The name of the interface.
     * @param interposeInterfaceOK Can an interpose interface be returned.
     * If not then only an intrerface implemented by the portDriver will be returned.
     * @return The interface or null if an interface with this name does not exist.
     */
    Interface findInterface(User user,String interfaceName,boolean interposeInterfaceOK);
    /**
     * Queue a request for the port.
     * @param user The user.
     * @return status. If the request failed user.getMessage() provides the reason.
     */
    Status queuePortRequest(User user);
    /**
     * Queue a request for a device connected to the port.
     * @param user The user.
     * @param queuePriority The priority.
     * @return status. If the request failed user.getMessage() provides the reason.
     */
    Status queueDeviceRequest(User user,QueuePriority queuePriority);
    /**
     * Scan the queues.
     */
    void scanQueues();
    /**
     * Cancel a queueRequest.
     * @param user
     */
    void cancelRequest(User user);
    /**
     * Lock the port.
     * @param user The user.
     */
    void lockPort(User user);
    /**
     * lockPort with permission to perform IO.
     * It will attempt to connect of autoConnect is true. 
     * @param user The user.
     * @return Status.sucess if the port is connected, enabled, and not blocked by another user.
     */
    Status lockPortForIO(User user);
    /**
     * Unlock the port.
     * @param user The user that called lockPort.
     */
    void unlockPort(User user);
    /**
     * Register to receive notice when lockPort, unLock port are called.
     * @param lockPortNotify The notification interface.
     */
    void registerLockPortNotify(LockPortNotify lockPortNotify);
    /**
     * Unregister to receive notice when lockPort, unLock port are called.
     */
    void unregisterLockPortNotify();
    /**
     * Set the enable state.
     * @param trueFalse The new state.
     */
    void enable(boolean trueFalse);
    /**
     * Set the autoConnect state.
     * @param trueFalse The new state.
     */
    void autoConnect(boolean trueFalse);
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
     * This must be called with no locks held and without owning the port.
     */
    void exceptionConnect();
    /**
     * A disconnect exception.This is called by the portDriver.
     * This must be called with no locks held and without owning the port.
     */
    void exceptionDisconnect(); 
}
