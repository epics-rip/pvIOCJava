/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;

/**
 * Reflection interface for array fields
 * @author mrk
 *
 */
public interface DBDArrayField extends DBDField, Array {
    /**
     * Get the element DBType.
     * @return The DBType.
     */
    DBType getElementDBType();
}
