/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Convert between PV types or convert a field to a string.
 * Conversions are between scalar numeric types or between arrays of
 * numeric types. It is not possible to convert between a scalar
 * and an array.
 * The conversions are between a primitive numeric type and a PV that has <i>Type</i>:
 * <i>pvByte</i>, <i>pvShort</i>, <i>pvInt</i>, <i>pvLong</i>, <i>pvFloat</i>, or <i>pvDouble</i>.
 * 
 * <p><i>getString</i> will convert any PV <i>Type</i> to a <i>String</i>.
 * All code that implements any PVData interface should implement method <i>toString</i> by
 * calling this method.</p>
 * @author mrk
 *
 */
public interface Convert {
    /**
     * Convert the PV to a string.
     * Code that implements PVData should call this to implement <i>toString</i>.
     * @param pv the PV to convert to a string.
     * If the PV is a structure or array be prepared for a very long string.
     * @param indentLevel indentation level
     * @return value converted to string
     */
    String getString(PVData pv, int indentLevel);
    /**
     * Convert the PV to a string.
     * Code that implements PVData should call this to implement <i>toString</i>.
     * @param pv the PV to convert to a string.
     * If the PV is a structure or array be prepared for a very long string.
     * @return value converted to string
     */
    String getString(PVData pv);
    /**
     * Convert the PV to a <byte>.
     * @param pv the PV
     * @return The pv conveted to a byte.
     */
    byte toByte(PVData pv);
    /**
     * Convert the PV to a <i>short</i>.
     * @param pv the PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    short toShort(PVData pv);
    /**
     * Convert the PV to an <i>int</i>
     * @param pv the PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    int   toInt(PVData pv);
    /**
     * Convert the PV to a <i>long</i>
     * @param pv the PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    long  toLong(PVData pv);
    /**
     * Convert the PV to a <i>float</i>
     * @param pv the PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    float toFloat(PVData pv);
    /**
     * Convert the PV to a <i>double</i>
     * @param pv the PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    double toDouble(PVData pv);
    /**
     * Convert the PV from a <i>byte</i>
     * @param pv the PV
     * @param from source for data to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void fromByte(PVData pv, byte from);
    /**
     * Convert the PV from a <i>short</i>
     * @param pv the PV
     * @param from source for data to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromShort(PVData pv, short from);
    /**
     * Convert the PV from an <i>int</i>
     * @param pv the PV
     * @param from source for data to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromInt(PVData pv, int from);
    /**
     * Convert the PV from a <i>long</i>
     * @param pv the PV
     * @param from source for data to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromLong(PVData pv, long from);
    /**
     * Convert the PV from a <i>float</i>
     * @param pv the PV
     * @param from source for data to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromFloat(PVData pv, float from);
    /**
     * Convert the PV from a <i>double</i>
     * @param pv the PV
     * @param from source for data to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromDouble(PVData pv, double from);
    /**
     * Convert the PV from a <i>String</i>
     * @param pv the PV
     * @param from a String value to convert and put into the PV
     * @throws IllegalArgumentException if the Type is not a scalar
     * @throws NumberFormatException if the String does not have a valid value.
     */
    void fromString(PVData pv,String from);
    /**
     * Convert the PV array to a <i>byte</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int toByteArray(PVData pv, int offset, int len, byte[]to, int toOffset);
    /**
     * Convert the PV array to a <i>short</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int toShortArray(PVData pv, int offset, int len, short[]to, int toOffset);
    /**
     * Convert the PV array to an <i>int</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int toIntArray(PVData pv, int offset, int len, int[]to, int toOffset);
    /**
     * Convert the PV array to a <i>long</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int toLongArray(PVData pv, int offset, int len, long[]to, int toOffset);
    /**
     * Convert the PV array to a <i>float</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int toFloatArray(PVData pv, int offset, int len, float[]to, int toOffset);
    /**
     * Convert the PV array to a <i>double</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int toDoubleArray(PVData pv, int offset, int len, double[]to, int toOffset);
    /**
     * Convert the PV array from a <i>byte</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int fromByteArray(PVData pv, int offset, int len, byte[]from, int fromOffset);
    /**
     * Convert the PV array from a <i>short</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int fromShortArray(PVData pv, int offset, int len, short[]from, int fromOffset);
    /**
     * Convert the PV array from an <i>int</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int fromIntArray(PVData pv, int offset, int len, int[]from, int fromOffset);
    /**
     * Convert the PV array from a <i>long</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int fromLongArray(PVData pv, int offset, int len, long[]from, int fromOffset);
    /**
     * Convert the PV array from a <i>float</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int fromFloatArray(PVData pv, int offset, int len, float[]from, int fromOffset);
    /**
     * Convert the PV array from a <i>double</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a numeric array
     */
    int fromDoubleArray(PVData pv, int offset, int len, double[]from, int fromOffset);
    /**
     * Convert the PV array from a <i>String</i> array.
     * @param pv the PV
     * @param offset starting element in the PV
     * @param len number of elements to transfer
     * @param from source for data to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the Type is not a scalar
     * @throws NumberFormatException if the String does not have a valid value.
     */
    int fromStringArray(PVData pv, int offset, int len, String[]from, int fromOffset);
}
