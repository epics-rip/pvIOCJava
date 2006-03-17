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
     * get the <i>short</i> value stored in the PV
	 * @return short value of field
	 */
	short get();
    /**
     * put the <i>short</i> value into the PV
     * @param value new short value for field
     */
    void put(short value);
}
