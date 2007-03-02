/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;
/**
 * Interface for an IOC record instance link field.
 * @author mrk
 *
 */
public interface DBLink extends DBField{
    /**
     * Get the PVLink for this DBLink.
     * @return The PVLink.
     */
    PVLink getPVLink();
    /**
     * Replace the current PVLink.
     */
    void replacePVLink();
    /**
     * Get the configuration data for the support.
     * @return The configuration structure.
     */
    PVStructure getConfigurationStructure();
    /**
     * Set the configuration data.
     * @param configurationStructure The configuration data.
     * The structure must be the type of structure associated with the support.
     * @return <i>null</i> if the configurationStructure was changed or the reason why the request failed.
     */
    String setConfigurationStructure(PVStructure configurationStructure);
}
