/**
 * 
 */
package org.epics.ioc.dbDefinition;

/**
 * support for a field that is a structure
 * @author mrk
 *
 */
public interface DBDStructureField extends DBDField {
    
    /**
     * get the structure describing the field
     * @return the DBDStructure
     */
    DBDStructure getDBDStructure();
}
