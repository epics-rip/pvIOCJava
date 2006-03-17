/**
 * 
 */

package org.epics.ioc.pvAccess;

/**
 * Introspection for an enum field. An enum is a field that has an array of
 * choices. and an index describing the current choice.
 * 
 * @author mrk
 * 
 */

public interface Enum extends Field {
	/**
     * Can the choices be modified?
	 * @return (true,false) if choices (can, can't) be modified
	 */
	boolean isChoicesMutable();
}
