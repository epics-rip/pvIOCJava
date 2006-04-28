/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Convert between numeric types,  convert any field to a string,
 *  or convert from a string to a scalar field.
 * <p>Numeric conversions are between scalar numeric types or between arrays of
 * numeric types. It is not possible to convert between a scalar
 * and an array.
 * Numeric conversions are between types:
 * <i>pvByte</i>, <i>pvShort</i>, <i>pvInt</i>,
 * <i>pvLong</i>, <i>pvFloat</i>, or <i>pvDouble</i>.</p>
 * 
 * <p><i>getString</i> converts any supported type to a <i>String</i>.
 * Code that implements a PVData interface can implement
 * method <i>toString</i> by calling this method.</p>
 *
 * <p><i>fromString</i> converts a <i>String<i> to a scalar.
 * <i>fromStringArray</i> converts an array of <i>String</i>
 * to a <i>pvArray</i>, which must have a scaler element type.
 * A scalar field is a numeric field or <i>pvBoolean</i> or <i>pvString</i>.</p>
 * @author mrk
 *
 */
public interface Convert {
    /**
     * Convert a PV to a string.
     * @param pv a PV to convert to a string.
     * If a PV is a structure or array be prepared for a very long string.
     * @param indentLevel indentation level
     * @return value converted to string
     */
    String getString(PVData pv, int indentLevel);
    /**
     * Convert a PV to a string.
     * @param pv a PV to convert to a string.
     * If a PV is a structure or array be prepared for a very long string.
     * @return value converted to string
     */
    String getString(PVData pv);
    /**
     * Convert a PV from a <i>String</i>
     * @param pv a PV
     * @param from a String value to convert and put into a PV
     * @throws IllegalArgumentException if the Type is not a scalar
     * @throws NumberFormatException if the String does not have a valid value.
     */
    void fromString(PVData pv,String from);
    /**
     * Convert a PV array from a <i>String</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * @param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element Type is not a scalar
     * @throws NumberFormatException if the String does not have a valid value.
     */
    int fromStringArray(PVData pv, int offset, int len, String[]from, int fromOffset);
    /**
     * Convert a PV to a <byte>.
     * @param pv a PV
     * @return converted value
     */
    byte toByte(PVData pv);
    /**
     * Convert a PV to a <i>short</i>.
     * @param pv a PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    short toShort(PVData pv);
    /**
     * Convert a PV to an <i>int</i>
     * @param pv a PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    int   toInt(PVData pv);
    /**
     * Convert a PV to a <i>long</i>
     * @param pv a PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    long  toLong(PVData pv);
    /**
     * Convert a PV to a <i>float</i>
     * @param pv a PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    float toFloat(PVData pv);
    /**
     * Convert a PV to a <i>double</i>
     * @param pv a PV
     * @return converted value
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    double toDouble(PVData pv);
    /**
     * Convert a PV from a <i>byte</i>
     * @param pv a PV
     * @param from value to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void fromByte(PVData pv, byte from);
    /**
     * Convert a PV from a <i>short</i>
     * @param pv a PV
     * @param from value to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromShort(PVData pv, short from);
    /**
     * Convert a PV from an <i>int</i>
     * @param pv a PV
     * @param from value to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromInt(PVData pv, int from);
    /**
     * Convert a PV from a <i>long</i>
     * @param pv a PV
     * @param from value to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromLong(PVData pv, long from);
    /**
     * Convert a PV from a <i>float</i>
     * @param pv a PV
     * @param from value to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromFloat(PVData pv, float from);
    /**
     * Convert a PV from a <i>double</i>
     * @param pv a PV
     * @param from value to put into PV
     * @throws IllegalArgumentException if the Type is not a numeric scalar
     */
    void  fromDouble(PVData pv, double from);
    /**
     * Convert a PV array to a <i>byte</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int toByteArray(PVData pv, int offset, int len, byte[]to, int toOffset);
    /**
     * Convert a PV array to a <i>short</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int toShortArray(PVData pv, int offset, int len, short[]to, int toOffset);
    /**
     * Convert a PV array to an <i>int</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int toIntArray(PVData pv, int offset, int len, int[]to, int toOffset);
    /**
     * Convert a PV array to a <i>long</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int toLongArray(PVData pv, int offset, int len, long[]to, int toOffset);
    /**
     * Convert a PV array to a <i>float</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int toFloatArray(PVData pv, int offset, int len, float[]to, int toOffset);
    /**
     * Convert a PV array to a <i>double</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param to where to put the PV data
     * @param toOffset starting element in the array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int toDoubleArray(PVData pv, int offset, int len, double[]to, int toOffset);
    /**
     * Convert a PV array from a <i>byte</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int fromByteArray(PVData pv, int offset, int len, byte[]from, int fromOffset);
    /**
     * Convert a PV array from a <i>short</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int fromShortArray(PVData pv, int offset, int len, short[]from, int fromOffset);
    /**
     * Convert a PV array from an <i>int</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int fromIntArray(PVData pv, int offset, int len, int[]from, int fromOffset);
    /**
     * Convert a PV array from a <i>long</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int fromLongArray(PVData pv, int offset, int len, long[]from, int fromOffset);
    /**
     * Convert a PV array from a <i>float</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int fromFloatArray(PVData pv, int offset, int len, float[]from, int fromOffset);
    /**
     * Convert a PV array from a <i>double</i> array.
     * @param pv a PV
     * @param offset starting element in a PV
     * param len number of elements to transfer
     * @param from value to put into PV
     * @param fromOffset starting element in the source array
     * @return number of elements converted
     * @throws IllegalArgumentException if the element type is not numeric
     */
    int fromDoubleArray(PVData pv, int offset, int len, double[]from, int fromOffset);
}
