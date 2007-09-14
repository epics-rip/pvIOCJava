/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Interface for field reflection.
 * @author mrk
 *
 */
public interface Field {
    /**
     * Get the field name
     * @return the field name
     */
    String getFieldName();
    /**
     * Get the field type.
     * @return The field type.
     */
    Type getType();
    /**
     * Set the create name for this field.
     * @param name The create name.
     */
    void setCreateName(String name);
    /**
     * Get the create name for this field.
     * @return The createname.
     */
    String getCreateName();
    /**
     * Set the support name for this field.
     * @param name The name of the support.
     */
    void setSupportName(String name);
    /**
     * Get the name of support for this field.
     * @return The name of the support.
     */
    String getSupportName();
    /**
     * Convert to a string
     * @return The field as a string
     */
    String toString();
    /**
     * Convert to a string
     * @param indentLevel Indentation level
     * @return The field as a string
     */
    String toString(int indentLevel);
    /**
     * Get the attribute interface for the field.
     * @return The FieldAttribute.
     */
    FieldAttribute getFieldAttribute();
}
