/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.vxi11;
import org.epics.pvioc.pdrv.Status;
/**
 * Methods for communication with a VXI11 Controller.
 * 
 * @author mrk
 *
 */
public interface VXI11Controller {
    /**
     * Connect to the controller;
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status connect(VXI11User user);
    /**
     * Is the controller connected?
     * @return (false,true) if the controller (is not,is) connected.
     */
    boolean isConnected();
    /**
     * Disconnect from the controller.
     */
    void disconnect();
    /**
     * Create interface to a VXI11 device.
     * @param pad Primary Address. If -1 then connect to controller itself.
     * @param sad Secondary Address. If -1 no secondary address.
     * @return The VXI11Device interface.
     */
    VXI11Device createDevice(int pad,int sad);
    /**
     * Return the time interval that I/O operations will wait to
     * obtain the VXI11 controller lock.
     * @return Time interval, in milliseconds, that I/O operations
     * will wait to obtain the lock.
     */
    int getLockTimeout();
    /**
     * Set the time interval that I/O operations will wait to
     * obtain the VXI11 controller lock.
     * The default lock timeout interval is 20&nbsp;seconds.
     * @param msec The time interval, in milliseconds, that subsequent
     * I/O operations will wait to obtain the lock.
     */
    void setLockTimeout(int msec);
    /**
     * Return the time interval in which I/O operations must complete.
     * @return Time interval, in milliseconds, in which I/O operations
     * must complete.
     */
    int getIoTimeout();
    /**
     * Set the time interval in which I/O operations must complete.
     * I/O operations which have not completed in this interval will
     * throw an exception.
     * The default I/O interval is 20 seconds.
     * @param msec The time interval, in milliseconds, in which
     * subsequent I/O operations must complete.
     */
    void setIoTimeout(int msec);
    /**
     * Will subsequent I/O
     * and lock operations will wait for a locked device to be
     * unlocked?
     * @return (false,true) if (will not, will) wait.
     */
    boolean getWaitLock();
    /**
     * Set the flag which controls whether or not subsequent I/O
     * and lock operations will wait for a locked device to be
     * unlocked.
     * If the flag is false, I/O and lock operations
     * on a locked device will immediately raise an VXI11Exception.
     * If the wait lock flag is true, I/O and lock operations
     * on a locked device will wait up to lockTimeout
     * milliseconds before raising the exception.
     * The default value of the wait lock flag is false.
     * @param state The value to which the wait lock flag will be set.
     */

    void setWaitLock(boolean state);
    /**
     * Clear the interfaces of devices.
     * The controller pulls the bus IFC (Interface Clear) line
     * low momentarily to clear the interfaces of all devices attached
     * to the bus.
     * @param user The user.
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */

    Status ifc(VXI11User user);
    /**
     * Return the VXI11 bus address of the controller.
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status getBusAddress(VXI11User user);
    /**
     * Set the VXI11 bus address of the controller.
     * @param user The user.
     * @param pad The value to which the controller's VXI11 talker/listener address will be set.
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status setBusAddress(VXI11User user,int pad);
    /**
     * Get the status of the VXI11 bus REN (Remote Enable) line.
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status isRemote(VXI11User user);
    /**
     * Get the status of the VXI11 bus SRQ (Service Request) line.
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status isSRQ(VXI11User user);
    
    /**
     * Is NDAC asserted?
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status isNDAC(VXI11User user);
    /**
     * Is the controller the system controller?
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */

    Status isSystemController(VXI11User user);
    /**
     * Is the controller the controller in charge?
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status isControllerInCharge(VXI11User user);
    /**
     * Is the controller addressed to talk?
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status isTalker(VXI11User user);
    /**
     * Is the controller addressed to listen?
     * @param user The user.
     * @return The status. If status is success then user.getBoolean() has the result.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status isListener(VXI11User user);
    /**
     * Set the state of the ATN line.
     * @param user The user.
     * @param state (false,true) => (assert,do not assert).
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status setATN(VXI11User user,boolean state);
    /**
     * Set the status of the VXI11 bus REN (Remote Enable) line.
     * @param user The user.
     * @param state (false,true) => (assert,do not assert).
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status setREN(VXI11User user,boolean state);
    /**
     * Pass Control to another controller.
     * @param user The user.
     * @param addr The address of the other controller.
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status passControl(VXI11User user,int addr);
    /**
     * Send arbitrary commands to the VXI11 bus.
     * The controller asserts the VXI11 bus Attention (ATN) line
     * and transmits the bytes to the bus.
     * @param user The user.
     * @param data The commands to send.
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status sendCommand(VXI11User user,byte data[]);
    /**
     * Enable or disable the sending of Service Request (SRQ) messages
     * from the VXI11 controller.
     * When Service Request messages are enabled the VXI11 controller
     * will send a message to the controller object each time the VXI11 service
     * request bus line changes from the inactive to the active state.
     * The VXI11 controller object will then invoke the
     * ieee488SrqHandler method of the object registered to handle such
     * messages.  The
     * argument passed to the ieee488SrqHandler method is the controller
     * object invoking the handler.
     * @param user The user.
     * @param state Enable (true) or disable (false) the sending of
     * SRQ messages.
     * @return The status.
     * If status is not success user.getError() and user.getString() report cause of failure.
     */
    Status enableSRQ (VXI11User user,boolean state);
    /**
     * Register the object which will handle SRQ callbacks.
     * When the controller object receives a Service Request (SRQ) message
     * from the VXI11 controller it will invoke the
     * ieee488SrqHandler method of the registered handler object.  The
     * argument passed to the ieee488SrqHandler method is the controller
     * object invoking the handler.
     * @param srqHandler The object to which SRQ messages will be sent.
     */
    void registerSrqHandler(VXI11SrqHandler srqHandler);
}
