/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * reflection interface for field attributes.
 * @author mrk
 *
 */
public interface FieldAttribute {
    /**
     * get the default value for the field as a String.
     * @return the default.
     */
    String getDefault();
    /**
     * is the field readonly .
     * @return (false,true) if it (is not, is) readonly.
     */
    boolean isReadOnly();
    /**
     * can this field be configurable by Database Configuration Tools.
     * @return (false,true) if it (can't, can) be configured.
     */
    boolean isDesign();
    /**
     * Is this field is a link to another record.
     * @return (false,true) if it (is not, is) a link to another record.
     */
    boolean isLink();
    /**
     * get the Access Security Level for this field.
     * @return the level.
     */
    int getAsl();
    /**
     * create a string describing the attributes.
     * @param indentLevel indent level. Each level is four spaces.
     * @return the string describing the attributes.
     */
    String toString(int indentLevel);
    
}
