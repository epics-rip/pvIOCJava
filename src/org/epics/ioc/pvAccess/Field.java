/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Field is an introspection interface to a field of a (PV) process variable
 * @author mrk
 *
 */
public interface Field {
    /**
     * @return the field name
     */
    String getName();
    /**
     * @return the field type
     */
    PVType getPVType();
    /**
     * @return returns true if the field is a constant.
     * Being constant means that the data associated with the field
     * can not be modified via the PVData interfaces.
     */
    boolean isConstant();
    /**
     * @param propertyName the name of the desired property
     * @return A Property interface.
     * This is null if propertyName is not a property of the field.
     */
    Property getProperty(String propertyName);
    /**
     * @return A Property array.
     * If null is returned then the field has no associated properties.
     */
    Property[] getProperties();
}
