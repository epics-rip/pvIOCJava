/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * Base interface for requestors.
 * @author mrk
 *
 */
public interface Requestor {
    /**
     * Get the name of the requestor.
     * @return The name.
     */
    String getRequestorName();
    /**
     * Report a message.
     * @param message The message.
     * @param messageType The message type.
     */
    void message(String message, MessageType messageType);
}
