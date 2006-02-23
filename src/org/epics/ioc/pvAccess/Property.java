/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * A field of a process variable(PV) can have associated properties,
 *  where a property has a name and data.
 *  The data for the property is described by another field of the PV.
 * @author mrk
 *
 */
public interface Property {
    /**
     * @return Returns the property name
     */
    String getName();
    /**
     * @return Returns a Field interface mthat bdescribes the data
     */
    Field getField();
}
