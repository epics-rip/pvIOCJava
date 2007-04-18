/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Interface for accessing fields of a record instance.
 * @author mrk
 *
 */
public interface PVAccess {
    /**
     * Get the record that is being accessed.
     * @return The interface for the record instance.
     */
    PVRecord getPVRecord();
    /**
     * Specify a field to access.
     * The search is relative to the field located
     * by the last call to setField.
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
     * @return The PVField interface or null if the field is not found.
     */
    PVField findField(String name);
    /**
     * Set field.
     * @param pvField A field of the record instance.
     * If pvData is null then the field is set to the record instance itself.
     * @throws IllegalArgumentException If pvData is not in the record instance.
     */
    void setPVField(PVField pvField);
}
