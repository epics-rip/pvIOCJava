/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.Enum;

/**
 * reflection interface for menu fields.
 * It is used for DBType.dbMenu.
 * @author mrk
 *
 */

public interface DBDMenuField extends DBDField, Enum { }