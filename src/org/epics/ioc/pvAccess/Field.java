/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Introspection interface for a field.
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
     * Is the field a constant
     * @return returns true if the field is a constant.
     * Being constant means that the data associated with the field
     * can not be modified via the PVData interfaces.
     */
    boolean isConstant();
    /**
     * Set the field to be a constant
     * @param value should field be constant?
     * Being constant means that the data associated with the field
     * can not be modified via the PVData interfaces.
     */
    void setConstant(boolean value);
}
