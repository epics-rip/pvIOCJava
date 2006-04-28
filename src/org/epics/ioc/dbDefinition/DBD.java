/**
 * 
 */
package org.epics.ioc.dbDefinition;

import java.util.*;

/**
 * DBD (DatabaseDefinition) is the interface for locating interfaces for
 * menu, structure, record, and linkSupport definitions.
 * @author mrk
 *
 */
public interface DBD {
    /**
     * get the name of this DBD.
     * Multiple DBDs can be created via DBDCreateFactory.
     * @return the DBD name.
     */
    String getName();
    /**
     * get the DBDMenu for the specified name.
     * @param menuName the menu to retrieve.
     * @return the DBDMenu or null if the menu does not exist.
     */
    DBDMenu getMenu(String menuName);
    /**
     * @param menu the DBDMenu to add.
     * @return (true,false) if the menu (was not, was) added.
     * If the menu is already present it is not added.
     */
    boolean addMenu(DBDMenu menu);
    /**
     * get a Collection of all menus.
     * @return the Collection.
     */
    Collection<DBDMenu> getMenuList();
    /**
     * get the DBDStructure for the specified name.
     * @param structureName the structure to retrieve.
     * @return the DBDStructure or null if the structure does not exist.
     */
    DBDStructure getDBDStructure(String structureName);
    /**
     * @param structure the DBDStructure to add.
     * @return  (true,false) if the structure (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addStructure(DBDStructure structure);
    /**
     * get a Collection of all the structures.
     * @return the Collection.
     */
    Collection<DBDStructure> getDBDStructureList();
    /**
     * get a DBDStructure that describes the recordType.
     * @param recordTypeName get the description of a recordType.
     * @return the description.
     */
    DBDRecordType getDBDRecordType(String recordTypeName);
    /**
     * @param recordType the DBDStructure that describes the recordType.
     * @return  (true,false) if the recordType (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addRecordType(DBDRecordType recordType);
    /**
     * get a collection of all the recordTypes.
     * @return the Collection
     */
    Collection<DBDRecordType> getDBDRecordTypeList();
    /**
     * @param linkSupportName the name of the link support desired.
     * @return the DBDLink Support.
     */
    DBDLinkSupport getLinkSupport(String linkSupportName);
    /**
     * @param linkSupport.
     * @return  (true,false) if the link support (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addLinkSupport(DBDLinkSupport linkSupport);
    /**
     * get a collection of all the link supports.
     * @return the collection.
     */
    Collection<DBDLinkSupport> getLinkSupportList();
}
