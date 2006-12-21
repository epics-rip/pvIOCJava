/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * interface for field property reflection.
 * A property has a name.
 * The data for the property is described by another field of the PV.
 * @author mrk
 *
 */
public interface Property {
    /**
     * Get the property name.
     * @return The property name.
     */
    String getName();
    /**
     * Get the name of the field that has the property value.
     * @return The field name.
     */
    String getFieldName();
    /**
     * Convert to a string.
     * @return The property as a string.
     */
    String toString();
    /**
     * Convert to a string.
     * @param indentLevel Indentation level.
     * @return The property as a string.
     */
    String toString(int indentLevel);
}
