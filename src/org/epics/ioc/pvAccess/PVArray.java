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
     * Get the current length of the array
     * @return The current length of the array
     */
    int getLength();
    /**
     * Set the length of the array
     * @param len Set the length. Is this needed?
     */
    void setLength(int len);
    /**
     * Get the current capacity of the array, i.e. the allocated number of elements
     * @return The capacity
     */
    int getCapacity();
    /**
     * Set the capacity.
     * @param len The new capacity for the array
     */
    void setCapacity(int len);
}
