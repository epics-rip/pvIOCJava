/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Return a PVStructure interface for an array element
 * @author mrk
 *
 */
public interface PVStructureArray extends PVArray{
	/**
	 * @param index of the element to get
	 * @return Interface for accessing the structure data
	 */
	PVStructure getInterface(int index);
}
