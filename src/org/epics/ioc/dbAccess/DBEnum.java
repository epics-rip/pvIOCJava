/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;


/**
 * Provides access to a an array of String choices
 * and an index specifying the current choice.
 * @author mrk
 *
 */
public interface DBEnum extends DBData, PVEnum{}
