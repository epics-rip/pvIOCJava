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
     * get the <i>long</i> value stored in the PV
	 * @return long value of field
	 */
	long get();
    /**
     * put the <i>long</i> value into the PV
     * @param value new long value for field
     */
    void put(long value);
}
