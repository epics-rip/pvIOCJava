/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;




/**
 * An interface for making PDRV (Port Driver) requests and for communication between driver
 * and user.
 * The PDRV rules ensure that at any given time at most one user is the "owner" of a port.
 * Only the owner is allow to call the I/O interfaces implemented by the port/device driver.
 * While owner a user can make an arbitrary number of calls to the port/device interfaces.
 * Each PDRV user must create a User instance by calling Factory.createUser.
 * Locking rules:
 * <ul>
 *    <li> A user owns the port because of 1) it called
 *    port.lockPort or 2) The users queueRequestCallback is active.</li>
 *    <li>connectPort, connectDevice, disconnectDevice, and disconnectPort
 *    can all be called without owning the port.</li>
 *    <li>duplicateUser can safely be called if no connect or disconnect can occur
 *    while the duplicateUser is active.</li>
 *    <li>All other methods should only be called either while the user owns the port
 *    or by the user when no queueRequest is active.</li>
 *    <li>A user can call port and device interface methods only
 *    while it owns the port. Also some port and device methods may require that the caller
 *    own the port.</li>
 * </ul>
 * port.lockPort should be called via the following pattern:
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
public interface User {
    /**
     * A common value for user.reason
     */
    public static final int REASON_SIGNAL = -1;
    
    /**
     * Create a new User that is connected to the same port and device.
     * The new user will also have the same reason and timeout as the original.
     * @param queueRequestCallback The callback for queueRequest.
     * @return The new User.
     */
    User duplicateUser(QueueRequestCallback queueRequestCallback);
    /**
     * Connect the user to a port.
     * @param portName The portName.
     * @return An interface to the port or null if the port does not exist.
     */
    Port connectPort(String portName);
    /**
     * Disconnect this user from the port.
     * If a queueRequest is outstanding it is canceled.
     */
    void disconnectPort();
    /**
     * Get the port to which this user is connected.
     * @return The port or null if the user is not connected to a port.
     */
    Port getPort();
    /**
     * Connect the user to a device.
     * @param deviceName The device deviceName.
     * @return An interface to the device or null if the device does not exist.
     */
    Device connectDevice(String deviceName);
    /**
     * Disconnect this user from the device.
     */
    void disconnectDevice();
    /**
     * Get the device to which this user is connected.
     * @return The device or null if the user is not connected to a device.
     */
    Device getDevice(); 
    /**
     * Queue a request for a port.
     * @param queuePriority The priority.
     */
    void queueRequest(QueuePriority queuePriority);
    /**
     * Cancel a queueRequest.
     * This must be called with the port unlocked.
     */
    void cancelRequest();
    /**
     * lockPort with permission to perform IO.
     * The request will fail for any of the following reasons:
     * <ul>
     *    <li>The port and/or device is not enabled</li>
     *    <li>The device is blocked by another user</li>
     *    <li>The port and/or device is not connected.</li>
     * </ul>
     * It will attempt to connect if autoConnect is true. 
     * @return Status.success if the port is connected, enabled, and not blocked by another user.
     */
    Status lockPort();
    /**
     * lockPort with permission to call port.connect.
     * @return success if user is connected to a port and it is not connected.
     */
    Status lockPortForConnect();
    /**
     * lockPort with permission to call device.connect.
     * @return success if user is connected to a device and it is not connected.
     */
    Status lockDeviceForConnect();
    /**
     * Unlock the port.
     */
    void unlockPort();
    /**
     * Called to report errors to a user.
     * @param message The message.
     */
    void setMessage(String message);
    /**
     * Get the latest message.
     * @return The latest message.
     */
    String getMessage();
    /**
     * Set the timeout for individual IO requests.
     * @param timeout The timeout in seconds.
     */
    void setTimeout(double timeout);
    /**
     * Get the timeout.
     * @return The timeout in seconds.
     */
    double getTimeout();
    /**
     * Set a reference to a portDriverPrivate object.
     * A driver must never keep information about a user within the driver.
     * Instead it should use setPortDriverPvt and getPortDriverPvt.
     * @param portDriverPvt The object.
     */
    void setPortDriverPvt(Object portDriverPvt);
    /**
     * Get the reference to the portDriverPvt object.
     * @return Return a reference to portDriverPvt object.
     */
    Object getPortDriverPvt();
    /**
     * Set a reference to a deviceDriverPrivate object.
     * A driver must never keep information about a user within the driver.
     * Instead it should use setDeviceDriverPvt and getDeviceDriverPvt.
     * @param deviceDriverPvt The deviceDriverPvt.
     */
    void setDeviceDriverPvt(Object deviceDriverPvt);
    /**
     * Get the reference to the deviceDriverUserPvt object.
     * @return Return a reference to deviceDriverUserPvt.
     */
    Object getDeviceDriverPvt();
    /**
     * Set a reference to a user private object.
     * @param userPvt The userPvt object.
     */
    void setUserPvt(Object userPvt);
    /**
     * Get the reference to the user private object.
     * @return Return a reference to a user private object.
     */
    Object getUserPvt();
    /**
     * Reason is for optional use by user/driver communication.
     * Both user and driver must understand how it is used in order for
     * it to be useful.
     * @param reason The reason.
     */
    void setReason(int reason);
    /**
     * Get the reason.
     * @return The reason.
     */
    int getReason();
    /**
     * An auxiliary status that can be set by drivers to communicate information to users.
     * @param auxStatus The additional status.
     */
    void setAuxStatus(int auxStatus);
    /**
     * Get the auxiliary status.
     * @return The status.
     */
    int getAuxStatus();
    /**
     * A boolean value set by a driver to pass additional information to a user.
     * @param value The value.
     */
    void setBoolean(boolean value);
    /**
     * Get the boolean value.
     * @return The value.
     */
    boolean getBoolean();
    /**
     * An int value set by a driver to pass additional information to a user.
     * @param value The int value.
     */
    void setInt(int value);
    /**
     * Get the int value.
     * @return The int value.
     */
    int getInt();
    /**
     * An double value set by a driver to pass additional information to a user.
     * @param value The double value.
     */
    void setDouble(double value);
    /**
     * Get the double value.
     * @return The double value.
     */
    double getDouble();
    /**
     * An string value set by a driver to pass additional information to a user.
     * @param value The string value.
     */
    void setString(String value);
    /**
     * Get the string value.
     * @return The string value.
     */
    String getString();
}
