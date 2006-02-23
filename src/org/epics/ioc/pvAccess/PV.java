/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * A Process Variable(PV) is something that has a name,
 * contains an arbitrary set of fields, and can also
 * have fields that are located in other Process Variables.
 * 
 * PV is the introspection interface for a Process Variable
 * @author mrk
 *
 */
public interface PV {
    /**
     * @return the Process Variable name
     */
    String getPVName();
    /**
     * @return The names of the fields contained in the PV
     */
    String[] getFieldNames();
    /**
     * @param fieldName The name of the field
     * @return The introspection interface for the field
     */
    Field getField(String fieldName);
    /**
     * @return An array of introspection interfaces for the fields
     * contained by the PV
     */
    Field[] getFields();
    /**
     * @param fieldName The name of a field
     * @return The name of the process variable that has the field
     */
    String getLinkedFieldPV(String fieldName);
    /**
     * @return An array of strings
     */
    String[] getLinkedFieldNames();
}
