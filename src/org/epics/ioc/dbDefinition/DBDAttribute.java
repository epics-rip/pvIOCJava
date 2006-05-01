package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;

/**
 * reflection interface for field attributes.
 * @author mrk
 *
 */
public interface DBDAttribute {
    /**
     * get the field name.
     * @return the name.
     */
    String getName();
    /**
     * get the DBType of the field.
     * @return the DBType.
     */
    DBType getDBType();
    /**
     * Get the Type of the field.
     * @return the type.
     */
    Type getType();
    /**
     * get the default value for the field as a String.
     * @return the default.
     */
    String getDefault();
    /**
     * is the field readonly .
     * @return (false,true) if it (is not, is) readonly.
     */
    boolean isReadOnly();
    /**
     * can this field be configurable by Database Configuration Tools.
     * @return (false,true) if it (can't, can) be configured.
     */
    boolean isDesign();
    /**
     * Is this field is a link to another record.
     * @return (false,true) if it (is not, is) a link to another record.
     */
    boolean isLink();
    /**
     * get the Access Security Level for this field.
     * @return the level.
     */
    int getAsl();
    /**
     * Get the DBDMenu of a menu field.
     * If the field is not a menu null is returned.
     * @return the DBDMenu or null if field is not a menu.
     */
    DBDMenu getMenu();
    /**
     * Get the DBDStructure of a structure field.
     * If the field is not a structure null is returned.
     * @return the DBDStructure of null if the field is not a structure.
     */
    DBDStructure getStructure();
    /**
     * Get the element DBType of an array field.
     * dbPvType is returned if the field is not an array. The Type will be pvUnknown.
     * @return the DBtype.
     */
    DBType getElementDBType();
    /**
     * Get the element Type.
     * If the file is not an array pvUnknown is returned.
     * @return the Type.
     */
    Type getElementType();
    /**
     * create a string describing the properties.
     * @param indentLevel indent level. Ecah level is four spaces.
     * @return the string describing the properties.
     */
    String toString(int indentLevel);
    
}
