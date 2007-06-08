/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.*;

/**
 * PVField is the base class for each PVData field.
 * Each PVType has an interface that extends PVField.
 * @author mrk
 *
 */
public interface PVField extends Requester {
    /**
     * Get the fullFieldName, i.e. the complete hierarchy.
     * @return The name.
     */
    String getFullFieldName();
    /**
     * Get the full name, which is the recordName plus the fullFieldName
     * @return The name.
     */
    String getFullName();
    /**
     * Get the <i>Field</i> that describes the field.
     * @return Field, which is the reflection interface.
     */
    Field getField();
    /**
     * Get the parent of this field.
     * The parent can be a PVStructure, PVLink, or PVArray.
     * @return The parent interface.
     */
    PVField getParent();
    /**
     * Get the record.
     * @return The record interface.
     */
    PVRecord getPVRecord();
    /**
     * Replace the data implementation for a field.
     * @param newPVField The new implementation for this field.
     */
    void replacePVField(PVField newPVField);
    /**
     * Get the support name if it exists.
     * @return The name of the support.
     */
    String getSupportName();
    /**
     * Set the name of the support or null to specify no support.
     * @param name The name.
     */
    void setSupportName(String name);
    /**
     * Add a listener for asynchronous access of the field.
     * @param asynAccessListener The listener.
     */
    void asynAccessListenerAdd(AsynAccessListener asynAccessListener);
    /**
     * Remove a listener for asynchronous access of the field.
     * @param asynAccessListener The listener.
     */
    void asynAccessListenerRemove(AsynAccessListener asynAccessListener);
    /**
     * Call the asynchronous listeners.
     * Each time a synchronous modification is made this is called before
     * any modification and after all modifications are complete.
     * The caller must not block between the two calls, i.e. the modification must be synchronous.
     */
    void asynAccessCallListeners(boolean begin);
    /**
     * Register to be the asynchronous modifier of the field.
     * @param asynModifier  The modifier.
     * @return (false,true) if caller (is not, is) allowed to modify the field.
     * The value will be false if another modifier has registered.
     */
    boolean asynModifyStart(Object asynModifier);
    /**
     * /**
     * End asynchronous modification of the field.
     * @param asynModifier  The modifier.
     */
    void asynModifyEnd(Object asynModifier);
    /**
     * Is an asynchronous modifier registered?
     * @return (false,true) if the field (is not, is) being asynchronously modified.
     */
    boolean isAsynModifyActive();
    /**
     * Convert the PVField to a string.
     * @return The string.
     */
    String toString();
    /**
     * Convert the PVField to a string.
     * Each line is indented.
     * @param indentLevel The indentation level.
     * @return The string.
     */
    String toString(int indentLevel);
}
