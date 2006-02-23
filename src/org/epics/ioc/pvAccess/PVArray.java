/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Base interface for array data.
 * Each PVType has an array interface that extends PVArray
 * @author mrk
 *
 */
public interface PVArray extends PVData{
    /**
     * @return The current length of the array
     */
    int getLength();
    /**
     * @param len Set the length. Is this needed?
     */
    void setLength(int len);
    /**
     * @return The capacity
     */
    int getCapacity();
    /**
     * @param Set tthe capacity
     */
    void setCapacity(int len);
}
