/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public interface PVRecord extends PVStructure {
    /**
     * Get the record instance name.
     * @return The name.
     */
    String getRecordName();
    /**
     * Report a message.
     * The record name will be appended to the message.
     * @param message The message.
     * @param messageType The message type.
     */
    void message(String message, MessageType messageType);
    void addRequestor(Requestor requestor);
    void removeRequestor(Requestor requestor);
}
