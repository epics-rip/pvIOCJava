/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get the PVArray interface for an element of the array of arrays
 * Introspection can be used to determine the element type
 * @author mrk
 *
 */
public interface PVArrayArray extends PVArray{
	/**
	 * @param index The element of the array of array to get
	 * @return Interface to the array
	 */
	PVArray getInterface(int index);
}
