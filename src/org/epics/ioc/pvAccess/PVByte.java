/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * get/put byte data.
 * @author mrk
 *
 */
public interface PVByte extends PVData{
    /**
     * get the <i>byte</i> value stored in the field.
     * @return byte value of field.
     */
    byte get();
    /**
     * put the <i>byte</i> value into the field.
     * @param value new byte value for field.
     * @throws IllegalStateException if the field is not mutable.
     */
    void put(byte value);
}
