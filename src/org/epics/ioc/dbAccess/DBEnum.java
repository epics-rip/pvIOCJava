/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;


/**
 * provides access to a an array of String choices
 * and an index specifying the current choice.
 * @author mrk
 *
 */
public interface DBEnum extends DBData, PVEnum{}
