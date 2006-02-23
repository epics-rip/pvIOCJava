/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put boolean data
 * @author mrk
 *
 */
public interface PVBoolean extends PVData{
	/**
	 * @return boolean value of field
	 */
	boolean get();
    /**
     * @param value new boolean value for field
     */
    void put(boolean value);
}
