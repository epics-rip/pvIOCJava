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
     * get the record that is being accessed.
     * @return the interface for the record instance.
     */
    DBRecord getDbRecord();
    /**
     * specify a field to access.
     * The search is relative to the current field as specified
     * by the last call to setField that returned AccessSetResult.local.
     * @param name the field name.
     * A null or empty string resets to the record itself being accessed.
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
     * If the associatedField starts with "/" then the search for
     * the propertyName starts at the record itself.
     * @return (false,true) if the field (is not,is) found.
     * If it is not found the then access is set to the record itself.
     * Also if it is not found getRemoteField make provide the name of another record that has the data.
     */
    AccessSetResult setField(String name);
    /**
     * if setField returned AccessSetResult.otherRecord return the name of the other record.
     * @return the name of the other record.
     * The record may be located in a different IOC.
     * Thu Channel Access not DBAccess should be used to connect to the other record.
     */
    String getOtherRecord();
    /**
     * if setField returned AccessSetResult.otherRecord return the name of the field in the other record.
     * @return
     */
    String getOtherField();
    /**
     * set field.
     * @param dbData a field of the record instance.
     * If dbData is null then the field is set to the record instance itself.
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
     * get the field data interface for the specified property.
     * @param property the property.
     * It must be a property associated with the field related to the last setField.
     * If setField was not called or was a reset call then the property must be a property
     * of the record itself.
     * @return the field interface or null if the property or field does not exist.
     */
    DBData getPropertyField(Property property);
    /**
     * get the field data interface for the specified propertyName.
     * @param propertyName the property name.
     * It must be the name of a property associated with the field related to the last setField.
     * If setField was not called or was a reset call then the property must be a property
     * of the record itself.
     * @return the field interface or null if the property or field does not exist.
     */
    DBData getPropertyField(String propertyName);
    /**
     * replace the data implementation for a field.
     * @param oldField the interface for the old implementation.
     * @param newField the new implementation.
     */
    void replaceField(DBData oldField, DBData newField);
}
