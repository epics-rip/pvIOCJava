/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;

import org.epics.ioc.pdrv.User;
import org.epics.ioc.support.alarm.AlarmSupport;

/**
 * PortDriverLink.
 * Implemented by PortDriverLinkFactory.
 * Everything in the structure this supports is controlled by PortDriverLink.
 * @author mrk
 *
 */
public interface PortDriverLink {
    /**
     * Get the User.
     * @return The interface.
     */
    User getUser();
    /**
     * Get the alarmSupport, which must for an alarm field in the structure the portDriverLink supports.
     * @return The interface.
     */
    AlarmSupport getAlarmSupport();
    /**
     * Get a byte buffer.
     * In the byte buffer is null when this is called an 80 element buffer is allocated.
     * @return The buffer.
     */
    byte[] getByteBuffer();
    /**
     * Expand the byte buffer.
     * A new buffer is allocated only if the current buffer is null or smaller than size.
     * If a buffer already exists it is copied into the new buffer.
     * @param size The number of elements required.
     * @return The new buffer.
     */
    byte[] expandByteBuffer(int size);
}
