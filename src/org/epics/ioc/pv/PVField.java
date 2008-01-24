/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.Requester;

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
     * Get the PVField subfield with name fieldName.
     * This is only useful for PVStructure, PVStructureArray and PVArrayArray fields.
     * @param fieldName The fieldName.
     * @return The PVField or null if the subfield does not exist.
     */
    PVField getSubField(String fieldName);
    /**
     * If the PVField is an enumerated structure create and return a PVEnumerated.
     * @return The PVEnumerated interface or null if the field is not an enumerated structure.
     */
    PVEnumerated getPVEnumerated();
    /**
     * Find a field that is a subfield or a property of this field.
     * The fieldName is of the form item.item... where item is name or name[index].
     * 
     * The algorithm implemented by findProperty is:
     * <ul>
     *  <li>Start with the leftmost item and find it.</li>
     *  <li>find the next leftmost item and find it.</li>
     *  <li>Continue until all items have been found or a search fails.</li>
     *  <li>Return the interface for the last item or null if a search fails.</li>
     * </ul>
     * 
     *  An item is found as follows:
     *  <ul>
     *   <li>Find the name part of item. If no [index] is present then fail.</li>
     *   <li>If [index] is present than field must be a structure with elementType structure or array.
     *  If so then make sure the index element is found. If so it is the field.</li>
     *   <li>fail</li>
     *  </ul>
     *  
     *  A name is found as follows:
     *  <ol>
     *    <li>If the Field for the current PVField is named "value" back up one level in parent tree.</li>
     *    <li>The current PV must be a structure. If not fail.</li>
     *    <li>If the current PVField is type structure with a fieldName=name then use it.</li>
     *    <li>If the fieldName is not timeStamp than fail.</li>
     *    <li>If the parent tree is null then fail.</li>
     *    <li>Back up one level in the parent tree and go to 2).</li>
     *  </ol>
     *  
     * @param fieldName A string of the form item.item... where item is name or name[index]
     * @return The PVField interface for the property or null if not found. 
     */
    PVField findProperty(String fieldName);
    /**
     * Find a property by searching up the parent tree.
     * @param propertyName The property name which is expected to match the name of a field.
     * @return The interface to the first field found that is not a null structure or null if not found.
     */
    PVField findPropertyViaParent(String propertyName);
    /**
     * Get the names of all the properties for this PVField.
     * A property name is the field name.
     * If this PVfield is a structure then every field except null structures is a property.
     * If this PVField is the value field the parent is the starting point and the properties will
     * not include the value field itself. In addition a search up the parent tree is made for the timeStamp.
     * @return The String array for the names of the properties.
     */
    String[] getPropertyNames();
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
