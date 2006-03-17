/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Introspection for a structure field
 * @author mrk
 *
 */
public interface Structure extends Field{
    /**
     * get the name of the structure
     * @return the structure name
     */
    String getStructureName();
    /**
     * get the names of the fields
     * @return the names of the fields
     */
    String[] getFieldNames();
    /**
     * get the <i>Field</i> for the specified field
     * @param fieldName the name of the field
     * @return The <i>Field</i> that describes the field
     */
    Field getField(String fieldName);
    /**
     * get all the <i>Field</i>s for the structure
     * @return an array of <i>Field</i> that describes each of the fields in the structure.
     */
    Field[] getFields();
}
