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
     * get the <i>String</i> value stored in the field
	 * @return string value of field
	 */
	String get();
    /**
     * put the <i>String</i> value into the field
     * @param value new string value for field
     * @throws IllegalStateException if the field is not mutable
     */
    void put(String value);
}
