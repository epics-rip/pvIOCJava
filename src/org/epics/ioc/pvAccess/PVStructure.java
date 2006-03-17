/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get the PVData for the structure fields
 * @author mrk
 *
 */
public interface PVStructure extends PVData{	
	/**
     * get the <i>PVData</i> array for the fields of the structure
	 * @return array of PVData. One for each field
	 */
	PVData[] getFieldPVData();
}
