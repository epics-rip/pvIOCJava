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
     * Get the <i>Field</i> that has the property value
     * @return A <i>Field</i> that describes the data associated
     * with the property
     */
    Field getField();
}
