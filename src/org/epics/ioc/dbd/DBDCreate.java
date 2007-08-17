/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbd;

/**
 * Reflection interface for a DBDSupport.
 * @author mrk
 *
 */
public interface DBDCreate {
    /**
     * Get the create name.
     * @return The name.
     */
    String getCreateName();
    /**
     * Get the name of the factory for create.
     * @return The name.
     */
    String getFactoryName();
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
