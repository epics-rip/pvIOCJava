/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * @author mrk
 *
 */
public interface Structure extends Field{
    String getStructureName();
    String[] getFieldNames();
    Field getField(String fieldName);
    Field[] getFields();
}
