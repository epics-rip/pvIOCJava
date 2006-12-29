/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

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
     * Multiple DBDs can be created via DBDFieldFactory.
     * @return The name.
     */
    String getName();
    /**
     * Get the master DBD.
     * In order to support on-line add of new DBD components a master DBD can be created.
     * A separate DBD can be created for adding new components.
     * The new components can be added to the new DBD and when all new components have been added
     * the new DBD can be merged into the master DBD. 
     * @return The master DBD or null if no master exists.
     */
    DBD getMasterDBD();
    /**
     * Merge all definitions into the master DBD.
     * After the merge all definitions are cleared from this DBD and this DBD is removed from the DBDFactory list.
     */
    void mergeIntoMaster();
    /**
     * Get the DBDMenu for the specified name.
     * It will be returned if it resides in this DBD or in the master DBD.
     * @param menuName The menu to retrieve.
     * @return The DBDMenu or null if the menu does not exist.
     */
    DBDMenu getMenu(String menuName);
    /**
     * Add a menu definition.
     * @param menu The DBDMenu to add.
     * @return (true,false) if the menu (was not, was) added.
     * If it is already present in either this DBD or in the master DBD it is not added.
     */
    boolean addMenu(DBDMenu menu);
    /**
     * Get a Map of all menus in this DBD.
     * @return The Map.
     */
    Map<String, DBDMenu> getMenuMap();
    /**
     * Get the DBDStructure for the specified name.
     * It will be returned if it resides in this DBD or in the master DBD.
     * @param structureName The structure to retrieve.
     * @return The DBDStructure or null if the structure does not exist.
     */
    DBDStructure getStructure(String structureName);
    /**
     * Add a structure definition.
     * @param structure The DBDStructure to add.
     * @return  (true,false) if the structure (was not, was) added.
     * If it is already present in either this DBD or in the master DBD it is not added.
     */
    boolean addStructure(DBDStructure structure);
    /**
     * Get a Map of all the structures in this DBD.
     * @return The Map.
     */
    Map<String,DBDStructure> getStructureMap();
    /**
     * Get a DBDRecordType that describes the recordType.
     * It will be returned if it resides in this DBD or in the master DBD.
     * @param recordTypeName The recordTypeName.
     * @return The DBDRecordType or null if it does not exists.
     */
    DBDRecordType getRecordType(String recordTypeName);
    /**
     * Add a record type definition.
     * @param recordType The DBDRecordType that describes the recordType.
     * @return  (true,false) if the recordType (was not, was) added.
     * If it is already present in either this DBD or in the master DBD it is not added.
     */
    boolean addRecordType(DBDRecordType recordType);
    /**
     * Get a Map of all the recordTypes in this DBD.
     * @return The Map
     */
    Map<String,DBDRecordType> getRecordTypeMap();
    /**
     * Get a support.
     * It will be returned if it resides in this DBD or in the master DBD.
     * @param supportName The name of the support desired.
     * @return The DBDSupport or null if it does not exist.
     */
    DBDSupport getSupport(String supportName);
    /**
     * Add a support definition.
     * @param support The support to add.
     * @return  (true,false) if the support (was not, was) added.
     * If it is already present in either this DBD or in the master DBD it is not added.
     */
    boolean addSupport(DBDSupport support);
    /**
     * Get a Map of all the supports in this DBD.
     * @return The Map.
     */
    Map<String,DBDSupport> getSupportMap();
    /**
     * Get a link support.
     * It will be returned if it resides in this DBD or in the master DBD.
     * @param linkSupportName The name of the support desired.
     * @return The DBDSupport or null if it does not exist.
     */
    DBDLinkSupport getLinkSupport(String linkSupportName);
    /**
     * Add a link support definition.
     * @param support The support to add.
     * @return  (true,false) if the support (was not, was) added.
     * If it is already present in either this DBD or in the master DBD it is not added.
     */
    boolean addLinkSupport(DBDLinkSupport support);
    /**
     * Get a Map of all the supports in this DBD.
     * @return The Map.
     */
    Map<String,DBDLinkSupport> getLinkSupportMap();
    /**
     * Generate a list of menu definitions with menu names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] menuList(String regularExpression);
    /**
     * Dump all the menu definitions with menu names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String menuToString(String regularExpression);
    /**
     * Generate a list of structure definitions with structure names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] structureList(String regularExpression);
    /**
     * Dump all the structure definitions with structure names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String structureToString(String regularExpression);
    /**
     * Generate a list of recordType definitions with recordType names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] recordTypeList(String regularExpression);
    /**
     * Dump all the recordType definitions with recordType names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String recordTypeToString(String regularExpression);
    /**
     * Generate a list of support definitions with support names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] supportList(String regularExpression);
    /**
     * Dump all the support definitions with support names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String supportToString(String regularExpression);
    /**
     * Generate a list of link support definitions with support names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] linkSupportList(String regularExpression);
    /**
     * Dump all the link support definitions with support names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String linkSupportToString(String regularExpression);
}
