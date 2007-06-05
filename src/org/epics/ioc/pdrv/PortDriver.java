/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;

/**
 * @author mrk
 *
 */
public interface PortDriver {
    /**
     * Report port specific information.
     * @param details How much detail.
     * @return
     */
    String report(int details);
    /**
     * Create a new device for this port.
     * @param user The user connecting to a device.
     * @param addr The device address.
     * @return The device interface or null if it can not be created.
     * If null is returned user.getrMessage() provides the reason.
     */
    Device createDevice(User user, int addr);
    /**
     * Attempt to connect.
     * @param User The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status connect(User user);
    /**
     * Attempt to disconnect.
     * @param user The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage() describes why the request failed.
     */
    Status disconnect(User user);
}
