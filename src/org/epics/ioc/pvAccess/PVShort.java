/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put short data
 * @author mrk
 *
 */
public interface PVShort extends PVData{
	/**
	 * @return short value of field
	 */
	short get();
    /**
     * @param value new short value for field
     */
    void put(short value);
}
