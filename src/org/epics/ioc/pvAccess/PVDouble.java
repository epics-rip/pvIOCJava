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
     * get the <i>double</i> value stored in the PV
	 * @return double value of field
	 */
	double get();
    /**
     * put the <i>double</i> value into the PV
     * @param value new double value for field
     */
    void put(double value);
}
