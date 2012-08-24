/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.vxi11;

/**
 * This is an interface for code that uses VXI11Factory.
 * It allows the VXI11Factory to pass information back to the user.
 * VXI11Factory calls set methods and the user calls get methods.
 * @author mrk
 *
 */
public interface VXI11User {
    /**
     * Request count reached
     */
    public static final int EOM_REQCNT = 0x0001;
    /**
     * End of String detected
     */
    public static final int EOM_CHR = 0x0002; 
    /**
     * End indicator detected
     */
    public static final int EOM_END = 0x0004; 
    /**
     * Clear all values. Called by user.
     */
    void clear();
    /**
     * Called by VXI11Factory to set a boolean value.
     * @param value The value.
     */
    void setBoolean(boolean value);
    /**
     * Called by user to get the value.
     * @return The value.
     */
    boolean getBoolean();
    /**
     * Called by VXI11Factory to set a byte value.
     * @param value The value.
     */
    void setByte(byte value);
    /**
     * Called by user to get the value.
     * @return The value.
     */
    byte getByte();
    /**
     * Called by VXI11Factory to set an integer value.
     * @param value The value.
     */
    void setInt(int value);
    /**
     * Called by user to get the value.
     * @return The value.
     */
    int getInt();
    /**
     * Called by VXI11Factory to set an array of bytes.
     * @param data The data.
     */
    void setByteArray(byte[] data);
    /**
     * Called by user to get the value.
     * @return The data.
     */
    byte[] getByteArray();
    /**
     * Called by VXI11Factory to set a string value.
     * @param value The value.
     */
    void setString(String value);
    /**
     * Called by user to get the value.
     * @return The value.
     */
    String getString();
    /**
     * Called by VXI11Factory to set an error.
     * @param error The error.
     */
    void setError(VXI11ErrorCode error);
    /**
     * Called by user to get the value.
     * @return The error.
     */
    VXI11ErrorCode getError();
    /**
     * Called by VXI11Factory to set a reason.
     * @param reason The reason.
     */
    void setReason(int reason);
    /**
     * Called by user to get the value.
     * @return The reason.
     */
    int getReason();
}
