/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv;


/**
 * This is implemented by a driver and is only called by org.epics.pvioc.pdrv.Factory.
 * @author mrk
 *
 */
public interface DeviceDriver {
    /**
     * Report device specific information.
     * @param details How much detail.
     * @return The report.
     */
    String report(int details);
    /**
     * Attempt to connect.
     * @param user The requester.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage describes why the request failed.
     */
    Status connect(User user);
    /**
     * Attempt to disconnect.
     * @param user The requester.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage describes why the request failed.
     */
    Status disconnect(User user);
}
