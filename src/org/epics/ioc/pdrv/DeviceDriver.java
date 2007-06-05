/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv;


public interface DeviceDriver {
    String report(int details);
    /**
     * Attempt to connect.
     * @param user The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage describes why the request failed.
     */
    Status connect(User user);
    /**
     * Attempt to disconnect.
     * @param user The requestor.
     * @return Result. Status.success means that the attempt was successful.
     * If the attempt fails user.getMessage describes why the request failed.
     */
    Status disconnect(User user);
}
