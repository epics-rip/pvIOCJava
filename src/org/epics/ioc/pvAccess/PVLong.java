/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put long data
 * @author mrk
 *
 */
public interface PVLong extends PVData{
	/**
	 * @return long value of field
	 */
	long get();
    /**
     * @param value new long value for field
     */
    void put(long value);
}
