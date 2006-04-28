/**
 * 
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
