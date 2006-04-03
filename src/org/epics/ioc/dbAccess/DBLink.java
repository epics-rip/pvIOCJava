/**
 * 
 */
package org.epics.ioc.dbAccess;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Interface for accessing the link field of a record instance.
 * @author mrk
 *
 */
public interface DBLink extends DBStructure {
    /**
     * get the name of the field that has the configuration structure
     * @return the field name
     */
    String getConfigStructureFieldName();
    /**
     * specify the name of the field that has the configuration information.
     * @param name the name of the field that has the configuration sytructure
     */
    void putConfigStructureFieldName(String name);
    /**
     * get the name of the link support
     * @return the support name
     */
    String getLinkSupportName();
    /**
     * specify the link support name.
     * @param name the support name
     */
    void putLinkSupportName(String name);
}
