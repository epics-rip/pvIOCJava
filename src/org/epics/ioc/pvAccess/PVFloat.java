/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put float data.
 * @author mrk
 *
 */
public interface PVFloat extends PVData{
    /**
     * get the <i>float</i> value stored in the field.
     * @return float value of field.
     */
    float get();
    /**
     * put the <i>float</i> value into the field.
     * @param value new float value for field.
     * @throws IllegalStateException if the field is not mutable.
     */
    void put(float value);
}
