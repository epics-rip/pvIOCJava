/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;

/**
 * reflection interface for a structure field.
 * @author mrk
 *
 */
public interface DBDStructureField extends DBDField,Structure {
    /**
     * get the DBDStructure definition for this field.
     * If the field definition did not specify a file name thid will return null.
     * @return the DBDStructure or null if a struicture was not defined.
     */
    DBDStructure getDBDStructure();
}
