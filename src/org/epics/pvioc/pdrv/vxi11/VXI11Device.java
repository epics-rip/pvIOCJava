/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pdrv.vxi11;

import org.epics.pvioc.pdrv.Status;

/**
 * Methods for communication with a VXI11 Device.
 * 
 * @author mrk
 *
 */

public interface VXI11Device {
    /**
     * Connect to the device;
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status connect(VXI11User user);
    /**
     * Is the device connected?
     * @return (false,true) if the device (is not,is) connected.
     */
    boolean isConnected();
    /**
     * Disconnect from the device.
     */
    void disconnect();
    /**
     * Trigger the device.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status trigger(VXI11User user);
    /**
     * Clear the device.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status clear(VXI11User user);
    /**
     * Lock the device.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status lock(VXI11User user);
    /**
     * Unlock the device.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status unlock(VXI11User user);
    /**
     * Get current state of the object's EOI flag.
     * @return The current state of the object's EOI flag.
     */
    boolean getEOI();
    /**
     * Set the object's EOI flag.
     * If the object's EOI flag is true subsequent write operations
     * will assert the EOI line when the final byte of data is
     * transmitted to the device.
     * The default state of the EOI flag is true.
     */
    void setEOI(boolean eoi);
    /**
     * Get the object's terminating character.
     * @return The character.
     */
    int getTermChar();
    /**
     * Set object's terminating character.
     * If the <I>termChar</I> argument is less than zero subsequent read
     * operations will terminate only when the requested number of bytes
     * have been received or when the EOI line is asserted.
     * <P>
     * If the <I>termChar</I> argument is greater than or equal to zero and
     * less than or equal to 255 read operations will also terminate when
     * a byte with that value is received.  The terminating character will
     * be included in the data returned by the read operation.
     * <P>
     * If the <I>termChar</I> argument is greater than 255 the return status is error and user.getString() has the reason.
     * <P>
     * The default terminating character value is -1.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status setTermChar(VXI11User user,int termChar);
    /**
     * Write an array of bytes to the IEEE-488 device.
     * If the object's EOI flag is set
     * the final byte of the array will be transmitted with
     * the EOI line asserted.
     * @param user The user.
     * @param data Array of bytes to be written to the device.
     * @param nbytes Number of bytes to write.
     * Large arrays are sent using multiple remote procedure
     * calls to avoid exceeding the maxReceiverSize
     * of the VXI11 controller.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     *
     */
    Status write(VXI11User user,byte[] data,int nbytes);
    /**
     * Read from the VXI11 device until <I>maxCount</I> bytes
     * have been read or until the EOI line is asserted, or until
     * the terminating character, if any, is received.
     * @param user The user.
     * @param data The array of bytes in which the return data is put.
     * @param maxCount The maximum number of characters to be read.  If
     * this value is less than zero bytes will be read until one of the
     * other terminating conditions occurs.
     * @return The status. If success then user.getInt() has the number of bytes returned. 
     * If failure user.getError() and user.getString() report cause of failure.
     */

    Status read(VXI11User user,byte[]data,int maxCount);
    /**
     * Get the value of the device's status byte.
     * @param user The user.
     * @return The status. If success then user.getByte() has the status byte.
     * If failure user.getError() and user.getString() report cause of failure.
     */
    Status getStatusByte(VXI11User user);
    /**
     * Place the device under remote control.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status remote(VXI11User user);
    /**
     * Place the device under local control.
     * @param user The user.
     * @return The status. If failure user.getError() and user.getString() report cause of failure.
     */
    Status local(VXI11User user);
}
