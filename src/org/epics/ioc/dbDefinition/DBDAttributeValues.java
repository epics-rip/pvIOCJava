/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

/**
 * interface that provides values for field attributes.
 * @author mrk
 *
 */
public interface DBDAttributeValues {
    /**
     * Get the number of attributes.
     * Once this is known the caller can iterate through all the attributes.
     * @return the number of attributes.
     */
    int getLength();
    /**
     * get the attribute value for name.
     * If the name is not an attribute null is returned.
     * @param name the name of the desired attribute.
     * @return the value or null if name does not exist.
     */
    String getValue(String name);
    /**
     * Get the name for the specified attribute.
     * @param index the index of the desired attribute.
     * @return the name or null if index is out of bounds.
     */
    String getName(int index);
    /**
     * Get the value for the specified attribute.
     * @param index the name or null if index is out of bounds.
     * @return the name or null if index is out of bounds.
     */
    String getValue(int index);
}
