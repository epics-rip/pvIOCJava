/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;
import org.epics.ioc.dbd.*;
/**
 * Interface for an IOC record instance link field.
 * @author mrk
 *
 */
public interface DBLink extends DBData{
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
    /**
     * Called by BaseDBData when support is changed.
     * @param linkSupport The new link support.
     * @param dbd The DBD that defines the link support.
     * @return (null,message) if the request (is,is not) successful.
     */
    String newSupport(DBDLinkSupport linkSupport,DBD dbd);
    /**
     * Get the PVLink for this DBLink.
     * @return The PVLink.
     */
    PVLink getPVLink();
}
