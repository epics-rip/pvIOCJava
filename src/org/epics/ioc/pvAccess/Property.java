/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Introspection for a field property
 * A property has a name.
 * The data for the property is described by another field of the PV.
 * @author mrk
 *
 */
public interface Property {
    /**
     * Get the property name
     * @return The property name
     */
    String getName();
    /**
     * Get the name of the field that has the property value
     * @return the field name
     */
    String getFieldName();
    /**
     * convert to a string
     * @param indentLevel indentation level
     * @return the property as a string
     */
    String toString(int indentLevel);
}
