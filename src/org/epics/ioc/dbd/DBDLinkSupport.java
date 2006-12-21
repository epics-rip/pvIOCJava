/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

/**
 * @author mrk
 *
 */
public interface DBDLinkSupport extends DBDSupport {
    /**
     * Get the name of the configuration structure.
     * @return The name.
     */
    String getConfigurationStructureName();
}
