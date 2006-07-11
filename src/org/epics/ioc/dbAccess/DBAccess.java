/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;

/**
 * interface for accessing fields of a record instance.
 * @author mrk
 *
 */
public interface DBAccess {
    /**
     * Get the record that is being accessed.
     * @return The interface for the record instance.
     */
    DBRecord getDbRecord();
    /**
     * Specify a field to access.
     * The search is relative to the field located
     * by the last call to setField that returned AccessSetResult.local.
     * If this is the first call to setField the search begins at the record instance.
     * @param name The field name.
     * A null or empty string resets to the record itself being accessed.
     * A '.' separates the name into subfields.
     * Each subfield is found as follows: The first search that succeeds is used.
     * <ol>
     * <li>If the field is a structure look in the subfields of the structure.
     * If the field is not a structure look at the fields of it's parent.
     * <li>If the field is not found than if the current field has a property with its propertyName
     * equal to name the associatedField is located and the search resumes with the associated field.</li>
     * <li>If the parent has a property with a propertyName
     *  that is the same as the name the associatedField is used.</li>
     *  </ol>
     * A subfield can access a structure element of an array of structures by following the
     * subfield name with [index].
     * If the associatedField starts with "/" then the search for
     * the propertyName starts at the record itself.
     * @return otherRecord, thisRecord, or notFound.
     * If otherRecord is returned it is not found getOtherRecord and getOtherField can be used to
     * create another DBAccess for accessing the data.
     * If thisRecord is returned then getField can be used to get the interface for accessing the field.
     * If notFound is returned then the call makes no change to the internal state of the DBAccess.
     */
    AccessSetResult setField(String name);
    /**
     * Return the name of the record set by the last call to setField that returned otherRecord.
     * @return the name of the other record.
     * The record may be located in a different IOC.
     * Thu Channel Access not DBAccess should be used to connect to the other record.
     */
    String getOtherRecord();
    /**
     * Return the name of the field set by the last call to setField that returned otherRecord.
     * @return the field within the other record.
     */
    String getOtherField();
    /**
     * Set field.
     * @param dbData A field of the record instance.
     * If dbData is null then the field is set to the record instance itself.
     * @throws IllegalArgumentException If the dbField is not in the record instance.
     */
    void setField(DBData dbData);
    
    /**
     * Get the interface for the current field.
     * @return The interface for the field.
     * If setField was never called or the last call failed then null is returned.
     */
    DBData getField();
    /**
     * Get the field data interface for the specified property.
     * @param property The property.
     * It must be a property associated with the field related to the last setField.
     * If setField was not called or was a reset call then the property must be a property
     * of the record itself.
     * @return The field interface or null if the property or field does not exist.
     */
    DBData getPropertyField(Property property);
    /**
     * Get the field data interface for the specified propertyName.
     * @param propertyName The property name.
     * It must be the name of a property associated with the field related to the last setField.
     * If setField was not called or was a reset call then the property must be a property
     * of the record itself.
     * @return the field interface or null if the property or field does not exist.
     */
    DBData getPropertyField(String propertyName);
    /**
     * Replace the data implementation for a field.
     * @param oldField The interface for the old implementation.
     * @param newField The new implementation.
     */
    void replaceField(DBData oldField, DBData newField);
}
