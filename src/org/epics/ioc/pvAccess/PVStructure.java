/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get the PVData for the structure fields
 * @author mrk
 *
 */
public interface PVStructure {	
	/**
	 * @return array of PVData. One for each field
	 */
	PVData[] getFieldPVData();
}
