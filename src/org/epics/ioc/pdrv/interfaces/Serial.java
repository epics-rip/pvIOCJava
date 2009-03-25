/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 * Interface for serial communication.
 * @author mrk
 *
 */
public interface Serial extends Interface {
    /**
     * Request count reached
     */
    public static final int EOM_CNT = 0x0001;
    /**
     * End of String detected
     */
    public static final int EOM_EOS = 0x0002; 
    /**
     * End indicator detected
     */
    public static final int EOM_END = 0x0004; 
    
    /**
     * Write a serial message.
     * Interpose or driver code may add end of string terminators to the message
     * but the extra characters are not included in the number of bytes written. 
     * @param user The user.
     * @param data The data.
     * @param nbytes The number of bytes to write. 
     * @return The status of the request.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.success or Status.overflow then
     * user.getInt() returns the number of bytes of data that were written.
     */
    Status write(User user,byte[] data,int nbytes);
    /**
     * Read an serial message.
     * If read returns asynSuccess than eomReason
     * (some combination of EOM_CNT, EOM_EOS, and EOM_END)
     * tells why the read completed.
     * Interpose or driver code may strip end of string terminators from the message.
     * If it does the the eos characters will not be included in nbytesTransfered.
     * @param user The user.
     * @param data The data.
     * @param nbytes The number of bytes to read. 
     * @return The status of the request.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.success or Status.overflow then user.getInt returns the number of bytes that were read.
     * If status is Status.overflow than user.getAuxStatus() returns the number
     * of bytes not read.
     * If status is Status.success than user.getAuxStatus() returns
     * the eomMessage.
     */
    Status read(User user,byte[] data,int nbytes);
    /**
     * Flush any input data.
     * @param user The user.
     * @return The status.
     * If status is Status.error than user.getMessage() returns the reason.
     */
    Status flush(User user);
    /**
     * Set termination characters for input.
     * @param user The user.
     * @param eos The termination string.
     * @param eosLen The number of termination bytes.
     * @return The status.
     * If status is Status.error than user.getMessage() returns the reason.
     */
    Status setInputEos(User user,byte[] eos,int eosLen);
    /**
     * Get termination characters for output.
     * @param user The user.
     * @param eos The termination string.
     * @return The status.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.success than user.getAuxStatus() returns the number
     * of termination characters.
     */
    Status getInputEos(User user,byte[] eos);
    /**
     * Set termination characters for output.
     * @param user The user.
     * @param eos The termination string.
     * @param eosLen The number of termination bytes.
     * @return The status.
     * If status is Status.error than user.getMessage() returns the reason.
     */
    Status setOutputEos(User user,byte[] eos,int eosLen);
    /**
     * Get termination characters for input.
     * @param user The user.
     * @param eos The termination string.
     * @return The status.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.success than user.getAuxStatus() returns the number
     * of termination characters.
     */
    Status getOutputEos(User user,byte[] eos);
    /**
     * Register a listener for value changes.
     * @param user The user.
     * @param serialInterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status addInterruptUser(User user,
        SerialInterruptListener serialInterruptListener);
    /**
     * Cancel a listener.
     * @param user The user.
     * @param serialInterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,
        SerialInterruptListener serialInterruptListener);
    /**
     * An interrupt occurred.
     * @param data The byte array.
     * @param nbytes The number of bytes.
     */
    void interruptOccurred(byte[] data,int nbytes);
}
