/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.FieldAttribute;

/**
 * DBD (DataBase Definition) is the interface for locating interfaces for
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
     * Create a DBDStructure.
     * @param name The name of the structure.
     * @param field An array of Field for the fields of the structure.
     * @param fieldAttribute The fieldAttribute for the structure.
     * @return The interface for the newly created structure.
     */
    public DBDStructure createStructure(String name,
        Field[] field,FieldAttribute fieldAttribute);
    /**
     * Create a DBDCreate.
     * @param createName The create name.
     * @param factoryName The name of the create factory.
     * @return the DBDCreate or null if it does not exist.
     */
    public DBDCreate createCreate(String createName,String factoryName);
    /**
     * Create a DBDSupport.
     * @param supportName The name of the support.
     * @param factoryName The name of the factory for creating support instances.
     * @return the DBDSupport or null if it already existed.
     */
    public DBDSupport createSupport(String supportName,String factoryName);
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
     * Get an array of all the structures in this DBD.
     * @return The Map.
     */
    DBDStructure[] getDBDStructures();
    /**
     * Get a create.
     * It will be returned if it resides in this DBD or in the master DBD.
     * @param createName The name of the create desired.
     * @return The DBDCreate or null if it does not exist.
     */
    DBDCreate getCreate(String createName);
    /**
     * Add a create definition.
     * @param create The create to add.
     * @return  (true,false) if the create (was not, was) added.
     * If it is already present in either this DBD or in the master DBD it is not added.
     */
    boolean addCreate(DBDCreate create);
    /**
     * Get an array of all the creates in this DBD.
     * @return The Map.
     */
    DBDCreate[] getDBDCreates();
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
     * Get an array of all the supports in this DBD.
     * @return The Map.
     */
    DBDSupport[] getDBDSupports();
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
     * Generate a list of create definitions with names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string array containing the list.
     */
    String[] createList(String regularExpression);
    /**
     * Dump all the create definitions with names that match the regular expression.
     * @param regularExpression The regular expression.
     * @return A string containing the dump.
     */
    String createToString(String regularExpression);
    
}
