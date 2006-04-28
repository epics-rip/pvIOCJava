/**
 *
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.Enum;

/**
 * reflection interface for enum fields.
 * It is used for Type.pvEnum fields.
 * @author mrk
 *
 */

public interface DBDEnumField extends DBDField, Enum { }
