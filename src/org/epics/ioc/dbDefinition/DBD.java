/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
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
     * get a Map of all menus.
     * @return the Map.
     */
    Map<String, DBDMenu> getMenuMap();
    /**
     * get the DBDStructure for the specified name.
     * @param structureName the structure to retrieve.
     * @return the DBDStructure or null if the structure does not exist.
     */
    DBDStructure getStructure(String structureName);
    /**
     * @param structure the DBDStructure to add.
     * @return  (true,false) if the structure (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addStructure(DBDStructure structure);
    /**
     * get a Map of all the structures.
     * @return the Map.
     */
    Map<String,DBDStructure> getStructureMap();
    /**
     * get a DBDStructure that describes the recordType.
     * @param recordTypeName get the description of a recordType.
     * @return the description.
     */
    DBDRecordType getRecordType(String recordTypeName);
    /**
     * @param recordType the DBDStructure that describes the recordType.
     * @return  (true,false) if the recordType (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addRecordType(DBDRecordType recordType);
    /**
     * get a Map of all the recordTypes.
     * @return the Map
     */
    Map<String,DBDRecordType> getRecordTypeMap();
    /**
     * get a linkSupport
     * @param linkSupportName the name of the link support desired.
     * @return the DBDLinkSupport.
     */
    DBDLinkSupport getLinkSupport(String linkSupportName);
    /**
     * add a linkSupport
     * @param linkSupport support to add.
     * @return  (true,false) if the link support (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addLinkSupport(DBDLinkSupport linkSupport);
    /**
     * get a Map of all the link supports.
     * @return the Map.
     */
    Map<String,DBDLinkSupport> getLinkSupportMap();
}
