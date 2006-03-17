/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put a PVArray array.
 * The caller must be prepared to get/put the array in chunks.
 * The return argument is always the number of elements that were transfered.
 * It may be less than the number requested.
 * @author mrk
 *
 */
public interface PVArrayArray extends PVArray{
    /**
     * get values from the <i>PVArrayArray</i> and put them in <i>PVArray[]to</i>
     * @param offset The offset to the first element to get.
     * @param len The maximum number of elements to transfer.
     * @param to The array into which the data is transfered.
     * @param toOffset The offset into the array.
     * @return The number of elements transfered.
     * This is always less than or equal to len.
     * If the value is less then get should be called again.
     */
    int get(int offset, int len, PVArray[]to, int toOffset);
    /**
     * put values into the <i>PVArrayArray</i> from <i>PVArray[]to</i>
     * @param offset The offset to the first element to put.
     * @param len The maximum number of elements to transfer.
     * @param from The array from which the data is taken.
     * @param fromOffset The offset into the array.
     * @return The number of elements transfered.
     * This is always less than or equal to len.
     * If the value is less then put should be called again.
     */
    int put(int offset, int len, PVArray[]from, int fromOffset);
}
