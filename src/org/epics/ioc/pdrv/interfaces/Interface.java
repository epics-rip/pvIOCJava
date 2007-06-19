/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

/**
 * Base port and device interface.
 * @author mrk
 *
 */
public interface Interface {
    /**
     * Get the interface name.
     * @return The name.
     */
    String getInterfaceName();
}
