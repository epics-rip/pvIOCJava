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
	 * @return string value of field
	 */
	String get();
    /**
     * @param value new string value for field
     */
    void put(String value);
}
