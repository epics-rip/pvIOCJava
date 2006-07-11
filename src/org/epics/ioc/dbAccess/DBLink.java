/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbProcess.LinkSupport;

/**
 * Interface for accessing the link field of a record instance.
 * @author mrk
 *
 */
public interface DBLink extends DBStructure {
    /**
     * Get the name of the configuration structure.
     * @return The field name.
     */
    String getConfigStructureName();
    /**
     * Specify the name of configuration structure.
     * @param name The name of the configuration sytructure.
     */
    void putConfigStructureName(String name);
    /**
     * Get the name of the link support.
     * @return The support name.
     */
    String getLinkSupportName();
    /**
     * Specify the link support name.
     * @param name The support name.
     */
    void putLinkSupportName(String name);
    /**
     * Get the structure that has the configration information,
     * @return The DBStructure.
     */
    DBStructure getConfigStructure();
    /**
     * Specify the configuration structure.
     * @param dbStructure The structure for the configuration information.
     */
    void putConfigStructure(DBStructure dbStructure);
    /**
     * Get the link support for this link instance.
     * @return The LinkSupport or null if no support has been set.
     */
    LinkSupport getLinkSupport();
    /**
     * Set the link support.
     * @param support The support.
     * @return true if the support was set and false if the support already was set.
     */
    boolean setLinkSupport(LinkSupport support);
}
