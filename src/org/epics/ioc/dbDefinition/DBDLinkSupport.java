/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

/**
 * interface for link support.
 * @author mrk
 *
 */
public interface DBDLinkSupport {
    /**
     * get the name of the link support.
     * @return the name.
     */
    String getLinkSupportName();
    /**
     * get the name of the link support configuration structure.
     * @return the name.
     */
    String getConfigStructureName();
}
