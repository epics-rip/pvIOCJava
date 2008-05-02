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
     * Set the createName for this field.
     * @param name The createName.
     */
    void setCreateName(String name);
    /**
     * Get the createName for this field.
     * @return The createName or null if no createName.
     */
    String getCreateName();
    /**
     * Set the supportName for this field.
     * @param name The supportName.
     */
    void setSupportName(String name);
    /**
     * Get the supportName for this field.
     * @return The supportName or null if no supportName.
     */
    String getSupportName();
    /**
     * Get the attribute interface for the field.
     * @return The FieldAttribute.
     */
    FieldAttribute getFieldAttribute();
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
}
