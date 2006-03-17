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
     * get the <i>float</i> value stored in the PV
	 * @return float value of field
	 */
	float get();
    /**
     * put the <i>float</i> value into the PV
     * @param value new float value for field
     */
    void put(float value);
}
