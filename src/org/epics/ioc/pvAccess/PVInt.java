/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put int data.
 * @author mrk
 *
 */
public interface PVInt extends PVData{
    /**
     * get the <i>int</i> value stored in the field.
     * @return int value of field.
     */
    int get();
    /**
     * put the <i>int</i> value into the field.
     * @param value new int value for field.
     * @throws IllegalStateException if the field is not mutable.
     */
    void put(int value);
}
