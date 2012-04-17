package org.epics.pvioc.pdrv.interfaces;

import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.User;

public interface GpibController {
    /**
     * Clear the interfaces of devices.
     * The controller pulls the bus IFC (Interface Clear) line
     * low momentarily to clear the interfaces of all devices attached
     * to the bus.
     * @param user The user.
     * @return The status.
     * If status is not success user.setAlarm() is called.
     */
    Status ifc(User user);
    /**
     * Return the GPIB bus address of the controller.
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status getBusAddress(User user);
    /**
     * Set the GPIB bus address of the controller.
     * @param user The user.
     * @param address The value to which the controller's VXI11 talker/listener address will be set.
     * @return The status.
     * If status is not success user.setAlarm() is called.
     */
    Status setBusAddress(User user,int address);
    /**
     * Get the status of the REN (Remote Enable) line.
     * @return The status.
     * If status is not success user.setAlarm() is called.
     */
    Status isRemote(User user);
    /**
     * Get the status of the VXI11 bus SRQ (Service Request) line.
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status isSRQ(User user);
    /**
     * Is NDAC asserted?
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status isNDAC(User user);
    /**
     * Is the controller the system controller?
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */

    Status isSystemController(User user);
    /**
     * Is the controller the controller in charge?
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status isControllerInCharge(User user);
    /**
     * Is the controller addressed to talk?
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status isTalker(User user);
    /**
     * Is the controller addressed to listen?
     * @param user The user.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status isListener(User user);
    /**
     * Set the state of the ATN line.
     * @param user The user.
     * @param state (false,true) => (assert,do not assert).
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status setATN(User user,boolean state);
    /**
     * Set the status of the REN (Remote Enable) line.
     * @param user The user.
     * @param state (false,true) => (assert,do not assert).
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status setREN(User user,boolean state);
    /**
     * Pass Control to another controller.
     * @param user The user.
     * @param addr The address of the other controller.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status passControl(User user,int addr);
    /**
     * Send arbitrary commands to the VXI11 bus.
     * The controller asserts the VXI11 bus Attention (ATN) line
     * and transmits the bytes to the bus.
     * @param data The command bytes to be sent to the VXI11 bus.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status sendCommand(User user,byte data[]);
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
     * @param state Enable (true) or disable (false) the sending of
     * SRQ messages.
     * @return The status. If status is success then user.getInt() has the result.
     * If status is not success user.setAlarm() is called.
     */
    Status enableSRQ (User user,boolean state);
    /**
     * Register the object which will handle SRQ callbacks.
     * When the controller object receives a Service Request (SRQ) message
     * from the VXI11 controller it will invoke the
     * ieee488SrqHandler method of the registered handler object.  The
     * argument passed to the ieee488SrqHandler method is the controller
     * object invoking the handler.
     * @param srqHandler The object to which SRQ messages will be sent.
     */
    void registerSrqHandler(GpibSrqHandler srqHandler);
}
