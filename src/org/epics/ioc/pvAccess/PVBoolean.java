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
     * get the <i>booolean</i> value stored in the field.
     * @return boolean value of field.
    */
    boolean get();
    /**
     * put the field from a <i>boolean</i> value.
     * @param value new boolean value for field.
     * @throws IllegalStateException if the field is not mutable.
     */
    void put(boolean value);
}
