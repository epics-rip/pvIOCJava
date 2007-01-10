/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.*;

/**
 * PVData is the base class for field data.
 * Each PVType has an interface that extends PVData.
 * @author mrk
 *
 */
public interface PVData extends Requestor {
    /**
     * Get the full field name, i.e. the complete hierarchy.
     * @return The name.
     */
    String getFullFieldName();
    /**
     * Get the <i>Field</i> that describes the field.
     * @return Field, which is the reflection interface.
     */
    Field getField();
    /**
     * Get the parent of this field.
     * @return The parent interface.
     */
    PVData getParent();
    /**
     * Get the record.
     * @return The record interface.
     */
    PVRecord getPVRecord();
    /**
     * Replace the data implementation for a field.
     * @param newPVData The new implementation for this field.
     */
    void replacePVData(PVData newPVData);
    /**
     * Get the support name if it exists.
     * @return The name of the support.
     */
    String getSupportName();
    /**
     * Set the name of the support or null to specify no support.
     * @param name The name.
     * @return null if the name was set or the reason why the name was not set.
     */
    String setSupportName(String name);
    /**
     * Convert the data to a string.
     * @return The string.
     */
    String toString();
    /**
     * Convert the data to a string.
     * Each line is indented.
     * @param indentLevel The indentation level.
     * @return The string.
     */
    String toString(int indentLevel);
}
