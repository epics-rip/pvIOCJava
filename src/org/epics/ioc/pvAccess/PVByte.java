/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put byte data
 * @author mrk
 *
 */
public interface PVByte extends PVData{
	/**
	 * @return byte value of field
	 */
	byte get();
    /**
     * @param value new byte value for field
     */
    void put(byte value);
}
