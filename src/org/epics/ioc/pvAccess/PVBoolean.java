/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put boolean data
 * @author mrk
 *
 */
public interface PVBoolean extends PVData{
	/**
     * get the <i>booolean</i> value stored in the PV.
	 * @return boolean value of field
	 */
	boolean get();
    /**
     * put the PV from a <i>boolean</i> value
     * @param value new boolean value for field
     */
    void put(boolean value);
}
