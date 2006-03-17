/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put string data
 * @author mrk
 *
 */
public interface PVString extends PVData{
	/**
     * get the <i>String</i> value stored in the PV
	 * @return string value of field
	 */
	String get();
    /**
     * put the <i>String</i> value into the PV
     * @param value new string value for field
     */
    void put(String value);
}
