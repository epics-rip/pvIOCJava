/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put int data
 * @author mrk
 *
 */
public interface PVInt extends PVData{
	/**
     * get the <i>int</i> value stored in the PV
	 * @return int value of field
	 */
	int get();
    /**
     * put the <i>int</i> value into the PV
     * @param value new int value for field
     */
    void put(int value);
}
