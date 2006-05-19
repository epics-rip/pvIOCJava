/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

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
     * @return the property name.
     */
    String getName();
    /**
     * get the name of the field that has the property value.
     * @return the field name.
     */
    String getFieldName();
    /**
     * convert to a string.
     * @return the property as a string.
     */
    String toString();
    /**
     * convert to a string.
     * @param indentLevel indentation level.
     * @return the property as a string.
     */
    String toString(int indentLevel);
}
