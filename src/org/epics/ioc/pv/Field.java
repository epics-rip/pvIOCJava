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
     * Get the parent field.
     * This is the structure or null if this is a top level structure.
     * @return The parent field or null.
     */
    Field getParent();
    /**
     * Get the propertys for the field.
     * @return A Property array.
     * If null is returned then the field has no associated properties.
     */
    Property[] getPropertys();
    /**
     * Get a specific property
     * @param propertyName the name of the desired property
     * @return A Property interface.
     * This is null if propertyName is not a property of the field.
     */
    Property getProperty(String propertyName);
    /**
     * Get the field type.
     * @return The field type.
     */
    Type getType();
    /**
     * Get the name of support for this field.
     * @return The name of the support.
     */
    String getSupportName();
    /**
     * Set the support name for this field.
     * @param name The name of the support.
     */
    void setSupportName(String name);
    /**
     * can the data for the field be modified?
     * @return if it can be modified
     */
    boolean isMutable();
    /**
     * specify if the data for the field can be modified
     * @param value (false,true) if the data (can not, can) be modified
     */
    void setMutable(boolean value);
    /**
     * convert to a string
     * @return the field as a string
     */
    String toString();
    /**
     * convert to a string
     * @param indentLevel indentation level
     * @return the field as a string
     */
    String toString(int indentLevel);
    /**
     * Get the attribute interface for the field.
     * @return The FieldAttribute.
     */
    FieldAttribute getFieldAttribute();
}
