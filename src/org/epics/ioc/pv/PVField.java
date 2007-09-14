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
     * Can the data for the field be modified?
     * @return If it can be modified
     */
    boolean isMutable();
    /**
     * Specify if the data for the field can be modified
     * @param value (false,true) if the data (can not, can) be modified
     */
    void setMutable(boolean value);
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
     * This finds a property, i.e. a field related to this PVField.
     * The fieldName is of the form item.item... where item is name or name[index].
     * 
     * The algorithm implemented by findProperty is:
     * 1)Start with the leftmost item and find it.
     * 2) find the next leftmost item and find it.
     * 3) Continue until all items have been found or a search fails.
     * 4) Return the interface for the last item or null if a search fails.
     * 
     *  An item is found as follows:
     *  1) Find the name part of item. If no [index] is present then done
     *  2) If [index] is present than field must be a structure with elementType structure or array.
     *  If so then make sure the index element is found. If so it is the field
     *  
     *  A name is found as follows:
     *  1) If the Field for the current PVField is named "value" back up one level in parent tree.
     *  2) The current PV must be a structure.
     *  3) If the current PVField is type structure with a fieldName=name then use it.
     *  4) If the parent tree is null then fail.
     *  5) Back up one level in the parent tree and go to 2).
     *  
     * @param propertyName A string of the form item.item... where item is name or name[index]
     * @return The PVField interface for the property or null if not found. 
     */
    PVField findProperty(String propertyName);
    /**
     * Get all the propertys for this PVField.
     * If this PVField is the value field the parent is the starting point and the propertys will
     * not include the value field itself.
     * @return The PVField array for the property fields.
     */
    PVField[] getPropertys();
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
     * Set the support name or null to specify no support.
     * @param name The name.
     */
    void setSupportName(String name);
    /**
     * Call the asynchronous listener.
     * Each time a synchronous modification is made this is called before
     * any modification and after all modifications are complete.
     * The caller must not block between the two calls, i.e. the modification must be synchronous.
     */
    void asynAccessCallListener(boolean begin);
    /**
     * Start an asynchronous access of the field.
     * @param asynAccessListener The interface for notifying of synchronous accesses.
     * @return (false,true) if caller (is not, is) allowed to access the field.
     * The value will be false if other code is already registered..
     */
    boolean asynAccessStart(AsynAccessListener asynAccessListener);
    /**
     * End asynchronous access of the field.
     * @param asynAccessListener The interface for notifying of synchronous accesses.
     */
    void asynAccessEnd(AsynAccessListener asynAccessListener);
    /**
     * Is an asynchronous access active?
     * @return (false,true) if the field (is not, is) being asynchronously accessed.
     */
    boolean isAsynAccessActive();
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
