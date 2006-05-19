/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * Interface for  structure field reflection.
 * @author mrk
 *
 */
public interface Structure extends Field{
    /**
     * get the name of the structure.
     * @return the structure name.
     */
    String getStructureName();
    /**
     * get the names of the fields.
     * @return the names of the fields.
     */
    String[] getFieldNames();
    /**
     * get the <i>Field</i> for the specified field.
     * @param fieldName the name of the field.
     * @return The <i>Field</i> that describes the field.
     */
    Field getField(String fieldName);
    /**
     * Get the index of the specified field.
     * @param fieldName the name of the field.
     * @return the index or -1 if fieldName is not a field in the structure.
     */
    int getFieldIndex(String fieldName);
    /**
     * get all the <i>Field</i>s for the structure.
     * @return an array of <i>Field</i> that describes
     * each of the fields in the structure.
     */
    Field[] getFields();
}
