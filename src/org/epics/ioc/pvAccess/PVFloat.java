/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put float data
 * @author mrk
 *
 */
public interface PVFloat extends PVData{
	/**
	 * @return float value of field
	 */
	float get();
    /**
     * @param value new float value for field
     */
    void put(float value);
}
