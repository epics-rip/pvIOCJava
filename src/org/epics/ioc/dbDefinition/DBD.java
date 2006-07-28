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
     * Get the name of this DBD.
     * Multiple DBDs can be created via DBDCreateFactory.
     * @return The name.
     */
    String getName();
    /**
     * Get the DBDMenu for the specified name.
     * @param menuName The menu to retrieve.
     * @return The DBDMenu or null if the menu does not exist.
     */
    DBDMenu getMenu(String menuName);
    /**
     * @param menu The DBDMenu to add.
     * @return (true,false) if the menu (was not, was) added.
     * If the menu is already present it is not added.
     */
    boolean addMenu(DBDMenu menu);
    /**
     * Get a Map of all menus.
     * @return The Map.
     */
    Map<String, DBDMenu> getMenuMap();
    /**
     * Get the DBDStructure for the specified name.
     * @param structureName The structure to retrieve.
     * @return The DBDStructure or null if the structure does not exist.
     */
    DBDStructure getStructure(String structureName);
    /**
     * Add a DVDStructure.
     * @param structure The DBDStructure to add.
     * @return  (true,false) if the structure (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addStructure(DBDStructure structure);
    /**
     * Get a Map of all the structures.
     * @return The Map.
     */
    Map<String,DBDStructure> getStructureMap();
    /**
     * Get a DBDRecordType that describes the recordType.
     * @param recordTypeName The recordTypeName.
     * @return The description.
     */
    DBDRecordType getRecordType(String recordTypeName);
    /**
     * Add a record type description.
     * @param recordType The DBDRecordType that describes the recordType.
     * @return  (true,false) if the recordType (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addRecordType(DBDRecordType recordType);
    /**
     * Get a Map of all the recordTypes.
     * @return The Map
     */
    Map<String,DBDRecordType> getRecordTypeMap();
    /**
     * Get a support.
     * @param supportName The name of the support desired.
     * @return The DBDSupport.
     */
    DBDSupport getSupport(String supportName);
    /**
     * Add a support
     * @param support The support to add.
     * @return  (true,false) if the support (was not, was) added.
     * It is not added if it is already present.
     */
    boolean addSupport(DBDSupport support);
    /**
     * Get a Map of all the supports.
     * @return The Map.
     */
    Map<String,DBDSupport> getSupportMap();
}
