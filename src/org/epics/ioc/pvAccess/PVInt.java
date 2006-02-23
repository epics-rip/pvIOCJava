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
	 * @return int value of field
	 */
	int get();
    /**
     * @param value new int value for field
     */
    void put(int value);
}
