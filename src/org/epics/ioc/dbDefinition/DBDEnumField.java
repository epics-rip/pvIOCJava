/**
 *
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.Enum;

/**
 * The interface for enum and menu fields.
 * It is used for Type.pvEnum fields, which includes DBType.dbMenu.
 * @author mrk
 *
 */

public interface DBDEnumField extends DBDField, Enum { }
