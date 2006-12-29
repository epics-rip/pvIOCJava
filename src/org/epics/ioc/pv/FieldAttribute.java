/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Reflection interface for field attributes.
 * @author mrk
 *
 */
public interface FieldAttribute {
    /**
     * Get the default value for the field as a String.
     * @return The default.
     */
    String getDefault();
    /**
     * Is the field readonly .
     * @return (false,true) if it (is not, is) readonly.
     */
    boolean isReadOnly();
    /**
     * Can this field be configurable by Database Configuration Tools.
     * @return (false,true) if it (can't, can) be configured.
     */
    boolean isDesign();
    /**
     * Is this field a link to another record.
     * @return (false,true) if it (is not, is) a link to another record.
     */
    boolean isLink();
    /**
     * Get the Access Security Level for this field.
     * @return The level.
     */
    int getAsl();
    /**
     * Create a string describing the attributes.
     * @param indentLevel Indent level. Each level is four spaces.
     * @return The string describing the attributes.
     */
    String toString(int indentLevel);
    
}
