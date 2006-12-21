/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * @author mrk
 *
 */
public interface PVLink extends PVData {
    /**
     * Get the configuration data for the support.
     * @return The configuration structure.
     */
    PVStructure getConfigurationStructure();
    /**
     * Set the configuration data.
     * @param pvStructure The configuration data.
     * The structure must be the type of structure associated with the support.
     * @return (false,true) if the configuration data (was not,was) modified.
     */
    boolean setConfigurationStructure(PVStructure pvStructure);
}
