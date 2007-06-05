/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

import org.epics.ioc.pdrv.Interface;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.User;

/**
 * Interface for an octet, i.e. an array of 8 bit bytes.
 * @author mrk
 *
 */
public interface Octet extends Interface {
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
     * Write an octet array and add possible terminators.
     * @param user The user.
     * @param data The data.
     * @param nbytes The number of bytes to write. 
     * @return The status of the request.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.overflow than user.getAuxStatus() returns the number
     * of bytes not written.
     * If status is Status.success than user.getAuxStatus() returns
     * the EOM_XXX and user.getInt() returns the number of bytes of data that were written.
     */
    Status write(User user,byte[] data,int nbytes);
    /**
     * Write an octet array without appending any terminators.
     * @param user The user.
     * @param data The data.
     * @param nbytes The number of bytes to write. 
     * @return The status of the request.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.overflow than user.getAuxStatus() returns the number
     * of bytes not written.
     * If status is Status.success than user.getAuxStatus() returns
     * the EOM_XXX and user.getInt() returns the number of bytes of data that were written.
     */
    Status writeRaw(User user,byte[] data,int nbytes);
    /**
     * Read an octet array and strip possible terminators.
     * @param user The user.
     * @param data The data.
     * @param nbytes The number of bytes to write. 
     * @return The status of the request.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.overflow than user.getAuxStatus() returns the number
     * of bytes not read.
     * If status is Status.success than user.getAuxStatus() returns
     * the EOM_XXX and user.getInt() returns the number of bytes of data that were read.
     */
    Status read(User user,byte[] data,int nbytes);
    /**
     * Read an octet array without stripping any terminators.
     * @param user The user.
     * @param data The data.
     * @param nbytes The number of bytes to write. 
     * @return The status of the request.
     * If status is Status.error than user.getMessage() returns the reason.
     * If status is Status.overflow than user.getAuxStatus() returns the number
     * of bytes not read.
     * If status is Status.success than user.getAuxStatus() returns
     * the EOM_XXX and user.getInt() returns the number of bytes of data that were read.
     */
    Status readRaw(User user,byte[] data,int nbytes);
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
     * @param octetInterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status addInterruptUser(User user,
        OctetInterruptListener octetInterruptListener);
    /**
     * Cancel a listener.
     * @param user The user.
     * @param octetInterruptListener The listener interface.
     * @return The status.
     * If the status is not Status.success
     * than user.message() describes the problem.
     */
    Status removeInterruptUser(User user,
        OctetInterruptListener octetInterruptListener);
}
