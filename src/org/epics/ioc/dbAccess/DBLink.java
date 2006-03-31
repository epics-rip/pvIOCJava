/**
 * 
 */
package org.epics.ioc.dbAccess;
import org.epics.ioc.dbDefinition.*;

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
     * specvify the DBD that defines the structure and the name of the field that has the structure.
     * @param dbd the DBD that has the structure description
     * @param name the name of the field that has the configuration sytructure
     */
    void putConfigStructureFieldName(DBD dbd,String name);
    /**
     * get the name of the link support
     * @return the support name
     */
    String getLinkSupportName();
    /**
     * specify the link support name.
     * This will also determine the configuration support structure
     * @param name the support name
     */
    void putLinkSupportName(String name);
    /**
     * get the configuration structure.
     * IS THIS NEEDED?
     * @return the DBStructure
     */
    DBStructure getConfigStructure();
}
