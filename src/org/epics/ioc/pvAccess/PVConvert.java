/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * @author mrk
 *
 */
public interface PVConvert {
	String getString(PVData pv);
	byte toByte(PVData pv);
    short toShort(PVData pv);
    int   toInt(PVData pv);
    long  toLong(PVData pv);
    float toFloat(PVData pv);
    double toDouble(PVData pv);
    void fromByte(PVData pv, byte from);
    void  fromShort(PVData pv, short from);
    void  fromInt(PVData pv, int from);
    void  fromLong(PVData pv, long from);
    void  fromFloat(PVData pv, float from);
    void  fromDouble(PVData pv, double from);
    int toByteArray(PVData pv, int offset, int len, byte[]to, int toOffset);
    int toShortArray(PVData pv, int offset, int len, short[]to, int toOffset);
    int toIntArray(PVData pv, int offset, int len, int[]to, int toOffset);
    int toLongArray(PVData pv, int offset, int len, long[]to, int toOffset);
    int toFloatArray(PVData pv, int offset, int len, float[]to, int toOffset);
    int toDoubleArray(PVData pv, int offset, int len, double[]to, int toOffset);
    int fromByteArray(PVData pv, int offset, int len, byte[]from, int fromOffset);
    int fromShortArray(PVData pv, int offset, int len, short[]from, int fromOffset);
    int fromIntArray(PVData pv, int offset, int len, int[]from, int fromOffset);
    int fromLongArray(PVData pv, int offset, int len, long[]from, int fromOffset);
    int fromFloatArray(PVData pv, int offset, int len, float[]from, int fromOffset);
    int fromDoubleArray(PVData pv, int offset, int len, double[]from, int fromOffset);
}
