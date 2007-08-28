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
    void setAttributes(Map<String,String> attributes,String[] exclude);
    String setAttribute(String key,String value);
    Map<String,String> getAttributes();
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
