/**
 * 
 */
package org.epics.ioc.pvAccess;

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
    String getName();
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
     * Get the field type
     * @return the field type
     */
    Type getType();
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
}
