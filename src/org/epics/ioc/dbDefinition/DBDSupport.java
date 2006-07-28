/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

/**
 * @author mrk
 *
 */
public interface DBDSupport {
    /**
     * Get the support name.
     * @return The name.
     */
    String getSupportName();
    /**
     * Get the name of the configuration structure.
     * @return The name.
     */
    String getConfigurationStructureName();
    /**
     * Get the name of the factory for creating a support instance.
     * @return The name.
     */
    String getFactoryName();
}
