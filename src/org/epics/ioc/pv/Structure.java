/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Interface for  structure field reflection.
 * @author mrk
 *
 */
public interface Structure extends Field{
    /**
     * Get the name of the structure.
     * @return The structure name.
     */
    String getStructureName();
    /**
     * Get the names of the fields.
     * @return The names of the fields.
     */
    String[] getFieldNames();
    /**
     * Get the <i>Field</i> for the specified field.
     * @param fieldName The name of the field.
     * @return The <i>Field</i> that describes the field.
     */
    Field getField(String fieldName);
    /**
     * Get the index of the specified field.
     * @param fieldName The name of the field.
     * @return The index or -1 if fieldName is not a field in the structure.
     */
    int getFieldIndex(String fieldName);
    /**
     * Get all the <i>Field</i>s for the structure.
     * @return An array of <i>Field</i> that describes
     * each of the fields in the structure.
     */
    Field[] getFields();
    /**
     * Return a copy of the structure.
     * @return The copy.
     */
    Structure copy();
    /**
     * Replace a field of the structure.
     * The structure must a copy created via a call to copy
     * and the field must have the same field name as the original.
     * This operation can only be performed once on a copy.
     * @param name The name of the field to replace.
     * @param field The replacement field.
     * @return (false,true) if the the field (was not, was) replaced.
     */
    boolean replaceField(String name,Field field);
}
