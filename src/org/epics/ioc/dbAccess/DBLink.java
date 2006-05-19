/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * Interface for accessing the link field of a record instance.
 * @author mrk
 *
 */
public interface DBLink extends DBStructure {
    /**
     * get the name of the configuration structure.
     * @return the field name.
     */
    String getConfigStructureName();
    /**
     * specify the name of configuration structure.
     * @param name the name of the configuration sytructure.
     */
    void putConfigStructureName(String name);
    /**
     * get the name of the link support.
     * @return the support name.
     */
    String getLinkSupportName();
    /**
     * specify the link support name.
     * @param name the support name.
     */
    void putLinkSupportName(String name);
    /**
     * get the structure that has the configration information,
     * @return the DBStructure.
     */
    DBStructure getConfigStructure();
    /**
     * specify the configuration structure.
     * @param dbStructure the structure for the configuration information.
     */
    void putConfigStructure(DBStructure dbStructure);
}
