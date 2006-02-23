/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put double data
 * @author mrk
 *
 */
public interface PVDouble extends PVData{
	/**
	 * @return double value of field
	 */
	double get();
    /**
     * @param value new double value for field
     */
    void put(double value);
}
