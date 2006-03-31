/**
 * 
 */
package org.epics.ioc.dbAccess;

/**
 * get/put a DBMenu  array.
 * The caller must be prepared to get/put the array in chunks.
 * The return argument is always the number of elements that were transfered.
 * It may be less than the number requested.
 * @author mrk
 *
 */
public interface DBMenuArray extends DBArray{
    /**
     * get values from a <i>DBMenuArray</i> and put them into <i>DBMenu[]to</i>
     * @param offset The offset to the first element to get.
     * @param len The maximum number of elements to transfer.
     * @param to The array into which the data is transfered.
     * @param toOffset The offset into the array.
     * @return The number of elements transfered.
     * This is always less than or equal to len.
     * If the value is less then get should be called again.
     */
    int get(int offset, int len, DBMenu[]to, int toOffset);
    /**
     * put values into a <i>DBMenuArray</i> from <i>DBMenu[]to</i>
     * @param offset The offset to the first element to put.
     * @param len The maximum number of elements to transfer.
     * @param from The array from which the data is taken.
     * @param fromOffset The offset into the array.
     * @return The number of elements transfered.
     * This is always less than or equal to len.
     * If the value is less then put should be called again.
     * @throws IllegalStateException if isMutable is false
     */
    int put(int offset, int len, DBMenu[]from, int fromOffset);
}
