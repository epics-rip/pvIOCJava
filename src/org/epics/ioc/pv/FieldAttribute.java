/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.Map;

/**
 * Reflection interface for field attributes.
 * @author mrk
 *
 */
public interface FieldAttribute {
    /**
     * Add a set of attributes to the map.
     * @param attributes A map containing the attributes to add.
     * @param exclude An array of key names that will not be added.
     */
    void setAttributes(Map<String,String> attributes,String[] exclude);
    /**
     * Add a single attribute to the map of attributes.
     * @param key The key.
     * @param value The value.
     * @return The previous value for the key or null if none existed.
     */
    String setAttribute(String key,String value);
    /**
     * Get a map of the current attributes.
     * @return The map.
     */
    Map<String,String> getAttributes();
    /**
     * Get a single attribute value.
     * @param key The key.
     * @return The value or null of the key does not exist.
     */
    String getAttribute(String key);
    /**
     * Generate a string describing the attributes.
     * @return The string describing the attributes.
     */
    String toString();
    /**
     * Generate a string describing the attributes.
     * @param indentLevel Indent level. Each level is four spaces.
     * @return The string describing the attributes.
     */
    String toString(int indentLevel);
    
}
