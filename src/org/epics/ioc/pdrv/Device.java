/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

import org.epics.ioc.pdrv.interfaces.Interface;

public interface Device {
    /**
     * Report about the device.
     * This should be called without owning the port even though some report
     * results may be bad. It can cause unnecessary delays if it is called with the port locked.
     * @param details How much detail.
     * @return A String containing the report.
     */
    String report(int details);
    /**
     * Get an array of the device interfaces.
     * This can be called without owning the port.
     * @return The array of interfaces.
     */
    Interface[] getInterfaces();
    /**
     * Get the device address.
     * @return The address.
     */
    int getAddr();
    /**
     * Get the port for this device.
     * @return The Port interface.
     */
    Port getPort();
    /**
     * Get the interface for tracing.
     * @return The Trace interface.
     */
    Trace getTrace();
    /**
     * Enable or disable a device. If a device is disabled than any active request will complete at an
     * unknown time in the future.
     * @param trueFalse (false,true) means to (disable,enable) the device.
     */
    void enable(boolean trueFalse);
    /**
     * Set the autoConnect state for the device.
     * @param trueFalse The autoConnect state.
     */
    void autoConnect(boolean trueFalse);
    /**
     * Attempt to connect.
     * This must be called without owning the port.
     * @param user The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage describes why the request failed.
     */
    Status connect(User user);
    /**
     * Attempt to disconnect.
     * This must be called without owning the port.
     * @param user The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage describes why the request failed.
     */
    Status disconnect(User user);
    /**
     * Is the device connected to it's I/O.
     * @return (false,true) if it (is not, is) connected.
     */
    boolean isConnected();
    /**
     * Is the device enabled.
     * If not enabled then requests for the device fail.
     * @return (false,true) if it (is not, is) enabled.
     */
    boolean isEnabled();
    /**
     * Will an automatic connect request be made when the first request for the device is issued.
     * @return (false,true) if an attempt to connect (will not, will) made when a queueRequest is active
     * and the device is not connected.
     */
    boolean isAutoConnect();
    /**
     * Add a listener for connect exceptions.
     * @param connectExceptionListener The listener.
     */
    void exceptionListenerAdd(ConnectExceptionListener connectExceptionListener);
    /**
     * Remove a listener for connect exceptions.
     * @param connectExceptionListener The listener.
     */
    void exceptionListenerRemove(ConnectExceptionListener connectExceptionListener);
    /**
     * Find an interface.
     * @param user The user.
     * @param interfaceName The name of the interface.
     * @param interposeInterfaceOK If an interpose interface is present should it be returned.
     * @return The Interface or null if the interface is not registered.
     */
    Interface findInterface(User user,String interfaceName,boolean interposeInterfaceOK);
    /**
     * lockPort with permission to perform I/O.
     * This calls port.lockPort and in addition checks the state of the device.
     * The request will fail for any of the following reasons:
     * <ul>
     *    <li>port.lockPort fails.
     *    <li>The device is not enabled</li>
     *    <li>The device is blocked by another user</li>
     *    <li>The device is not connected.
     * </ul>
     * It will attempt to connect to the device if autoConnect is true. 
     * @param user The user.
     * @return Status.sucess if the port and device are connected, enabled, and not blocked by another user.
     */
    Status lockPort(User user);
;    /**
     * Block all other users from accessing the device.
     * Must be called with the port owned by the caller.
     * @param user The user that is making the block request.
     * @return The status of the request.
     */
    Status blockOtherUsers(User user);
    /**
     * Allow other users to again use the device.
     * Must be called with the port owned by the caller.
     * @param user The user that called blockOtherUsers.
     */
    void unblockOtherUsers(User user);
    /**
     * Is the device blocked by someone other than the caller.
     * Must be called with the port owned by the caller.
     * @param user The user.
     * @return (false,true) if the device (is not, is) blocked.
     */
    boolean isBlockedByOtherUser(User user);
    /**
     * Register an interface for accessing the device.
     * Called by deviceDriver to register an interface.
     * This must be called with the port locked.
     * @param iface The Interface.
     */
    void registerInterface(Interface iface);
    /**
     * Interpose an interface.
     * This must be called with the port locked.
     * The interpose interface can take special action and also call the lower level interface if it exists.
     * @param iface The interpose Interface.
     * @return The lower level interface of null if none exists.
     */
    Interface interposeInterface(Interface iface);
    /**
     * A connect exception. This is called by the deviceDriver.
     * This must be called with no locks held and without owning the port.
     */
    void exceptionConnect();
    /**
     *  A disconnect exception.This is called by the deviceDriver.
     * This must be called with no locks held and without owning the port.
     */
    void exceptionDisconnect();
}
