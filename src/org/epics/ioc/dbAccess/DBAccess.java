package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;

/**
 * interface for accessing fields of a record instance.
 * @author mrk
 *
 */
public interface DBAccess {
    /**
     * get the record that is being accessed.
     * @return the interface for the record instance.
     */
    DBRecord getDbRecord();
    /**
     * specify a field to access.
     * @param name the field name.
     * A null or empty string resets to no field being accessed.
     * A '.' separates the name into subfields.
     * Each subfield is found as follows: The first search that succeeds is used.
     * <ol>
     * <li>If the field is a structure look in the subfields of the structure.
     * If the field is not a structure look at the fields of it's parent.
     * <li>If the field is not found than if the current field has a property with its propertyName
     * equal to name the associatedField is located and the search resumes in the associared field.</li>
     * <li>If the parent has a property with a propertyName
     *  that is the same as the name the associatedField is used.</li>
     *  </ol>
     * A subfield can access a structure element of an array of structures by following the
     * subfield name with [index].
     * If the associatedField is ".." then a search up the structure hierarchy is made to locate
     * the propertyName.
     * @return (false,true) if the field (is not,is) found.
     * If it is not found the then access is set to the record itself.
     */
    boolean setField(String name);
    /**
     * set field.
     * @param dbData a field of the record instance.
     * @throws IllegalArgumentException if the dbField is not in the record instance.
     */
    void setField(DBData dbData);
    /**
     * get the interface for the current field.
     * @return the interface for the field.
     * If setField was never called or the last call failed then null is returned.
     */
    DBData getField();
    /**
     * get the field data interface for the specified property name.
     * @param property the property.
     * It must be a property associated with the field related to the last setField.
     * If setField was not called or was a reset call then the property name must be a property
     * of the record itself.
     * @return the field interface or null if the property or field does not exist.
     */
    DBData getPropertyField(Property property);
    /**
     * replace the data implementation for a field.
     * @param oldField the interface for the old implementation.
     * @param newField the new implementation.
     */
    void replaceField(DBData oldField, DBData newField);
}
